package com.gbaminecraft.emulator.dma;

import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.apu.APU;

/**
 * GBA DMA Controller — 4 DMA channels (DMA0-DMA3).
 * Handles memory transfers, FIFO audio replenishment, and VBlank/HBlank triggers.
 */
public class DMAController {

    private static final int DMA_BASE = 0x0B0;

    // Per-channel state
    private final int[]     srcAddr  = new int[4];
    private final int[]     dstAddr  = new int[4];
    private final int[]     wordCount= new int[4];
    private final int[]     control  = new int[4];
    private final int[]     internalSrc = new int[4];
    private final int[]     internalDst = new int[4];
    private final int[]     internalCnt = new int[4];
    private final boolean[] enabled  = new boolean[4];
    private final boolean[] pending  = new boolean[4];

    // Control register bits
    private static final int CTRL_DST_INCMODE = 0x0060;  // bits 5-6
    private static final int CTRL_SRC_INCMODE = 0x0180;  // bits 7-8
    private static final int CTRL_REPEAT      = 1 << 9;
    private static final int CTRL_32BIT       = 1 << 10;
    private static final int CTRL_GAMEPAK_DRQ = 1 << 11;
    private static final int CTRL_START_TIMING= 0x3000;  // bits 12-13
    private static final int CTRL_IRQ         = 1 << 14;
    private static final int CTRL_ENABLE      = 1 << 15;

    private static final int[] IRQ_BITS = {1 << 8, 1 << 9, 1 << 10, 1 << 11};

    private MemoryBus bus;
    private APU apu;

    public DMAController(MemoryBus bus) {
        this.bus = bus;
    }

    public void connectAPU(APU apu) {
        this.apu = apu;
    }

    // ── Immediate trigger ─────────────────────────────────────────────────
    public void checkImmediate() {
        for (int ch = 0; ch < 4; ch++) {
            if (!enabled[ch]) continue;
            int startMode = (control[ch] & CTRL_START_TIMING) >>> 12;
            if (startMode == 0) { // Immediate
                execute(ch);
            }
        }
    }

    public void onVBlank() {
        for (int ch = 0; ch < 4; ch++) {
            if (!enabled[ch]) continue;
            int startMode = (control[ch] & CTRL_START_TIMING) >>> 12;
            if (startMode == 1) execute(ch); // VBlank
            // FBA 13z2 — Direct Sound DMA source reload (emula m4aSoundVSync).
            //
            // ROOT CAUSE del audio "horrible": en modo Sound-FIFO (startMode 3,
            // repeat), el juego (MP2K) configura el DMA UNA sola vez apuntando a
            // un buffer de PCM en IWRAM (p.ej. 0x030066D0) que RELLENA IN-PLACE
            // cada frame. En hardware real, la rutina m4aSoundVSync — llamada al
            // inicio de CADA VBlank — reinicia el DMA de sonido (lo desactiva y
            // reactiva), lo que RECARGA el source address al inicio del buffer.
            //
            // Nuestro executeFIFO solo hacía internalSrc += 16 indefinidamente y
            // jamás recargaba: tras el primer frame leía PASADO el buffer (tras
            // 30 s, ~851 KB más allá), entregando memoria arbitraria al FIFO =
            // PCM corrupto = el "pitido/distorsión/borroso". El bug quedaba
            // OCULTO mientras el juego corría a 1/4 de velocidad (hack *4): el
            // FIFO se quedaba corto y repetía la última muestra (suave). Al
            // arreglar la velocidad real (13s) el FIFO se ejercita de verdad y
            // el defecto se hizo audible. Diagnóstico confirmado por captura:
            // reLatchDMA=544 con *4 (el juego reiniciaba) vs 0 sin *4.
            //
            // Reproducimos el efecto de m4aSoundVSync recargando el source al
            // inicio del buffer en cada VBlank para los canales FIFO activos.
            if (startMode == 3 && (ch == 1 || ch == 2)) {
                // FBA 13z7 — reload del source + offset medio buffer.
                // El juego rellena el buffer desde el inicio cada frame (m4aSoundMain).
                // Si el DMA también lee desde el inicio, hay una race condition:
                // lee datos a medio-escribir. En hardware real, m4aSoundVSync swapea
                // half-buffers (el DMA lee el half ya-terminado, el mixer escribe el
                // otro). Emulamos eso apuntando el DMA a la SEGUNDA mitad del buffer:
                // esa mitad fue escrita en el frame anterior y está completa.
                int bufLen = srcAddr[2] - srcAddr[1];
                if (bufLen < 256 || bufLen > 8192) bufLen = 1584;
                internalSrc[ch] = srcAddr[ch] + (bufLen / 2);
            }
        }
    }

