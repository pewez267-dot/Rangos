package com.gbaminecraft.emulator.debug;

import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Lightweight boot/diagnostics tracer. When enabled, records a rolling window
 * of recent CPU state (PC, key registers, mode, video regs) and the BIOS SWIs
 * the game invoked. If a ROM hangs or crashes, dumping this trace shows exactly
 * where it got stuck — the fastest way to find the next thing to fix.
 *
 * Designed to be near-zero cost when disabled.
 */
public final class BootTracer {

    private boolean enabled = false;
    private final int capacity;
    private final long[] pcRing;
    private final int[]  instrRing;
    private int ringPos = 0;
    private long total = 0;

    // SWI histogram (which BIOS calls the game used, and how many times)
    private final long[] swiCounts = new long[256];
    // Instruction-type and memory-region histograms (for a full picture)
    private final long[] typeCounts   = new long[11];
    private final long[] regionCounts = new long[16];

    // Detect tight infinite loops (same PC seen repeatedly)
    private long lastPc = -1;
    private int  samePcStreak = 0;
    private long longestStreak = 0;
    private long longestStreakPc = 0;
    // Tight-loop (polling) detector
    private long loopWinMin = 0xFFFFFFFFL, loopWinMax = 0;
    private int  loopWinCount = 0;
    private long tightestLoopSpan = 0, tightestLoopBase = 0;
    private final int[] loopRegs = new int[16]; // registers when the poll loop was detected
    private boolean loopThumb = false;
    private boolean frozen = false;   // set when PC derails to invalid memory
    private long derailPc = 0;
    private boolean derailIsSp = false;        // true if frozen due to SP corruption
    private boolean spCorruptionLogged = false;
    private final int[] derailRegs = new int[16]; // register snapshot at the moment of derail
    private int  derailCpsr = 0;
    private long derailFromPc = 0;    // last valid PC before the bad jump
    private int  derailFromInstr = 0; // the instruction that caused the jump

    // Interrupt / frame activity counters (to confirm IRQs and rendering work)
    private long irqCount = 0;     // total IRQs dispatched to the game handler
    private long vblankCount = 0;  // VBlank IRQs specifically
    private long frameCount = 0;   // full frames produced by the PPU
    private long irqHandlerRuns = 0; // times the game's user handler actually executed
    public void onIrq(int ifBits)  { if (enabled) { irqCount++; if ((ifBits & 1) != 0) vblankCount++; } }
    public void onFrame()          { if (enabled) frameCount++; }
    public void onIrqHandlerRun()  { if (enabled) irqHandlerRuns++; }

    // Memory-write watchpoints: track the last writes to key RAM/IO so we can
    // see WHAT the game last did to the address its poll loop is watching.
    private long lastIoWriteAddr = 0, lastIoWriteVal = 0; private long ioWriteCount = 0;
    public void onIoWrite(int addr, int val) {
        if (!enabled) return;
        lastIoWriteAddr = addr & 0xFFFFFFFFL; lastIoWriteVal = val & 0xFFFFFFFFL; ioWriteCount++;
    }

    public BootTracer(int capacity) {
        this.capacity = Math.max(16, capacity);
        this.pcRing   = new long[this.capacity];
        this.instrRing= new int[this.capacity];
    }

    public void setEnabled(boolean v) { enabled = v; }
    public boolean isEnabled()        { return enabled; }

    /** Record one executed instruction. Call from the emulation loop. */
    public void onStep(int pc, int instr) {
        onStep(pc, instr, null);
    }

