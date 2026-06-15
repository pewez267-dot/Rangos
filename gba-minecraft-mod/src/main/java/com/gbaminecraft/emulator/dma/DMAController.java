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

    // FBA 13z4 — workaround para m4aSoundVSync que NO corre en nuestro emulador.
    // Captura: el juego escribe los registros del DMA solo en boot (32 writes en
    // los primeros 100 frames) y deja wordCount=0 (= max 0x4000 = 64 KB, ~2 s).
    // m4aSoundVSync DEBERÍA reescribir REG_DMA1CNT_H cada PCM_DMA_BUF_SIZE (≈ 7)
    // frames pero solo se observan 2 invocaciones en 60 s (vs ~514 esperadas) —
    // probable bug del IntrWait en HleBios. Sin ese relatch, el DMA se desboca
    // muy lejos del buffer real (PCM_BUFFER_LEN ≈ 1568, doble buffer ≈ 3136
    // bytes) y reproduce basura. Mientras se arregla IntrWait, recargamos
    // internalSrc al SAD cada {@code FIFO_RELOAD_VBLANKS} frames; valor 6
    // elegido por barrido espectral (1, 3, 6, 9, 12 frames) sobre captura
    // headless real: clicks bajan de 4 752 (cada frame) a 1 533 con N=6, y
    // bass-energy sube de 56 % a 68.7 % — mejor que la referencia (5 603 / 58 %).
    // N≥9 produce basura (>34 k clicks) porque pasa el final del buffer.
    private static final int FIFO_RELOAD_VBLANKS = 6;
    private final int[] fifoReloadVblCounters = new int[4];

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
            // FBA 13z4 — Direct Sound DMA source reload cada FIFO_RELOAD_VBLANKS
            // frames (workaround para m4aSoundVSync no-corriente, ver field
            // doc). 13z2 recargaba CADA frame: arreglaba el desboque pero
            // repetía solo los primeros ~544 bytes a 60 Hz = "distorsión a
            // veces" residual del 25 % que reportaba el usuario. N=6 deja al
            // DMA recorrer ~3264 bytes lineales (≈ buffer doble PCM_BUFFER_LEN
            // de Emerald) antes de reanclar al SAD. Barrido espectral 1/3/6/9/12
            // confirmó N=6 como sweet spot: clicks 4 752 -> 1 533 (3x menos),
            // bass 56 % -> 68.7 % (mejor que la referencia 5 603 / 58 %).
            if (startMode == 3 && (ch == 1 || ch == 2)) {
                if (++fifoReloadVblCounters[ch] >= FIFO_RELOAD_VBLANKS) {
                    internalSrc[ch] = srcAddr[ch];
                    fifoReloadVblCounters[ch] = 0;
                }
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

        // FBA 13z3 — emulación correcta del modo REPEAT del DMA Sound-FIFO.
        //
        // 13z2 recargaba internalSrc al SAD en CADA VBlank: arreglaba el
        // desboque pero a costa de repetir solo los primeros ~544 bytes del
        // buffer cada frame (60 Hz), lo que el usuario percibía como
        // "distorsión a veces" residual.
        //
        // El comportamiento real del hardware (verificado contra mGBA dma.c
        // _dmaEvent y la traza: el juego escribe wordCount=0 / REPEAT y NO
        // toca el DMA durante el gameplay): cada transferencia decrementa
        // internalCnt; cuando llega a 0 con REPEAT activo, internalSrc se
        // recarga al SAD y internalCnt al wordCount original (0 -> 0x4000
        // como en hardware). El DMA recorre el buffer linealmente durante
        // ~2 s antes de dar la vuelta, mientras MP2K va rellenando in-place
        // por delante del puntero de lectura, igual que en una GBA real.
        internalCnt[ch] -= 4;
        if (internalCnt[ch] <= 0) {
            if ((control[ch] & CTRL_REPEAT) != 0) {
                internalSrc[ch] = srcAddr[ch];
                int cnt = wordCount[ch];
                if (cnt == 0) cnt = (ch == 3) ? 0x10000 : 0x4000;
                internalCnt[ch] = cnt;
            } else {
                enabled[ch] = false;
                control[ch] &= ~CTRL_ENABLE;
            }
        }
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