    public void onHBlank() {
        for (int ch = 0; ch < 4; ch++) {
            if (!enabled[ch]) continue;
            int startMode = (control[ch] & CTRL_START_TIMING) >>> 12;
            if (startMode == 2) execute(ch); // HBlank
        }
    }

    // Sound FIFO mode: timer overflow triggers DMA1 or DMA2.
    // The DMA only tops the FIFO up when it has drained to (or below) half —
    // exactly like the hardware, which kicks the DMA when the 8-word FIFO drops
    // to 4 words. The old code refilled 16 bytes on EVERY timer overflow while
    // only one sample is consumed per overflow, so the FIFO stayed permanently
    // full of stale data and fresh samples were dropped — i.e. the music never
    // really played. Gating the refill on the FIFO level fixes Direct Sound.
    private final byte[] fifoXfer = new byte[16];
    public void onTimerOverflow(int timerIdx) {
        for (int ch = 1; ch <= 2; ch++) {
            if (!enabled[ch]) continue;
            int startMode = (control[ch] & CTRL_START_TIMING) >>> 12;
            if (startMode != 3) continue; // not Sound FIFO mode
            int dst = dstAddr[ch];
            int sz;
            if (apu == null) sz = 0;
            else if (dst == 0x040000A0) sz = apu.fifoASize();
            else if (dst == 0x040000A4) sz = apu.fifoBSize();
            else sz = 99; // unknown dest: don't feed
            if (sz <= 16) executeFIFO(ch);
        }
    }

    private void executeFIFO(int ch) {
        // Transfer 4 words (16 bytes) from memory into the FIFO. The destination
        // is the fixed FIFO register in dstAddr[ch] (DMA1/2 DAD), NOT srcAddr.
        int src = internalSrc[ch];
        int dst = dstAddr[ch];

        // FBA 13z7 — clamp: si ya leímos más allá del buffer del juego, NO
        // avanzamos (push silencio). El VBlank reload reseteará al inicio.
        // Sin esto, cada frame lee ~3776 bytes pero el buffer solo tiene ~1584,
        // y el 40% restante es basura (= la "distorsión a veces" del 25%).
        int bufLen = soundFifoBufferLen(ch);
        if ((src - srcAddr[ch]) >= bufLen) {
            // ya pasamos el final: push silencio para no starvar el FIFO
            java.util.Arrays.fill(fifoXfer, (byte) 0);
            if (apu != null) {
                if (dst == 0x040000A0) apu.pushFifoA(fifoXfer);
                else if (dst == 0x040000A4) apu.pushFifoB(fifoXfer);
            }
            return;
        }

        for (int i = 0; i < 4; i++) {
            int val = bus.read32(src + i * 4);
            fifoXfer[i*4]   = (byte)(val);
            fifoXfer[i*4+1] = (byte)(val >>> 8);
            fifoXfer[i*4+2] = (byte)(val >>> 16);
            fifoXfer[i*4+3] = (byte)(val >>> 24);
        }
        internalSrc[ch] = src + 16;
        if (apu != null) {
            if (dst == 0x040000A0) apu.pushFifoA(fifoXfer);
            else if (dst == 0x040000A4) apu.pushFifoB(fifoXfer);
        }
    }

    /**
     * Buffer length (bytes) para el Direct Sound del canal ch. Derivado de la
     * separación entre los SAD de los dos canales FIFO (que MP2K coloca
     * contiguos). Para Pokémon Emerald: 0x03006D00 - 0x030066D0 = 0x630 = 1584.
     * Fallback conservador si el layout no encaja.
     */
    private int soundFifoBufferLen(int ch) {
        int diff = srcAddr[2] - srcAddr[1];
        if (diff < 0) diff = -diff;
        if (diff >= 256 && diff <= 8192) return diff;
        return 1584; // PCM_DMA_BUF de MP2K por defecto
    }