    /**
     * Record one executed instruction, with an optional CPU for snapshotting
     * registers at the exact moment of a derail.
     */
    public void onStep(int pc, int instr, ARM7TDMI cpu) {
        if (!enabled || frozen) return;

        // Derail detector: a valid GBA PC lives in BIOS(0x00), EWRAM(0x02),
        // IWRAM(0x03) or ROM(0x08-0x0D). Anything else means we jumped to junk.
        // The 0xF000F000 sentinel is the HLE IRQ-return marker (handled by the
        // CPU), so it is NOT a derail — ignore it entirely.
        if ((pc & 0xFFFFFFFFL) == 0xF000F000L) return;
        int region = (pc >>> 24) & 0xFF;
        boolean validRegion = region == 0x00 || region == 0x02 || region == 0x03
                || (region >= 0x08 && region <= 0x0D);
        if (!validRegion) {
            frozen = true;
            derailPc = pc & 0xFFFFFFFFL;
            // The previous recorded entry is the instruction that jumped here.
            int prev = ((ringPos - 1) % capacity + capacity) % capacity;
            derailFromPc    = pcRing[prev];
            derailFromInstr = instrRing[prev];
            if (cpu != null) {
                System.arraycopy(cpu.regs, 0, derailRegs, 0, 16);
                derailCpsr = cpu.cpsr;
            }
            return;
        }

        // SP-corruption tripwire: a healthy SP lives in IWRAM (0x03xxxxxx) or
        // EWRAM (0x02xxxxxx) and is word-aligned. If it ever points elsewhere
        // (e.g. into ROM) or becomes misaligned, freeze NOW so we catch the
        // exact instruction that corrupted it — not a later POP.
        if (cpu != null && !spCorruptionLogged) {
            int sp = cpu.regs[13];
            int spRegion = (sp >>> 24) & 0xFF;
            boolean spOk = (spRegion == 0x02 || spRegion == 0x03) && (sp & 3) == 0;
            // Allow the very first instructions before the game sets up its SP.
            if (!spOk && total > 4) {
                spCorruptionLogged = true;
                frozen = true;
                derailPc = sp & 0xFFFFFFFFL;        // reuse: show the bad SP value
                derailIsSp = true;
                derailFromPc    = pc & 0xFFFFFFFFL; // the instruction running now
                derailFromInstr = instr;
                System.arraycopy(cpu.regs, 0, derailRegs, 0, 16);
                derailCpsr = cpu.cpsr;
                return;
            }
        }

        pcRing[ringPos]    = pc & 0xFFFFFFFFL;
        instrRing[ringPos] = instr;
        ringPos = (ringPos + 1) % capacity;
        total++;

        // Tally region and instruction type for the big-picture report.
        if (region < regionCounts.length) regionCounts[region]++;
        classify(pc, instr, cpu);

        if ((pc & 0xFFFFFFFFL) == lastPc) {
            samePcStreak++;
            if (samePcStreak > longestStreak) {
                longestStreak = samePcStreak;
                longestStreakPc = pc & 0xFFFFFFFFL;
            }
        } else {
            samePcStreak = 0;
            lastPc = pc & 0xFFFFFFFFL;
        }

        // Tight-loop (polling) detector: track the min/max PC over a sliding
        // window. If the PC stays inside a tiny range for a long time, the game
        // is almost certainly spin-waiting on something that never changes.
        long upc = pc & 0xFFFFFFFFL;
        if (upc < loopWinMin) loopWinMin = upc;
        if (upc > loopWinMax) loopWinMax = upc;
        if (++loopWinCount >= 4096) {
            long span = loopWinMax - loopWinMin;
            if (span <= 64 && span > 0) {           // stuck in <=16 instructions
                if (span < tightestLoopSpan || tightestLoopSpan == 0) {
                    tightestLoopSpan = span;
                    tightestLoopBase = loopWinMin;
                    // Snapshot the registers + the instructions of the loop so
                    // the report can decode WHAT memory it polls and the value.
                    if (cpu != null) System.arraycopy(cpu.regs, 0, loopRegs, 0, 16);
                    loopThumb = (cpu != null) && cpu.isThumb();
                }
            }
            loopWinMin = 0xFFFFFFFFL; loopWinMax = 0; loopWinCount = 0;
        }
    }

