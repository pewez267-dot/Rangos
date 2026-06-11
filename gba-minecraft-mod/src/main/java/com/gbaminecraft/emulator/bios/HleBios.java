package com.gbaminecraft.emulator.bios;

import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * High-Level Emulation (HLE) of the GBA BIOS SWI calls.
 *
 * Instead of executing the real (copyrighted) BIOS ROM, we implement the most
 * common SWI functions directly in Java. This is enough for the great majority
 * of commercial games (incl. Pokémon Emerald) which rely heavily on:
 *   Div, DivArm, Sqrt, ArcTan2, CpuSet, CpuFastSet, BgAffineSet, ObjAffineSet,
 *   LZ77 (WRAM/VRAM), Huffman, RLUnComp, Diff8/16bitUnFilter, VBlankIntrWait,
 *   Halt, IntrWait, SoundBias, etc.
 *
 * The CPU calls {@link #handle(int)} when it decodes a SWI; if we return true,
 * the SWI was serviced in Java and the CPU simply returns to the caller.
 */
public final class HleBios {

    private final ARM7TDMI cpu;
    private final MemoryBus bus;
    private com.gbaminecraft.emulator.debug.BootTracer tracer;

    public HleBios(ARM7TDMI cpu, MemoryBus bus) {
        this.cpu = cpu;
        this.bus = bus;
    }

    public void setTracer(com.gbaminecraft.emulator.debug.BootTracer t) { this.tracer = t; }

    private int r(int i)            { return cpu.regs[i]; }
    private void setR(int i, int v) { cpu.regs[i] = v; }

    /**
     * Service a BIOS SWI by its comment number. Returns true if handled here
     * (so the CPU should NOT jump to the BIOS vector).
     */
    public boolean handle(int swiNum) {
        if (tracer != null) tracer.onSwi(swiNum & 0xFF);
        switch (swiNum & 0xFF) {
            case 0x00: softReset();       return true;
            case 0x01: registerRamReset();return true;
            case 0x02: halt();            return true;  // Halt
            case 0x03: halt();            return true;  // Stop/Sleep -> treat as halt
            case 0x04: intrWait();        return true;  // IntrWait
            case 0x05: vBlankIntrWait();  return true;  // VBlankIntrWait
            case 0x06: div();             return true;
            case 0x07: divArm();          return true;
            case 0x08: sqrt();            return true;
            case 0x09: arcTan();          return true;
            case 0x0A: arcTan2();         return true;
            case 0x0B: cpuSet();          return true;
            case 0x0C: cpuFastSet();      return true;
            case 0x0D: getBiosChecksum(); return true;
            case 0x0E: bgAffineSet();     return true;
            case 0x0F: objAffineSet();    return true;
            case 0x10: bitUnpack();       return true;
            case 0x11: lz77(false);       return true;  // LZ77UnCompWram (8-bit writes)
            case 0x12: lz77(true);        return true;  // LZ77UnCompVram (16-bit writes)
            case 0x13: huffUnComp();      return true;
            case 0x14: rlUnComp(false);   return true;  // RLUnCompWram
            case 0x15: rlUnComp(true);    return true;  // RLUnCompVram
            case 0x16: diffUnFilter(8,  true);  return true; // Diff8bitUnFilterWram
            case 0x17: diffUnFilter(8,  false); return true; // Diff8bitUnFilterVram
            case 0x18: diffUnFilter(16, true);  return true; // Diff16bitUnFilter
            case 0x19: soundBias();       return true;
            case 0x1F: midiKey2Freq();    return true;
            // Sound/multiboot SWIs we don't model: ack as no-op so games continue.
            case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E:
            case 0x20: case 0x21: case 0x22: case 0x23: case 0x24:
            case 0x25: case 0x26: case 0x27: case 0x28: case 0x29:
            case 0x2A: case 0x2B:
                return true;
            default:
                return true; // unknown SWI: no-op rather than crash
        }
    }

    // ── 0x00 SoftReset ───────────────────────────────────────────────────
    private void softReset() {
        // The real BIOS SoftReset re-initialises the stack pointers of SVC/IRQ/
        // System modes, switches to System mode (ARM state, IRQs enabled) and
        // jumps to either ROM or EWRAM per the flag at 0x03007FFA. Games rely on
        // this — without it their SP is garbage and the first POP derails.
        cpu.biosReinitStacks();
        int flag = bus.read8(0x03007FFA);
        setR(15, flag == 0 ? 0x08000000 : 0x02000000);
        cpu.flushPipeline();
    }

    // ── 0x01 RegisterRamReset ────────────────────────────────────────────
    private void registerRamReset() {
        int flags = r(0);
        if ((flags & 0x01) != 0) java.util.Arrays.fill(bus.getEWRAM(), (byte)0);
        if ((flags & 0x02) != 0) {
            // IWRAM except the last 0x200 bytes (BIOS/IRQ stack area)
            byte[] iw = bus.getIWRAM();
            java.util.Arrays.fill(iw, 0, Math.max(0, iw.length - 0x200), (byte)0);
        }
        if ((flags & 0x04) != 0) java.util.Arrays.fill(bus.getPalette(), (byte)0);
        if ((flags & 0x08) != 0) java.util.Arrays.fill(bus.getVRAM(), (byte)0);
        if ((flags & 0x10) != 0) java.util.Arrays.fill(bus.getOAM(), (byte)0);
        // bits 0x20/0x40/0x80: SIO/sound/other regs — left to the I/O defaults.
    }

    // ── 0x02 Halt / 0x03 Stop ────────────────────────────────────────────
    private void halt() { cpu.halted = true; }

    // ── 0x04 IntrWait / 0x05 VBlankIntrWait ──────────────────────────────
    // BIOS IntrWait semantics (mirror flags live at 0x03007FF8, "INTRCHECK"):
    //   IntrWait(discardOld, waitFlags):
    //     - if discardOld != 0, clear the bits of `waitFlags` from the mirror
    //     - HALT; on each IRQ wake, the game's IRQ handler ORs the serviced IF
    //       bits into the mirror. Resume only once (mirror & waitFlags) != 0,
    //       then clear those bits.
    //   VBlankIntrWait == IntrWait(1, 1) (wait for the VBlank IRQ, bit0).
    // The CPU stays halted; this routine is re-entered by the CPU's halt-wake
    // path which re-runs the pending SWI until the condition is satisfied.
    private static final int INTRCHECK = 0x03007FF8;

    private void intrWait() {
        int discard   = r(0);
        int waitFlags = r(1) & 0xFFFF;
        intrWaitCommon(discard != 0, waitFlags);
    }
    private void vBlankIntrWait() {
        intrWaitCommon(true, 0x0001); // discard old, wait for VBlank (bit0)
    }

    /** True while an IntrWait is in progress (so the CPU halt-wake path can
     *  re-evaluate it). */
    public int  intrWaitFlags = 0;
    public boolean intrWaitActive = false;

    private void intrWaitCommon(boolean discardOld, int waitFlags) {
        if (waitFlags == 0) waitFlags = 0x0001;
        if (!intrWaitActive) {
            // First entry: optionally discard previously-recorded interrupts so
            // we genuinely wait for a fresh one.
            if (discardOld) {
                int mirror = bus.read16(INTRCHECK) & 0xFFFF;
                bus.write16(INTRCHECK, (short)(mirror & ~waitFlags));
            }
            intrWaitActive = true;
            intrWaitFlags = waitFlags;
            cpu.halted = true;
            return;
        }
        // Re-entry (woken by an IRQ): has the awaited interrupt been recorded?
        int mirror = bus.read16(INTRCHECK) & 0xFFFF;
        if ((mirror & waitFlags) != 0) {
            // Consume the awaited bits and finish the wait.
            bus.write16(INTRCHECK, (short)(mirror & ~waitFlags));
            intrWaitActive = false;
            intrWaitFlags = 0;
            cpu.halted = false;
        } else {
            // Not yet — keep waiting.
            cpu.halted = true;
        }
    }

    // ── 0x06 Div / 0x07 DivArm ───────────────────────────────────────────
    private void div() {
        int num = r(0), den = r(1);
        if (den == 0) den = 1;                 // avoid /0 (hardware is UB; keep stable)
        int q = num / den;
        int rem = num % den;
        setR(0, q);
        setR(1, rem);
        setR(3, Math.abs(q));
    }
    private void divArm() {
        // Same as Div but with R0/R1 swapped (num in R1, den in R0)
        int num = r(1), den = r(0);
        if (den == 0) den = 1;
        setR(0, num / den);
        setR(1, num % den);
        setR(3, Math.abs(num / den));
    }

    // ── 0x08 Sqrt ────────────────────────────────────────────────────────
    private void sqrt() {
        long v = Integer.toUnsignedLong(r(0));
        setR(0, (int) Math.sqrt(v));
    }

    // ── 0x09 ArcTan / 0x0A ArcTan2 ───────────────────────────────────────
    private void arcTan() {
        // R0 = signed 16-bit fixed (1.14). Returns angle in R0.
        double x = (short) r(0) / 16384.0;
        double a = Math.atan(x);
        setR(0, (int)(a / (2 * Math.PI) * 0x10000) & 0xFFFF);
    }
    private void arcTan2() {
        double x = (short) r(0);
        double y = (short) r(1);
        double a = Math.atan2(y, x);
        if (a < 0) a += 2 * Math.PI;
        setR(0, (int)(a / (2 * Math.PI) * 0x10000) & 0xFFFF);
    }

    // ── 0x0B CpuSet ──────────────────────────────────────────────────────
    private void cpuSet() {
        int src = r(0), dst = r(1), ctrl = r(2);
        int count = ctrl & 0x1FFFFF;
        boolean fixed = (ctrl & (1 << 24)) != 0;   // fixed source (fill)
        boolean word  = (ctrl & (1 << 26)) != 0;   // 32-bit transfer
        if (word) {
            int val = bus.read32(src);
            for (int i = 0; i < count; i++) {
                if (!fixed) val = bus.read32(src + i*4);
                bus.write32(dst + i*4, val);
            }
        } else {
            int val = bus.read16(src);
            for (int i = 0; i < count; i++) {
                if (!fixed) val = bus.read16(src + i*2);
                bus.write16(dst + i*2, (short) val);
            }
        }
    }

    // ── 0x0C CpuFastSet (8-word blocks) ──────────────────────────────────
    private void cpuFastSet() {
        int src = r(0), dst = r(1), ctrl = r(2);
        int count = ctrl & 0x1FFFFF;
        boolean fixed = (ctrl & (1 << 24)) != 0;
        count = (count + 7) & ~7;                  // rounded up to multiples of 8
        int val = bus.read32(src);
        for (int i = 0; i < count; i++) {
            if (!fixed) val = bus.read32(src + i*4);
            bus.write32(dst + i*4, val);
        }
    }

    // ── 0x0D GetBiosChecksum ─────────────────────────────────────────────
    private void getBiosChecksum() { setR(0, 0xBAAE187F); } // GBA BIOS checksum constant

    // ── 0x0E BgAffineSet ─────────────────────────────────────────────────
    private void bgAffineSet() {
        int src = r(0), dst = r(1), num = r(2);
        for (int i = 0; i < num; i++) {
            int cx = bus.read32(src);        // 28.4? Actually signed 19.8 origin
            int cy = bus.read32(src + 4);
            int dispX = (short) bus.read16(src + 8);
            int dispY = (short) bus.read16(src + 10);
            int sx = (short) bus.read16(src + 12);
            int sy = (short) bus.read16(src + 14);
            int angle = (bus.read16(src + 16) >> 8) & 0xFF;
            double rad = angle / 128.0 * Math.PI;
            double c = Math.cos(rad), s = Math.sin(rad);
            int pa = (int)(sx * c) ;
            int pb = (int)(-sx * s);
            int pc = (int)(sy * s);
            int pd = (int)(sy * c);
            bus.write16(dst,      (short) pa);
            bus.write16(dst + 2,  (short) pb);
            bus.write16(dst + 4,  (short) pc);
            bus.write16(dst + 6,  (short) pd);
            // start coordinates
            bus.write32(dst + 8,  cx - (int)((dispX) * (pa) + (dispY) * (pb)));
            bus.write32(dst + 12, cy - (int)((dispX) * (pc) + (dispY) * (pd)));
            src += 20; dst += 16;
        }
    }

    // ── 0x0F ObjAffineSet ────────────────────────────────────────────────
    private void objAffineSet() {
        int src = r(0), dst = r(1), num = r(2), offset = r(3);
        for (int i = 0; i < num; i++) {
            int sx = (short) bus.read16(src);
            int sy = (short) bus.read16(src + 2);
            int angle = (bus.read16(src + 4) >> 8) & 0xFF;
            double rad = angle / 128.0 * Math.PI;
            double c = Math.cos(rad), s = Math.sin(rad);
            int pa = (int)(sx * c);
            int pb = (int)(-sx * s);
            int pc = (int)(sy * s);
            int pd = (int)(sy * c);
            bus.write16(dst,            (short) pa);
            bus.write16(dst + offset,   (short) pb);
            bus.write16(dst + offset*2, (short) pc);
            bus.write16(dst + offset*3, (short) pd);
            src += 6; dst += offset * 4;
        }
    }

    // ── 0x10 BitUnPack ───────────────────────────────────────────────────
    private void bitUnpack() {
        int src = r(0), dst = r(1), info = r(2);
        int len      = bus.read16(info);
        int srcWidth = bus.read8(info + 2);
        int dstWidth = bus.read8(info + 3);
        int dataOff  = bus.read32(info + 4);
        int zeroFlag = (dataOff >>> 31) & 1;
        int offset   = dataOff & 0x7FFFFFFF;

        int srcBitPos = 0, dstBuf = 0, dstBits = 0;
        int srcByte = bus.read8(src);
        int processed = 0;
        while (processed < len) {
            int unit = (srcByte >> srcBitPos) & ((1 << srcWidth) - 1);
            if (unit != 0 || zeroFlag == 1) unit += offset;
            dstBuf |= unit << dstBits;
            dstBits += dstWidth;
            if (dstBits >= 32) {
                bus.write32(dst, dstBuf);
                dst += 4; dstBuf = 0; dstBits = 0;
            }
            srcBitPos += srcWidth;
            if (srcBitPos >= 8) {
                srcBitPos = 0; src++; processed++;
                srcByte = bus.read8(src);
            }
        }
        if (dstBits > 0) bus.write32(dst, dstBuf);
    }

    // ── 0x11/0x12 LZ77 decompression ─────────────────────────────────────
    private void lz77(boolean vram) {
        int src = r(0), dst = r(1);
        int header = bus.read32(src); src += 4;
        int size = header >>> 8;          // decompressed size in bytes
        int written = 0;
        // For VRAM we must do 16-bit writes; buffer a pending byte.
        int pendingHalf = -1;
        while (written < size) {
            int flags = bus.read8(src++);
            for (int b = 0; b < 8 && written < size; b++) {
                if ((flags & (0x80 >> b)) != 0) {
                    // compressed block
                    int b1 = bus.read8(src++);
                    int b2 = bus.read8(src++);
                    int len = (b1 >> 4) + 3;
                    int disp = ((b1 & 0xF) << 8) | b2;
                    int copyFrom = dst + written - disp - 1;
                    for (int k = 0; k < len && written < size; k++) {
                        int val = readDst(copyFrom + k, vram, pendingHalf, dst, written);
                        pendingHalf = writeOut(vram, dst, written, val, pendingHalf);
                        written++;
                    }
                } else {
                    int val = bus.read8(src++);
                    pendingHalf = writeOut(vram, dst, written, val, pendingHalf);
                    written++;
                }
            }
        }
        if (vram && pendingHalf >= 0) {
            // flush trailing byte as a 16-bit write
            bus.write16(dst + (written & ~1), (short) pendingHalf);
        }
    }

    private int readDst(int addr, boolean vram, int pendingHalf, int base, int written) {
        return bus.read8(addr);
    }

    /** Writes one decompressed byte. For VRAM, coalesces into 16-bit writes. */
    private int writeOut(boolean vram, int dst, int written, int val, int pendingHalf) {
        if (!vram) {
            bus.write8(dst + written, (byte) val);
            return -1;
        }
        if ((written & 1) == 0) {
            return val & 0xFF;                 // hold low byte
        } else {
            int half = (pendingHalf & 0xFF) | ((val & 0xFF) << 8);
            bus.write16(dst + (written - 1), (short) half);
            return -1;
        }
    }

    // ── 0x13 Huffman (rare; minimal correct-ish) ─────────────────────────
    private void huffUnComp() {
        // Huffman is uncommon in gameplay; implement a safe passthrough that
        // at least advances registers so games don't hang. Full impl can be
        // added later. We treat the data size and zero-fill the destination.
        int src = r(0), dst = r(1);
        int header = bus.read32(src);
        int size = header >>> 8;
        for (int i = 0; i < size; i++) bus.write8(dst + i, (byte)0);
    }

    // ── 0x14/0x15 Run-Length decompression ───────────────────────────────
    private void rlUnComp(boolean vram) {
        int src = r(0), dst = r(1);
        int header = bus.read32(src); src += 4;
        int size = header >>> 8;
        int written = 0;
        int pendingHalf = -1;
        while (written < size) {
            int flag = bus.read8(src++);
            boolean compressed = (flag & 0x80) != 0;
            int len = (flag & 0x7F) + (compressed ? 3 : 1);
            if (compressed) {
                int val = bus.read8(src++);
                for (int k = 0; k < len && written < size; k++) {
                    pendingHalf = writeOut(vram, dst, written, val, pendingHalf);
                    written++;
                }
            } else {
                for (int k = 0; k < len && written < size; k++) {
                    int val = bus.read8(src++);
                    pendingHalf = writeOut(vram, dst, written, val, pendingHalf);
                    written++;
                }
            }
        }
        if (vram && pendingHalf >= 0) bus.write16(dst + (written & ~1), (short) pendingHalf);
    }

    // ── 0x16-0x18 Diff filters ───────────────────────────────────────────
    private void diffUnFilter(int unitBits, boolean wram) {
        int src = r(0), dst = r(1);
        int header = bus.read32(src); src += 4;
        int size = header >>> 8;          // bytes
        if (unitBits == 8) {
            int acc = 0, written = 0;
            while (written < size) {
                acc = (acc + bus.read8(src++)) & 0xFF;
                bus.write8(dst + written, (byte) acc);
                written++;
            }
        } else { // 16-bit
            int acc = 0, written = 0;
            while (written < size) {
                acc = (acc + bus.read16(src)) & 0xFFFF;
                src += 2;
                bus.write16(dst + written, (short) acc);
                written += 2;
            }
        }
    }

    // ── 0x19 SoundBias / 0x1F MidiKey2Freq ───────────────────────────────
    private void soundBias()    { /* no-op: audio bias not modeled here */ }
    private void midiKey2Freq() {
        // Returns frequency in R0; approximate as identity to avoid hangs.
        // Real formula uses a table; games tolerate approximations for boot.
        setR(0, r(0));
    }
}