    private void execute(int ch) {
        boolean is32    = (control[ch] & CTRL_32BIT) != 0;
        int dstMode = (control[ch] & CTRL_DST_INCMODE) >>> 5;
        int srcMode = (control[ch] & CTRL_SRC_INCMODE) >>> 7;
        int unitSize = is32 ? 4 : 2;

        int src = internalSrc[ch];
        int dst = internalDst[ch];
        int cnt = internalCnt[ch];

        for (int i = 0; i < cnt; i++) {
            if (is32) {
                bus.write32(dst, bus.read32(src));
            } else {
                bus.write16(dst, (short) bus.read16(src));
            }
            src = applyIncrement(src, srcMode, unitSize);
            dst = applyIncrement(dst, dstMode, unitSize);
        }

        if ((control[ch] & CTRL_IRQ) != 0) {
            bus.requestInterrupt(IRQ_BITS[ch]);
        }

        boolean repeat = (control[ch] & CTRL_REPEAT) != 0;
        if (repeat) {
            internalCnt[ch] = wordCount[ch] == 0 ? (ch == 3 ? 0x10000 : 0x4000) : wordCount[ch];
            // Reload dst if increment mode is 3 (reload)
            if (dstMode == 3) internalDst[ch] = dstAddr[ch];
        } else {
            enabled[ch] = false;
            control[ch] &= ~CTRL_ENABLE;
        }
    }

    private int applyIncrement(int addr, int mode, int unit) {
        switch (mode) {
            case 0: return addr + unit; // Increment
            case 1: return addr - unit; // Decrement
            case 2: return addr;        // Fixed
            case 3: return addr + unit; // Increment/Reload
            default: return addr + unit;
        }
    }

    // ── Register access ────────────────────────────────────────────────────
    public int readRegister(int offset) {
        int ch  = (offset - DMA_BASE) / 12;
        int reg = (offset - DMA_BASE) % 12;
        if (ch < 0 || ch > 3) return 0;
        switch (reg) {
            case 8:  return control[ch] & 0xFF;
            case 9:  return (control[ch] >>> 8) & 0xFF;
            default: return 0;
        }
    }

    public void writeRegister(int offset, int val) {
        int ch  = (offset - DMA_BASE) / 12;
        int reg = (offset - DMA_BASE) % 12;
        if (ch < 0 || ch > 3) return;
        switch (reg) {
            case 0:  srcAddr[ch]   = (srcAddr[ch]  & 0xFFFFFF00) | (val & 0xFF); break;
            case 1:  srcAddr[ch]   = (srcAddr[ch]  & 0xFFFF00FF) | ((val & 0xFF) << 8); break;
            case 2:  srcAddr[ch]   = (srcAddr[ch]  & 0xFF00FFFF) | ((val & 0xFF) << 16); break;
            case 3:  srcAddr[ch]   = (srcAddr[ch]  & 0x00FFFFFF) | ((val & 0xFF) << 24); break;
            case 4:  dstAddr[ch]   = (dstAddr[ch]  & 0xFFFFFF00) | (val & 0xFF); break;
            case 5:  dstAddr[ch]   = (dstAddr[ch]  & 0xFFFF00FF) | ((val & 0xFF) << 8); break;
            case 6:  dstAddr[ch]   = (dstAddr[ch]  & 0xFF00FFFF) | ((val & 0xFF) << 16); break;
            case 7:  dstAddr[ch]   = (dstAddr[ch]  & 0x00FFFFFF) | ((val & 0xFF) << 24); break;
            case 8:  wordCount[ch] = (wordCount[ch] & 0xFF00) | (val & 0xFF); break;
            case 9:  wordCount[ch] = (wordCount[ch] & 0x00FF) | ((val & 0xFF) << 8); break;
            case 10: { // CNT_L
                control[ch] = (control[ch] & 0xFF00) | (val & 0xFF);
                break;
            }
            case 11: { // CNT_H — writing high byte can trigger DMA
                boolean wasEnabled = enabled[ch];
                control[ch] = (control[ch] & 0x00FF) | ((val & 0xFF) << 8);
                enabled[ch] = (control[ch] & CTRL_ENABLE) != 0;
                if (!wasEnabled && enabled[ch]) {
                    // Latch internal registers
                    internalSrc[ch] = srcAddr[ch];
                    internalDst[ch] = dstAddr[ch];
                    int cnt = wordCount[ch];
                    if (cnt == 0) cnt = (ch == 3) ? 0x10000 : 0x4000;
                    internalCnt[ch] = cnt;
                    // Check for immediate start
                    int startMode = (control[ch] & CTRL_START_TIMING) >>> 12;
                    if (startMode == 0) execute(ch);
                }
                break;
            }
        }
    }

    public void reset() {
        java.util.Arrays.fill(srcAddr, 0);
        java.util.Arrays.fill(dstAddr, 0);
        java.util.Arrays.fill(wordCount, 0);
        java.util.Arrays.fill(control, 0);
        java.util.Arrays.fill(enabled, false);
        java.util.Arrays.fill(pending, false);
    }
}