    /**
     * Decodes the common Thumb load/compare instructions and, for loads,
     * resolves the actual memory address from the captured loop registers and
     * shows the value there. This is what reveals WHY a poll loop never exits.
     */
    private String decodeThumbLoad(int op, int pc, MemoryBus bus) {
        // LDRH Rd,[Rb,#imm5*2]  : 1000 1iii iibb bddd
        if ((op & 0xF800) == 0x8800) {
            int rb = (op >> 3) & 7, rd = op & 7, imm = ((op >> 6) & 0x1F) << 1;
            int addr = loopRegs[rb] + imm;
            return String.format("LDRH r%d,[r%d,#0x%X] -> addr=0x%08X val=0x%04X",
                    rd, rb, imm, addr, bus.read16(addr) & 0xFFFF);
        }
        // LDR Rd,[Rb,#imm5*4]   : 0110 1iii iibb bddd
        if ((op & 0xF800) == 0x6800) {
            int rb = (op >> 3) & 7, rd = op & 7, imm = ((op >> 6) & 0x1F) << 2;
            int addr = loopRegs[rb] + imm;
            return String.format("LDR r%d,[r%d,#0x%X] -> addr=0x%08X val=0x%08X",
                    rd, rb, imm, addr, bus.read32(addr));
        }
        // LDRB Rd,[Rb,#imm5]    : 0111 1iii iibb bddd
        if ((op & 0xF800) == 0x7800) {
            int rb = (op >> 3) & 7, rd = op & 7, imm = (op >> 6) & 0x1F;
            int addr = loopRegs[rb] + imm;
            return String.format("LDRB r%d,[r%d,#0x%X] -> addr=0x%08X val=0x%02X",
                    rd, rb, imm, addr, bus.read8(addr) & 0xFF);
        }
        // CMP Rn,#imm8          : 0010 1nnn iiii iiii
        if ((op & 0xF800) == 0x2800) {
            int rn = (op >> 8) & 7, imm = op & 0xFF;
            return String.format("CMP r%d,#0x%X (r%d=0x%08X)", rn, imm, rn, loopRegs[rn]);
        }
        // Bcc                   : 1101 cccc oooooooo
        if ((op & 0xF000) == 0xD000) {
            int cond = (op >> 8) & 0xF;
            String[] cc = {"EQ","NE","CS","CC","MI","PL","VS","VC","HI","LS","GE","LT","GT","LE","AL","NV"};
            return "B" + cc[cond] + " (salto condicional del bucle)";
        }
        return "";
    }

    /** Bucket the instruction into a coarse type for the histogram. */
    private void classify(int pc, int instr, ARM7TDMI cpu) {
        boolean thumb = cpu != null && cpu.isThumb();
        if (thumb) { typeCounts[9]++; return; }
        if ((instr & 0x0FFFFFF0) == 0x012FFF10)      typeCounts[8]++;  // BX
        else if ((instr & 0x0E000000) == 0x0A000000) typeCounts[3]++;  // B/BL
        else if ((instr & 0x0FC000F0) == 0x00000090) typeCounts[4]++;  // MUL
        else if ((instr & 0x0FBF0FFF) == 0x010F0000) typeCounts[6]++;  // MRS
        else if ((instr & 0x0DB0F000) == 0x0120F000) typeCounts[6]++;  // MSR
        else if ((instr & 0x0E000090) == 0x00000090 && ((instr>>>4)&1)==1) typeCounts[5]++; // halfword
        else if ((instr & 0x0C000000) == 0x04000000) typeCounts[1]++;  // LDR/STR
        else if ((instr & 0x0E000000) == 0x08000000) typeCounts[2]++;  // LDM/STM
        else if ((instr & 0x0F000000) == 0x0F000000) typeCounts[7]++;  // SWI
        else if ((instr & 0x0C000000) == 0x00000000) typeCounts[0]++;  // DataProc
        else                                          typeCounts[10]++; // other
    }

    public void onSwi(int num) {
        if (!enabled) return;
        swiCounts[num & 0xFF]++;
    }

    /** Build a human-readable diagnostic report. */
    public String report(ARM7TDMI cpu, MemoryBus bus) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== Fantastic Boy Advance — Boot Trace ====\n");
        sb.append("Instrucciones ejecutadas: ").append(total).append('\n');
        sb.append(String.format("PC actual: 0x%08X  (Thumb=%b)%n", cpu.getPC(), cpu.isThumb()));
        sb.append("Registros R0-R15:\n");
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("  R%-2d=0x%08X", i, cpu.regs[i]));
            if ((i & 3) == 3) sb.append('\n');
        }
        sb.append(String.format("CPSR=0x%08X%n", cpu.cpsr));

        // ── ROM header (confirma que la ROM se cargó bien) ──────────────
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int c = bus.read8(0x080000A0 + i);
            if (c >= 32 && c < 127) title.append((char) c);
        }
        sb.append(String.format("ROM titulo: '%s'  entrypoint[0x08000000]=0x%08X%n",
                title.toString(), bus.read32(0x08000000)));

        // ── Estado de vídeo / interrupciones ────────────────────────────
        sb.append(String.format("DISPCNT=0x%04X DISPSTAT=0x%04X VCOUNT=%d BG0CNT=0x%04X%n",
                bus.read16(0x04000000), bus.read16(0x04000004), bus.read16(0x04000006),
                bus.read16(0x04000008)));
        sb.append(String.format("IME=%d IE=0x%04X IF=0x%04X  WAITCNT=0x%04X%n",
                bus.read16(0x04000208) & 1, bus.read16(0x04000200), bus.read16(0x04000202),
                bus.read16(0x04000204)));
        sb.append(String.format("User IRQ handler [0x03007FFC]=0x%08X%n", bus.read32(0x03007FFC)));
        sb.append(String.format("IRQs disparadas=%d (VBlank=%d)  Frames renderizados=%d  Handler-juego ejecutado=%d%n",
                irqCount, vblankCount, frameCount, irqHandlerRuns));

        // ── Histograma de tipos de instrucción ejecutados ───────────────
        sb.append("Tipos de instrucción ejecutados:\n");
        String[] typeNames = {"DataProc","LDR/STR","LDM/STM","B/BL","MUL","Halfword",
                              "MSR/MRS","SWI","BX","Thumb","Coproc/otro"};
        for (int i = 0; i < typeNames.length; i++) {
            if (typeCounts[i] > 0) sb.append(String.format("  %-12s %d%n", typeNames[i], typeCounts[i]));
        }

        // ── Resumen de regiones de memoria visitadas ────────────────────
        sb.append("Regiones de memoria ejecutadas (PC):\n");
        String[] regNames = {"BIOS(00)","?","EWRAM(02)","IWRAM(03)","IO(04)","?","?","?",
                             "ROM(08)","ROM(09)","ROM(0A)","ROM(0B)","ROM(0C)","ROM(0D)"};
        for (int i = 0; i < regionCounts.length; i++) {
            if (regionCounts[i] > 0 && i < regNames.length)
                sb.append(String.format("  %-10s %d%n", regNames[i], regionCounts[i]));
        }

        // Hang detector
        if (longestStreak > 1000) {
            sb.append(String.format("POSIBLE BUCLE: PC 0x%08X repetido %d veces%n",
                    longestStreakPc, longestStreak));
        }
        if (tightestLoopSpan > 0) {
            sb.append("\n===== ANÁLISIS DEL BUCLE DE ESPERA (lo más importante) =====\n");
            sb.append(String.format("El PC gira en 0x%08X..0x%08X (%d bytes). %s.%n",
                    tightestLoopBase, tightestLoopBase + tightestLoopSpan, tightestLoopSpan,
                    loopThumb ? "Thumb" : "ARM"));
            sb.append("Registros en el bucle:\n");
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("  R%-2d=0x%08X", i, loopRegs[i]));
                if ((i & 3) == 3) sb.append('\n');
            }
            // Decode each instruction of the loop and, for loads, resolve the
            // actual memory address from the loop registers and show its value.
            sb.append("Desensamblado del bucle + lecturas de memoria:\n");
            int base = (int) tightestLoopBase;
            int end  = (int) (tightestLoopBase + tightestLoopSpan);
            for (int a = base; a <= end + 2; a += (loopThumb ? 2 : 4)) {
                if (loopThumb) {
                    int op = bus.read16(a) & 0xFFFF;
                    sb.append(String.format("  0x%08X: %04X  %s%n", a, op, decodeThumbLoad(op, a, bus)));
                } else {
                    int op = bus.read32(a);
                    sb.append(String.format("  0x%08X: %08X%n", a, op));
                }
            }
            sb.append(String.format("Última escritura a I/O: [0x%08X]=0x%X (total escrituras IO=%d)%n",
                    lastIoWriteAddr, lastIoWriteVal, ioWriteCount));
            sb.append("=============================================================\n\n");
        }
        if (frozen) {
            if (derailIsSp) {
                sb.append(String.format("*** SP CORRUPTO: R13=0x%08X (fuera de RAM o desalineado) ***%n", derailPc));
                sb.append(String.format("Instrucción que lo corrompió: 0x%08X en PC=0x%08X%n", derailFromInstr, derailFromPc));
            } else {
                sb.append(String.format("*** DESCARRILAMIENTO: PC saltó a 0x%08X (memoria inválida) ***%n", derailPc));
                sb.append(String.format("Instrucción culpable: 0x%08X en PC=0x%08X%n", derailFromInstr, derailFromPc));
            }
            sb.append("Registros EN EL MOMENTO del problema:\n");
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("  R%-2d=0x%08X", i, derailRegs[i]));
                if ((i & 3) == 3) sb.append('\n');
            }
            sb.append(String.format("  CPSR=0x%08X%n", derailCpsr));
        }

        // SWI histogram
        sb.append("SWIs usadas por el juego:\n");
        boolean any = false;
        for (int i = 0; i < 256; i++) {
            if (swiCounts[i] > 0) {
                any = true;
                sb.append(String.format("  SWI 0x%02X (%s): %d%n", i, swiName(i), swiCounts[i]));
            }
        }
        if (!any) sb.append("  (ninguna todavía)\n");

        // Recent PC window
        sb.append("Últimas instrucciones (PC : opcode):\n");
        int shown = Math.min(capacity, (int) Math.min(total, capacity));
        for (int k = 0; k < shown; k++) {
            int idx = ((ringPos - shown + k) % capacity + capacity) % capacity;
            sb.append(String.format("  0x%08X : 0x%08X%n", pcRing[idx], instrRing[idx]));
        }
        return sb.toString();
    }

    public void dumpToFile(Path file, ARM7TDMI cpu, MemoryBus bus) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file.toFile()))) {
            w.write(report(cpu, bus));
        } catch (IOException ignored) {}
    }

    private static String swiName(int n) {
        switch (n) {
            case 0x00: return "SoftReset";
            case 0x01: return "RegisterRamReset";
            case 0x02: return "Halt";
            case 0x04: return "IntrWait";
            case 0x05: return "VBlankIntrWait";
            case 0x06: return "Div";
            case 0x08: return "Sqrt";
            case 0x0B: return "CpuSet";
            case 0x0C: return "CpuFastSet";
            case 0x0E: return "BgAffineSet";
            case 0x0F: return "ObjAffineSet";
            case 0x11: return "LZ77Wram";
            case 0x12: return "LZ77Vram";
            case 0x13: return "HuffUnComp";
            case 0x14: return "RLWram";
            case 0x15: return "RLVram";
            default:   return "?";
        }
    }

    public void reset() {
        ringPos = 0; total = 0; lastPc = -1; samePcStreak = 0;
        longestStreak = 0; longestStreakPc = 0;
        frozen = false; derailPc = 0;
        derailIsSp = false; spCorruptionLogged = false;
        derailFromPc = 0; derailFromInstr = 0; derailCpsr = 0;
        loopWinMin = 0xFFFFFFFFL; loopWinMax = 0; loopWinCount = 0;
        tightestLoopSpan = 0; tightestLoopBase = 0; loopThumb = false;
        java.util.Arrays.fill(loopRegs, 0);
        irqCount = 0; vblankCount = 0; frameCount = 0; irqHandlerRuns = 0;
        lastIoWriteAddr = 0; lastIoWriteVal = 0; ioWriteCount = 0;
        java.util.Arrays.fill(derailRegs, 0);
        java.util.Arrays.fill(swiCounts, 0);
        java.util.Arrays.fill(typeCounts, 0);
        java.util.Arrays.fill(regionCounts, 0);
    }
}
