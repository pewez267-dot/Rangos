package com.gbaminecraft.emulator.cpu;

import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * ARM7TDMI CPU Emulator — full ARM + Thumb mode implementation.
 * Emulates the GBA's 16.78 MHz ARM7TDMI processor.
 */
public class ARM7TDMI {

    // ── Registers ──────────────────────────────────────────────────────────
    public final int[] regs = new int[16];   // R0-R15 (current-mode view)
    public int cpsr = 0x1F;                  // Current Program Status Register (System mode at start)
    public int spsr = 0;                     // Saved PSR

    // Banked registers for each mode
    private final int[] bankedR8Fiq  = new int[2];
    private final int[] bankedR9Fiq  = new int[2];
    private final int[] bankedR10Fiq = new int[2];
    private final int[] bankedR11Fiq = new int[2];
    private final int[] bankedR12Fiq = new int[2];
    private final int[] bankedR13    = new int[6]; // User/FIQ/IRQ/SVC/ABT/UND
    private final int[] bankedR14    = new int[6];
    private final int[] bankedSPSR   = new int[5]; // FIQ/IRQ/SVC/ABT/UND

    // CPSR flags
    private static final int FLAG_N = 1 << 31;
    private static final int FLAG_Z = 1 << 30;
    private static final int FLAG_C = 1 << 29;
    private static final int FLAG_V = 1 << 28;
    private static final int FLAG_T = 1 << 5;    // Thumb mode
    private static final int FLAG_F = 1 << 6;    // FIQ disable
    private static final int FLAG_I = 1 << 7;    // IRQ disable

    // CPU modes (bits 4:0 of CPSR)
    private static final int MODE_USER = 0x10;
    private static final int MODE_FIQ  = 0x11;
    private static final int MODE_IRQ  = 0x12;
    private static final int MODE_SVC  = 0x13;
    private static final int MODE_ABT  = 0x17;
    private static final int MODE_UND  = 0x1B;
    private static final int MODE_SYS  = 0x1F;

    // Cycle count
    public long cycles = 0;

    private final MemoryBus bus;
    public boolean halted = false;
    public boolean stopped = false;

    // Cached references to the memory arrays the CPU fetches from most often.
    // Used by a small fast path in step() to skip the full bus.read16/read32
    // page-switch when PC is in IWRAM/EWRAM/ROM (the overwhelmingly common case
    // in Pokémon, where most code runs from IWRAM and ROM). Resolved lazily on
    // the first step so the bus has finished loading.
    private byte[] iwramArr, ewramArr, romArr;
    private int    romLen;

    private void cacheBusArrays() {
        if (iwramArr == null) iwramArr = bus.getIWRAM();
        if (ewramArr == null) ewramArr = bus.getEWRAM();
        if (romArr == null || romArr.length != romLen) { romArr = bus.getROM(); romLen = romArr == null ? 0 : romArr.length; }
    }

    // Pipeline / execution state
    private boolean branchTaken = false;  // set true when an instruction writes PC (branch)
    private int curInstrAddr = 0;          // address of the instruction currently executing

    public ARM7TDMI(MemoryBus bus) {
        this.bus = bus;
        reset();
    }

    public void reset() {
        for (int i = 0; i < 16; i++) regs[i] = 0;
        cpsr  = MODE_SYS;
        spsr  = 0;
        halted = false;
        stopped = false;
        branchTaken = false;
        // SP initial values
        bankedR13[modeIndex(MODE_SVC)] = 0x03007FE0;
        bankedR13[modeIndex(MODE_IRQ)] = 0x03007FA0;
        bankedR13[modeIndex(MODE_SYS)] = 0x03007F00;
        regs[13] = 0x03007F00;
        regs[15] = 0x08000000; // ROM start
        cycles = 0;
    }

    /**
     * Emulates the stack-pointer setup the real BIOS performs on (Soft)Reset:
     * SP_svc=0x03007FE0, SP_irq=0x03007FA0, SP_sys/usr=0x03007F00, and leaves
     * the CPU in System mode (ARM state, IRQs enabled). Games depend on this.
     */
    public void biosReinitStacks() {
        // Persist whatever the current mode holds, then set canonical values.
        bankedR13[modeIndex(MODE_SVC)] = 0x03007FE0;
        bankedR13[modeIndex(MODE_IRQ)] = 0x03007FA0;
        bankedR13[modeIndex(MODE_SYS)] = 0x03007F00;
        bankedR14[modeIndex(MODE_SVC)] = 0;
        bankedR14[modeIndex(MODE_IRQ)] = 0;
        // Switch to System mode with a fresh SP.
        int oldMode = cpsr & 0x1F;
        if (oldMode != MODE_SYS) switchMode(oldMode, MODE_SYS);
        cpsr = (cpsr & ~0x1F) | MODE_SYS;  // System mode
        cpsr &= ~FLAG_T;                   // ARM state
        cpsr &= ~(FLAG_I | FLAG_F);        // IRQs enabled
        regs[13] = 0x03007F00;
    }

    // ── Cycle step ─────────────────────────────────────────────────────────
    /** Execute one instruction. Returns cycles consumed. */
    public int step() {
        if (halted || stopped) return 1;

        // HLE BIOS IRQ unwind: the game's handler returned to our sentinel.
        if (inHleIrq && (regs[15] & ~1) == (IRQ_RETURN_SENTINEL & ~1)) {
            hleIrqReturn();
            return 1;
        }

        branchTaken = false;
        curInstrAddr = regs[15];
        int cyc;

        // Resolve cached array refs on first use (after ROM has been loaded).
        if (romArr == null || iwramArr == null) cacheBusArrays();

        if (isThumb()) {
            int pc = curInstrAddr & ~1;
            int instr = fetchHalfword(pc);
            regs[15] = curInstrAddr + 4;          // pipeline value visible to instruction
            cyc = decodeThumb(instr);
            if (!branchTaken) regs[15] = curInstrAddr + 2;
        } else {
            int pc = curInstrAddr & ~3;
            int instr = fetchWord(pc);
            regs[15] = curInstrAddr + 8;          // pipeline value visible to instruction
            if (checkCondition((instr >>> 28) & 0xF)) {
                cyc = decodeARM(instr);
            } else {
                cyc = 1;
            }
            if (!branchTaken) regs[15] = curInstrAddr + 4;
        }
        cycles += cyc;
        return cyc;
    }

    /**
     * Fast halfword fetch for the instruction stream. Avoids the page-switch in
     * MemoryBus.read16 for the three regions that hold ~all of the executing
     * code (IWRAM/EWRAM/ROM); falls back to the full bus path otherwise.
     */
    private int fetchHalfword(int pc) {
        int page = (pc >>> 24) & 0xFF;
        switch (page) {
            case 0x03: { // IWRAM (mirrors every 32 KB)
                int off = pc & 0x7FFE;
                return (iwramArr[off] & 0xFF) | ((iwramArr[off + 1] & 0xFF) << 8);
            }
            case 0x02: { // EWRAM (mirrors every 256 KB)
                int off = pc & 0x3FFFE;
                return (ewramArr[off] & 0xFF) | ((ewramArr[off + 1] & 0xFF) << 8);
            }
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: { // ROM
                int off = pc - 0x08000000;
                if (off + 1 < romLen) return (romArr[off] & 0xFF) | ((romArr[off + 1] & 0xFF) << 8);
                return 0;
            }
            default:
                return bus.read16(pc) & 0xFFFF;
        }
    }

    /** Same fast path for word-aligned instruction fetches in ARM mode. */
    private int fetchWord(int pc) {
        int page = (pc >>> 24) & 0xFF;
        switch (page) {
            case 0x03: {
                int off = pc & 0x7FFC;
                return (iwramArr[off] & 0xFF) | ((iwramArr[off + 1] & 0xFF) << 8)
                     | ((iwramArr[off + 2] & 0xFF) << 16) | ((iwramArr[off + 3] & 0xFF) << 24);
            }
            case 0x02: {
                int off = pc & 0x3FFFC;
                return (ewramArr[off] & 0xFF) | ((ewramArr[off + 1] & 0xFF) << 8)
                     | ((ewramArr[off + 2] & 0xFF) << 16) | ((ewramArr[off + 3] & 0xFF) << 24);
            }
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: {
                int off = pc - 0x08000000;
                if (off + 3 < romLen) return (romArr[off] & 0xFF) | ((romArr[off + 1] & 0xFF) << 8)
                     | ((romArr[off + 2] & 0xFF) << 16) | ((romArr[off + 3] & 0xFF) << 24);
                return 0;
            }
            default:
                return bus.read32(pc);
        }
    }

    // ── Fast data reads ──────────────────────────────────────────────────
    // Same idea as the instruction-fetch fast path but for the LDR/LDRH/LDRB
    // operations the CPU runs ~tens of millions of times per second. The full
    // MemoryBus.read* methods do a page-switch + several method dispatches per
    // call; for the common RAM/ROM cases we can read the underlying byte arrays
    // directly. Anything outside those pages (I/O, VRAM, palette, OAM, save
    // chip, …) keeps going through the bus so all the side effects still run.

    private int dataRead32(int addr) {
        int aligned = addr & ~3;
        int page = (aligned >>> 24) & 0xFF;
        int v;
        switch (page) {
            case 0x03: {
                int off = aligned & 0x7FFC;
                v = (iwramArr[off] & 0xFF) | ((iwramArr[off + 1] & 0xFF) << 8)
                  | ((iwramArr[off + 2] & 0xFF) << 16) | ((iwramArr[off + 3] & 0xFF) << 24);
                break;
            }
            case 0x02: {
                int off = aligned & 0x3FFFC;
                v = (ewramArr[off] & 0xFF) | ((ewramArr[off + 1] & 0xFF) << 8)
                  | ((ewramArr[off + 2] & 0xFF) << 16) | ((ewramArr[off + 3] & 0xFF) << 24);
                break;
            }
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: {
                int off = aligned - 0x08000000;
                if (off + 3 < romLen) {
                    v = (romArr[off] & 0xFF) | ((romArr[off + 1] & 0xFF) << 8)
                      | ((romArr[off + 2] & 0xFF) << 16) | ((romArr[off + 3] & 0xFF) << 24);
                } else v = 0;
                break;
            }
            default:
                v = bus.read32(aligned);
        }
        // Honour the ARM7TDMI's mandatory unaligned-read rotation (already done
        // by ldrWord, but dataRead32 also reaches non-Thumb callers).
        int rot = (addr & 3) * 8;
        return rot == 0 ? v : Integer.rotateRight(v, rot);
    }

    private int dataRead16(int addr) {
        int a = addr & ~1;
        int page = (a >>> 24) & 0xFF;
        switch (page) {
            case 0x03: {
                int off = a & 0x7FFE;
                return (iwramArr[off] & 0xFF) | ((iwramArr[off + 1] & 0xFF) << 8);
            }
            case 0x02: {
                int off = a & 0x3FFFE;
                return (ewramArr[off] & 0xFF) | ((ewramArr[off + 1] & 0xFF) << 8);
            }
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: {
                int off = a - 0x08000000;
                if (off + 1 < romLen) return (romArr[off] & 0xFF) | ((romArr[off + 1] & 0xFF) << 8);
                return 0;
            }
            default:
                return bus.read16(a) & 0xFFFF;
        }
    }

    private int dataRead8(int addr) {
        int page = (addr >>> 24) & 0xFF;
        switch (page) {
            case 0x03: return iwramArr[addr & 0x7FFF] & 0xFF;
            case 0x02: return ewramArr[addr & 0x3FFFF] & 0xFF;
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: {
                int off = addr - 0x08000000;
                return (off < romLen) ? (romArr[off] & 0xFF) : 0xFF;
            }
            default:
                return bus.read8(addr) & 0xFF;
        }
    }

    // ── ARM mode ───────────────────────────────────────────────────────────
    private int decodeARM(int instr) {
        int type = (instr >>> 25) & 0x7;
        int bit4 = (instr >>> 4) & 1;
        int bit7 = (instr >>> 7) & 1;

        // Decode
        if ((instr & 0x0FFFFFF0) == 0x012FFF10) {
            // BX
            return armBX(instr);
        } else if ((instr & 0x0E000000) == 0x0A000000) {
            // B / BL
            return armBranch(instr);
        } else if ((instr & 0x0FB00FF0) == 0x01000090) {
            // SWP / SWPB
            return armSWP(instr);
        } else if ((instr & 0x0FC000F0) == 0x00000090) {
            // MUL / MLA
            return armMultiply(instr);
        } else if ((instr & 0x0F8000F0) == 0x00800090) {
            // MULL / MLAL
            return armMultiplyLong(instr);
        } else if ((instr & 0x0E000090) == 0x00000090 && bit4 == 1 && bit7 == 1 && type < 2) {
            // Halfword data transfer
            return armHalfwordTransfer(instr);
        } else if ((instr & 0x0C000000) == 0x04000000) {
            // Single data transfer (LDR/STR)
            return armSingleTransfer(instr);
        } else if ((instr & 0x0E000000) == 0x08000000) {
            // Block data transfer (LDM/STM)
            return armBlockTransfer(instr);
        } else if ((instr & 0x0F000000) == 0x0F000000) {
            // SWI
            return armSWI(instr);
        } else if ((instr & 0x0C000000) == 0x00000000) {
            // Data processing / PSR transfer
            return armDataProcessing(instr);
        } else if ((instr & 0x0E000000) == 0x0C000000) {
            // Coprocessor - not used on GBA, ignore
            return 1;
        }

        return 1; // Undefined
    }

    // ── ARM: Condition check ───────────────────────────────────────────────
    private boolean checkCondition(int cond) {
        boolean n = (cpsr & FLAG_N) != 0;
        boolean z = (cpsr & FLAG_Z) != 0;
        boolean c = (cpsr & FLAG_C) != 0;
        boolean v = (cpsr & FLAG_V) != 0;
        switch (cond) {
            case 0x0: return z;
            case 0x1: return !z;
            case 0x2: return c;
            case 0x3: return !c;
            case 0x4: return n;
            case 0x5: return !n;
            case 0x6: return v;
            case 0x7: return !v;
            case 0x8: return c && !z;
            case 0x9: return !c || z;
            case 0xA: return n == v;
            case 0xB: return n != v;
            case 0xC: return !z && (n == v);
            case 0xD: return z || (n != v);
            case 0xE: return true;
            case 0xF: return false; // Never (or UNPREDICTABLE in some ARMs)
        }
        return false;
    }

    // ── ARM: Data Processing ───────────────────────────────────────────────
    private int armDataProcessing(int instr) {
        int opcode = (instr >>> 21) & 0xF;
        boolean s   = (instr & (1 << 20)) != 0;
        int rn = (instr >>> 16) & 0xF;
        int rd = (instr >>> 12) & 0xF;
        boolean imm = (instr & (1 << 25)) != 0;

        // MRS
        if ((instr & 0x0FBF0FFF) == 0x010F0000) {
            rd = (instr >>> 12) & 0xF;
            boolean useSPSR = (instr & (1 << 22)) != 0;
            regs[rd] = useSPSR ? spsr : cpsr;
            return 1;
        }
        // MSR (register form 0x0120F000 and immediate form 0x0320F000)
        if ((instr & 0x0DB0F000) == 0x0120F000) {
            boolean useSPSR = (instr & (1 << 22)) != 0;
            int operand = getShifterOperand(instr, false).value;
            int mask = 0;
            if ((instr & (1 << 16)) != 0) mask |= 0x000000FF;
            if ((instr & (1 << 17)) != 0) mask |= 0x0000FF00;
            if ((instr & (1 << 18)) != 0) mask |= 0x00FF0000;
            if ((instr & (1 << 19)) != 0) mask |= 0xFF000000;
            if (useSPSR) {
                spsr = (spsr & ~mask) | (operand & mask);
            } else {
                int oldMode = cpsr & 0x1F;
                cpsr = (cpsr & ~mask) | (operand & mask);
                int newMode = cpsr & 0x1F;
                if (oldMode != newMode) switchMode(oldMode, newMode);
            }
            return 1;
        }

        ShifterResult sr = getShifterOperand(instr, s);
        int rnVal = regs[rn];
        int op2 = sr.value;
        boolean shiftCarry = sr.carry;
        int result = 0;
        boolean writeResult = true;
        boolean logical = false;

        switch (opcode) {
            case 0x0: result = rnVal & op2; logical = true; break;            // AND
            case 0x1: result = rnVal ^ op2; logical = true; break;            // EOR
            case 0x2: result = rnVal - op2; if (s) setSubFlags(rnVal, op2, result, true); break; // SUB
            case 0x3: result = op2 - rnVal; if (s) setSubFlags(op2, rnVal, result, true); break; // RSB
            case 0x4: result = rnVal + op2; if (s) setAddFlags(rnVal, op2, result, true); break; // ADD
            case 0x5: { // ADC
                int carry = (cpsr & FLAG_C) != 0 ? 1 : 0;
                long r = Integer.toUnsignedLong(rnVal) + Integer.toUnsignedLong(op2) + carry;
                result = (int) r;
                if (s) setAddFlagsCarry(rnVal, op2, carry, result);
                break;
            }
            case 0x6: { // SBC
                int carry = (cpsr & FLAG_C) != 0 ? 1 : 0;
                result = (int)((long)rnVal - Integer.toUnsignedLong(op2) - (1 - carry));
                if (s) setSubFlagsCarry(rnVal, op2, carry, result);
                break;
            }
            case 0x7: { // RSC
                int carry = (cpsr & FLAG_C) != 0 ? 1 : 0;
                result = (int)((long)op2 - Integer.toUnsignedLong(rnVal) - (1 - carry));
                if (s) setSubFlagsCarry(op2, rnVal, carry, result);
                break;
            }
            case 0x8: result = rnVal & op2; writeResult = false; logical = true; break; // TST
            case 0x9: result = rnVal ^ op2; writeResult = false; logical = true; break; // TEQ
            case 0xA: result = rnVal - op2; writeResult = false; if (s) setSubFlags(rnVal, op2, result, true); break; // CMP
            case 0xB: result = rnVal + op2; writeResult = false; if (s) setAddFlags(rnVal, op2, result, true); break; // CMN
            case 0xC: result = rnVal | op2; logical = true; break;            // ORR
            case 0xD: result = op2; logical = true; break;                    // MOV
            case 0xE: result = rnVal & ~op2; logical = true; break;           // BIC
            case 0xF: result = ~op2; logical = true; break;                   // MVN
            default:  result = 0; break;
        }

        if (s && logical) {
            setNZFlags(result);
            setCPSRCarry(shiftCarry);
        }

        if (writeResult) {
            if (rd == 15) {
                if (s) {
                    int savedSpsr = spsr;
                    int oldMode = cpsr & 0x1F;
                    cpsr = savedSpsr;
                    int newMode = cpsr & 0x1F;
                    if (oldMode != newMode) switchMode(oldMode, newMode);
                }
                if (isThumb()) regs[15] = result & ~1; else regs[15] = result & ~3;
                branchTaken = true;
                return 3;
            }
            regs[rd] = result;
        }
        return 1;
    }

    private void setSubFlags(int a, int b, int result, boolean s) {
        if (!s) return;
        setNZFlags(result);
        setCPSRCarry((Integer.toUnsignedLong(a) >= Integer.toUnsignedLong(b)));
        setCPSROverflow(((a ^ b) & (a ^ result)) < 0);
    }

    private void setAddFlags(int a, int b, int result, boolean s) {
        if (!s) return;
        setNZFlags(result);
        setCPSRCarry(Integer.toUnsignedLong(a) + Integer.toUnsignedLong(b) > 0xFFFFFFFFL);
        setCPSROverflow(!((a ^ b) < 0) && ((a ^ result) < 0));
    }

    private void setAddFlagsCarry(int a, int b, int carry, int result) {
        setNZFlags(result);
        setCPSRCarry(Integer.toUnsignedLong(a) + Integer.toUnsignedLong(b) + carry > 0xFFFFFFFFL);
        setCPSROverflow(!((a ^ b) < 0) && ((a ^ result) < 0));
    }

    private void setSubFlagsCarry(int a, int b, int carry, int result) {
        setNZFlags(result);
        long ua = Integer.toUnsignedLong(a);
        long ub = Integer.toUnsignedLong(b);
        setCPSRCarry(ua >= ub + (1 - carry));
        setCPSROverflow(((a ^ b) & (a ^ result)) < 0);
    }

    private void setNZFlags(int result) {
        if (result < 0)  cpsr |= FLAG_N; else cpsr &= ~FLAG_N;
        if (result == 0) cpsr |= FLAG_Z; else cpsr &= ~FLAG_Z;
    }

    private void setCPSRCarry(boolean c) {
        if (c) cpsr |= FLAG_C; else cpsr &= ~FLAG_C;
    }

    private void setCPSROverflow(boolean v) {
        if (v) cpsr |= FLAG_V; else cpsr &= ~FLAG_V;
    }

    // ── Shifter operand ────────────────────────────────────────────────────
    private ShifterResult getShifterOperand(int instr, boolean s) {
        boolean immForm = (instr & (1 << 25)) != 0;
        if (immForm) {
            int imm8  = instr & 0xFF;
            int rot   = ((instr >>> 8) & 0xF) << 1;
            int val   = Integer.rotateRight(imm8, rot);
            boolean carry = rot == 0 ? ((cpsr & FLAG_C) != 0) : ((val >> 31) != 0);
            return new ShifterResult(val, carry);
        }
        // Register form
        int rm = instr & 0xF;
        int rmVal = regs[rm];
        int shiftType = (instr >>> 5) & 0x3;
        boolean regShift = (instr & (1 << 4)) != 0;
        int shiftAmt;
        if (regShift) {
            int rs = (instr >>> 8) & 0xF;
            // When a register specifies the shift amount, PC reads as +12 (instr+12).
            if (rm == 15) rmVal += 4;   // regs[15] is already instr+8
            shiftAmt = regs[rs] & 0xFF;
        } else {
            shiftAmt = (instr >>> 7) & 0x1F;
        }
        return applyShift(rmVal, shiftType, shiftAmt, regShift);
    }

    private ShifterResult applyShift(int val, int type, int amount, boolean isRegShift) {
        boolean carry = (cpsr & FLAG_C) != 0;
        if (amount == 0 && !isRegShift) {
            if (type == 3) {
                // RRX
                boolean oldCarry = carry;
                carry = (val & 1) != 0;
                val = (val >>> 1) | (oldCarry ? (1 << 31) : 0);
            }
            return new ShifterResult(val, carry);
        }
        switch (type) {
            case 0: // LSL
                if (amount >= 32) { carry = amount == 32 && (val & 1) != 0; val = 0; }
                else if (amount > 0) { carry = (val >>> (32 - amount)) != 0; val <<= amount; }
                break;
            case 1: // LSR
                if (amount >= 32) { carry = amount == 32 && (val < 0); val = 0; }
                else if (amount > 0) { carry = ((val >>> (amount - 1)) & 1) != 0; val >>>= amount; }
                break;
            case 2: // ASR
                if (amount >= 32) { carry = val < 0; val = val < 0 ? -1 : 0; }
                else if (amount > 0) { carry = ((val >>> (amount - 1)) & 1) != 0; val >>= amount; }
                break;
            case 3: // ROR
                amount &= 31;
                if (amount > 0) { carry = ((val >>> (amount - 1)) & 1) != 0; val = Integer.rotateRight(val, amount); }
                break;
        }
        return new ShifterResult(val, carry);
    }

    // ── ARM: Branch ────────────────────────────────────────────────────────
    private int armBranch(int instr) {
        boolean link = (instr & (1 << 24)) != 0;
        int offset = (instr << 8) >> 6;       // sign-extend 24-bit, <<2
        // regs[15] currently holds curInstrAddr + 8 (the ARM branch base).
        if (link) regs[14] = curInstrAddr + 4; // return address = instr + 4
        regs[15] = regs[15] + offset;
        branchTaken = true;
        return 3;
    }

    // ── ARM: Branch and Exchange ───────────────────────────────────────────
    private int armBX(int instr) {
        int rm = instr & 0xF;
        int target = regs[rm];
        if ((target & 1) != 0) {
            cpsr |= FLAG_T;
            regs[15] = target & ~1;
        } else {
            cpsr &= ~FLAG_T;
            regs[15] = target & ~3;
        }
        branchTaken = true;
        return 3;
    }

    // ── ARM: Multiply ──────────────────────────────────────────────────────
    private int armMultiply(int instr) {
        boolean accumulate = (instr & (1 << 21)) != 0;
        boolean s = (instr & (1 << 20)) != 0;
        int rd = (instr >>> 16) & 0xF;
        int rn = (instr >>> 12) & 0xF;
        int rs = (instr >>> 8)  & 0xF;
        int rm = instr & 0xF;

        long result = Integer.toUnsignedLong(regs[rm]) * Integer.toUnsignedLong(regs[rs]);
        if (accumulate) result += regs[rn];
        regs[rd] = (int) result;
        if (s) setNZFlags(regs[rd]);
        return accumulate ? 4 : 3;
    }

    private int armMultiplyLong(int instr) {
        boolean sign     = (instr & (1 << 22)) != 0;
        boolean accumulate = (instr & (1 << 21)) != 0;
        boolean s        = (instr & (1 << 20)) != 0;
        int rdHi = (instr >>> 16) & 0xF;
        int rdLo = (instr >>> 12) & 0xF;
        int rs   = (instr >>> 8)  & 0xF;
        int rm   = instr & 0xF;

        long result;
        if (sign) {
            result = (long)regs[rm] * (long)regs[rs];
        } else {
            result = Integer.toUnsignedLong(regs[rm]) * Integer.toUnsignedLong(regs[rs]);
        }
        if (accumulate) {
            long acc = ((long)regs[rdHi] << 32) | Integer.toUnsignedLong(regs[rdLo]);
            result += acc;
        }
        regs[rdLo] = (int) result;
        regs[rdHi] = (int)(result >>> 32);
        if (s) {
            if (result < 0) cpsr |= FLAG_N; else cpsr &= ~FLAG_N;
            if (result == 0) cpsr |= FLAG_Z; else cpsr &= ~FLAG_Z;
        }
        return accumulate ? 5 : 4;
    }

    // ── ARM: SWP ──────────────────────────────────────────────────────────
    private int armSWP(int instr) {
        boolean byte_ = (instr & (1 << 22)) != 0;
        int rn = (instr >>> 16) & 0xF;
        int rd = (instr >>> 12) & 0xF;
        int rm = instr & 0xF;
        int addr = regs[rn];
        if (byte_) {
            int tmp = bus.read8(addr) & 0xFF;
            bus.write8(addr, (byte) regs[rm]);
            regs[rd] = tmp;
        } else {
            int tmp = ldrWord(addr);
            bus.write32(addr, regs[rm]);
            regs[rd] = tmp;
        }
        return 4;
    }

    // ── ARM: Single Data Transfer ──────────────────────────────────────────
    private int armSingleTransfer(int instr) {
        boolean imm    = (instr & (1 << 25)) == 0;
        boolean pre    = (instr & (1 << 24)) != 0;
        boolean up     = (instr & (1 << 23)) != 0;
        boolean byte_  = (instr & (1 << 22)) != 0;
        boolean wb     = (instr & (1 << 21)) != 0;
        boolean load   = (instr & (1 << 20)) != 0;
        int rn = (instr >>> 16) & 0xF;
        int rd = (instr >>> 12) & 0xF;

        int base = regs[rn];   // for rn==15, regs[15] already = instr+8

        int offset;
        if (imm) {
            offset = instr & 0xFFF;
        } else {
            int rm = instr & 0xF;
            int shiftType = (instr >>> 5) & 0x3;
            int shiftAmt  = (instr >>> 7) & 0x1F;
            offset = applyShift(regs[rm], shiftType, shiftAmt, false).value;
        }

        int addr = pre ? (up ? base + offset : base - offset) : base;

        if (load) {
            if (byte_) {
                regs[rd] = dataRead8(addr);
            } else {
                regs[rd] = dataRead32(addr);  // already includes the unaligned rotation
            }
        } else {
            int val = regs[rd];
            if (rd == 15) val += 4;   // stored PC = instr+12 (regs[15] is instr+8)
            if (byte_) bus.write8(addr, (byte) val);
            else        bus.write32(addr, val);
        }

        if (!pre) addr = up ? base + offset : base - offset;
        if ((!load || rd != rn) && (wb || !pre)) regs[rn] = addr;

        if (load && rd == 15) { regs[15] &= ~3; branchTaken = true; }

        return load ? 3 : 2;
    }

    // ── ARM: Halfword Data Transfer ────────────────────────────────────────
    private int armHalfwordTransfer(int instr) {
        boolean pre    = (instr & (1 << 24)) != 0;
        boolean up     = (instr & (1 << 23)) != 0;
        boolean imm    = (instr & (1 << 22)) != 0;
        boolean wb     = (instr & (1 << 21)) != 0;
        boolean load   = (instr & (1 << 20)) != 0;
        int rn = (instr >>> 16) & 0xF;
        int rd = (instr >>> 12) & 0xF;
        int sh = (instr >>> 5) & 0x3;

        int offset = imm ? ((instr & 0xF00) >>> 4) | (instr & 0xF) : regs[instr & 0xF];
        int base   = regs[rn];
        int addr   = pre ? (up ? base + offset : base - offset) : base;

        if (load) {
            switch (sh) {
                case 1: regs[rd] = dataRead16(addr); break;            // LDRH
                case 2: regs[rd] = (byte)(dataRead8(addr)); break;     // LDRSB
                case 3: regs[rd] = (short)(dataRead16(addr)); break;   // LDRSH
            }
        } else {
            if (sh == 1) bus.write16(addr, (short) regs[rd]);                  // STRH
        }

        if (!pre) addr = up ? base + offset : base - offset;
        if (!load || rd != rn) {
            if (wb || !pre) regs[rn] = addr;
        }
        return load ? 3 : 2;
    }

    // ── ARM: Block Data Transfer ───────────────────────────────────────────
    private int armBlockTransfer(int instr) {
        boolean pre    = (instr & (1 << 24)) != 0;
        boolean up     = (instr & (1 << 23)) != 0;
        boolean s      = (instr & (1 << 22)) != 0;
        boolean wb     = (instr & (1 << 21)) != 0;
        boolean load   = (instr & (1 << 20)) != 0;
        int rn = (instr >>> 16) & 0xF;
        int regList = instr & 0xFFFF;

        int base = regs[rn];
        int count = Integer.bitCount(regList);
        // Final base after the transfer (the writeback value), independent of P.
        int finalAddr = up ? base + (count * 4) : base - (count * 4);

        // Lowest-numbered register always goes to the lowest address.
        // Compute the starting (lowest) address for each of the 4 modes:
        //   IA(P=0,U=1)=base   IB(P=1,U=1)=base+4
        //   DA(P=0,U=0)=finalAddr+4   DB(P=1,U=0)=finalAddr
        int addr;
        if (up) addr = base + (pre ? 4 : 0);
        else    addr = finalAddr + (pre ? 0 : 4);

        for (int i = 0; i < 16; i++) {
            if ((regList & (1 << i)) != 0) {
                if (load) {
                    regs[i] = dataRead32(addr);
                    if (i == 15) { regs[15] &= ~3; branchTaken = true; }
                } else {
                    int val = regs[i];
                    if (i == 15) val += 4;   // stored PC = instr+12
                    bus.write32(addr, val);
                }
                addr += 4;
            }
        }

        if (wb && (!load || (regList & (1 << rn)) == 0)) {
            regs[rn] = finalAddr;
        }

        if (s && load && (regList & (1 << 15)) != 0) {
            cpsr = spsr;
        }

        return count + (load ? 2 : 1);
    }

    // ── HLE BIOS hook ───────────────────────────────────────────────────
    private com.gbaminecraft.emulator.bios.HleBios hleBios;
    private boolean useHleBios = true;

    public void setHleBios(com.gbaminecraft.emulator.bios.HleBios b) { this.hleBios = b; }
    public void setUseHleBios(boolean v) { this.useHleBios = v; }

    // Optional tracer hook: when set, hleIrqEnter notifies it on every dispatch
    // to the user IRQ handler so the diagnostic report's "Handler-juego ejecutado"
    // counter reflects reality. Defined as nullable so the field stays harmless
    // for embedders that don't ship the debug package.
    private com.gbaminecraft.emulator.debug.BootTracer tracer;
    public void setTracer(com.gbaminecraft.emulator.debug.BootTracer t) { this.tracer = t; }

    /** Force a pipeline reload (used by BIOS SoftReset and external jumps). */
    public void flushPipeline() { branchTaken = true; }

    // ── ARM: SWI ──────────────────────────────────────────────────────────
    private int armSWI(int instr) {
        // ARM SWI comment field is bits 23..0; the BIOS function is its high byte.
        int swiNum = (instr >>> 16) & 0xFF;
        if (useHleBios && hleBios != null) {
            hleBios.handle(swiNum);   // serviced in Java; just continue execution
            // IntrWait/VBlankIntrWait: while waiting, rewind PC to re-execute this
            // SWI after the next IRQ wakes the CPU, so the wait condition is
            // re-checked instead of falling through immediately.
            if (hleBios.intrWaitActive) { regs[15] = curInstrAddr; branchTaken = true; }
            return 3;
        }
        enterException(MODE_SVC, curInstrAddr + 4, true);
        return 3;
    }

    /**
     * Common exception entry: bank registers into the target mode, store the
     * return address in that mode's LR and the old CPSR in its SPSR, then jump
     * to the vector.
     */
    private void enterException(int newMode, int returnAddr, boolean toVectorSWI) {
        int oldCpsr = cpsr;
        int oldMode = cpsr & 0x1F;
        switchMode(oldMode, newMode);     // bank R13/R14 of old, load new
        regs[14] = returnAddr;            // LR (now the new mode's view)
        spsr = oldCpsr;                   // SPSR (now the new mode's view)
        int si = spsrIndex(newMode);
        if (si >= 0) bankedSPSR[si] = oldCpsr;
        cpsr = (cpsr & ~0x1F) | newMode;
        cpsr |= FLAG_I;                   // disable IRQ
        cpsr &= ~FLAG_T;                  // ARM state
        regs[15] = (newMode == MODE_IRQ) ? 0x18 : 0x08;
        branchTaken = true;
    }

    // ── Thumb mode ─────────────────────────────────────────────────────────
    private int decodeThumb(int instr) {
        int top5 = instr >>> 11;
        int top3 = instr >>> 13;

        if (top5 == 0x1E || top5 == 0x1F) { return thumbBL(instr, curInstrAddr); }
        else if ((instr & 0xFF00) == 0xDF00) { return thumbSWI(instr); }
        else if ((instr & 0xFF00) == 0xBE00) { return 1; } // BKPT
        else if ((instr & 0xF000) == 0xE000) { return thumbBranch(instr); }
        else if ((instr & 0xF000) == 0xD000) { return thumbCondBranch(instr); }
        else if ((instr & 0xF000) == 0xC000) { return thumbLDMSTM(instr); }
        else if ((instr & 0xFF00) == 0xB000) { return thumbAddSP(instr); }
        else if ((instr & 0xF600) == 0xB400) { return thumbPushPop(instr); }
        else if ((instr & 0xF000) == 0xA000) { return thumbLoadAddr(instr); }
        else if ((instr & 0xF000) == 0x9000) { return thumbSPRelLoad(instr); }
        else if ((instr & 0xF000) == 0x8000) { return thumbHalfword(instr); }
        else if ((instr & 0xE000) == 0x6000) { return thumbLoadStore(instr); }
        else if ((instr & 0xF200) == 0x5200) { return thumbLoadStoreSH(instr); }
        else if ((instr & 0xF200) == 0x5000) { return thumbLoadStoreReg(instr); }
        else if ((instr & 0xF800) == 0x4800) { return thumbLdrPcRel(instr); }
        else if ((instr & 0xE000) == 0x4000) {
            // Format 5: hi-register ops / BX occupy 0x4400-0x47FF.
            //   0x44xx ADD, 0x45xx CMP, 0x46xx MOV, 0x47xx BX/BLX
            if ((instr & 0xFC00) == 0x4400) {
                int op = (instr >>> 8) & 0x3;     // 0=ADD,1=CMP,2=MOV,3=BX
                if (op == 3) return thumbBX(instr);
                return thumbHiOps(instr);
            }
            // 0x40xx-0x43xx: the ALU operations.
            return thumbALU(instr);
        }
        else if ((instr & 0xE000) == 0x2000) { return thumbImm(instr); }
        else if ((instr & 0xF800) == 0x1800) { return thumbAddSub(instr); }
        else if (top3 == 0) { return thumbMoveShift(instr); }

        return 1;
    }

    private int thumbMoveShift(int instr) {
        int op     = (instr >>> 11) & 0x3;
        int offset = (instr >>> 6) & 0x1F;
        int rs     = (instr >>> 3) & 0x7;
        int rd     = instr & 0x7;
        ShifterResult sr = applyShift(regs[rs], op, offset, false);
        regs[rd] = sr.value;
        setNZFlags(regs[rd]);
        setCPSRCarry(sr.carry);
        cpsr &= ~FLAG_V;
        return 1;
    }

    private int thumbAddSub(int instr) {
        boolean imm = (instr & (1 << 10)) != 0;
        boolean sub = (instr & (1 << 9))  != 0;
        int rs = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int op2 = imm ? ((instr >>> 6) & 0x7) : regs[(instr >>> 6) & 0x7];
        if (sub) { regs[rd] = regs[rs] - op2; setSubFlags(regs[rs], op2, regs[rd], true); }
        else      { regs[rd] = regs[rs] + op2; setAddFlags(regs[rs], op2, regs[rd], true); }
        return 1;
    }

    private int thumbImm(int instr) {
        int op  = (instr >>> 11) & 0x3;
        int rd  = (instr >>> 8)  & 0x7;
        int imm = instr & 0xFF;
        switch (op) {
            case 0: regs[rd] = imm; setNZFlags(regs[rd]); break; // MOV
            case 1: { long r = Integer.toUnsignedLong(regs[rd]) - imm; setSubFlags(regs[rd], imm, (int)r, true); break; } // CMP
            case 2: { int r = regs[rd] + imm; setAddFlags(regs[rd], imm, r, true); regs[rd] = r; break; } // ADD
            case 3: { int r = regs[rd] - imm; setSubFlags(regs[rd], imm, r, true); regs[rd] = r; break; } // SUB
        }
        return 1;
    }

    private int thumbALU(int instr) {
        int op = (instr >>> 6) & 0xF;
        int rs = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int rsVal = regs[rs];
        int rdVal = regs[rd];
        int result;
        switch (op) {
            case 0x0: result = rdVal & rsVal; setNZFlags(result); regs[rd] = result; break; // AND
            case 0x1: result = rdVal ^ rsVal; setNZFlags(result); regs[rd] = result; break; // EOR
            case 0x2: { // LSL
                int amt = rsVal & 0xFF;
                boolean c = amt == 0 ? ((cpsr & FLAG_C) != 0) : amt < 32 ? ((rdVal >>> (32 - amt)) & 1) != 0 : (amt == 32 && (rdVal & 1) != 0);
                result = amt >= 32 ? 0 : rdVal << amt;
                regs[rd] = result; setNZFlags(result); setCPSRCarry(c); break;
            }
            case 0x3: { // LSR
                int amt = rsVal & 0xFF;
                boolean c = amt == 0 ? ((cpsr & FLAG_C) != 0) : amt < 32 ? ((rdVal >>> (amt-1)) & 1) != 0 : (amt == 32 && rdVal < 0);
                result = amt >= 32 ? 0 : rdVal >>> amt;
                regs[rd] = result; setNZFlags(result); setCPSRCarry(c); break;
            }
            case 0x4: { // ASR
                int amt = rsVal & 0xFF; if (amt > 31) amt = 31;
                boolean c = ((rdVal >>> (amt == 0 ? 0 : amt-1)) & 1) != 0;
                result = rdVal >> amt;
                regs[rd] = result; setNZFlags(result); setCPSRCarry(c); break;
            }
            case 0x5: { int c=(cpsr&FLAG_C)!=0?1:0; result=rdVal+rsVal+c; setAddFlagsCarry(rdVal,rsVal,c,result); regs[rd]=result; break; } // ADC
            case 0x6: { int c=(cpsr&FLAG_C)!=0?1:0; result=rdVal-rsVal-(1-c); setSubFlagsCarry(rdVal,rsVal,c,result); regs[rd]=result; break; } // SBC
            case 0x7: { // ROR
                int amt = rsVal & 0xFF;
                boolean c = amt == 0 ? ((cpsr & FLAG_C) != 0) : ((rdVal >>> ((amt-1)&31)) & 1) != 0;
                result = amt == 0 ? rdVal : Integer.rotateRight(rdVal, amt & 31);
                regs[rd] = result; setNZFlags(result); setCPSRCarry(c); break;
            }
            case 0x8: result = rdVal & rsVal; setNZFlags(result); break; // TST
            case 0x9: result = -rsVal; setSubFlags(0, rsVal, result, true); regs[rd] = result; break; // NEG
            case 0xA: { long r=(long)rdVal-rsVal; setSubFlags(rdVal,rsVal,(int)r,true); break; } // CMP
            case 0xB: { long r=(long)rdVal+rsVal; setAddFlags(rdVal,rsVal,(int)r,true); break; } // CMN
            case 0xC: result = rdVal | rsVal; setNZFlags(result); regs[rd] = result; break; // ORR
            case 0xD: { result = rdVal * rsVal; regs[rd] = result; setNZFlags(result); break; } // MUL
            case 0xE: result = rdVal & ~rsVal; setNZFlags(result); regs[rd] = result; break; // BIC
            case 0xF: result = ~rsVal; setNZFlags(result); regs[rd] = result; break; // MVN
            default: break;
        }
        return 1;
    }

    private int thumbHiOps(int instr) {
        int op  = (instr >>> 8) & 0x3;
        int h1  = (instr >>> 7) & 0x1;
        int h2  = (instr >>> 6) & 0x1;
        int rs  = ((instr >>> 3) & 0x7) | (h2 << 3);
        int rd  = (instr & 0x7) | (h1 << 3);
        int rsVal = regs[rs];
        switch (op) {
            case 0: regs[rd] = regs[rd] + rsVal; if (rd == 15) { regs[15] &= ~1; branchTaken = true; } break; // ADD
            case 1: { long r = (long)regs[rd] - rsVal; setSubFlags(regs[rd], rsVal, (int)r, true); break; } // CMP
            case 2: regs[rd] = rsVal; if (rd == 15) { regs[15] &= ~1; branchTaken = true; } break; // MOV
        }
        return op == 0 || op == 2 ? (rd == 15 ? 3 : 1) : 1;
    }

    private int thumbBX(int instr) {
        int rs = (instr >>> 3) & 0xF;
        int target = regs[rs];
        if ((target & 1) != 0) {
            cpsr |= FLAG_T;
            regs[15] = target & ~1;
        } else {
            cpsr &= ~FLAG_T;
            regs[15] = target & ~3;
        }
        branchTaken = true;
        return 3;
    }

    /** Format 6: LDR Rd, [PC, #imm8*4]. PC is (curInstrAddr+4) word-aligned. */
    private int thumbLdrPcRel(int instr) {
        int rd = (instr >>> 8) & 0x7;
        int offset = (instr & 0xFF) << 2;
        int base = (curInstrAddr + 4) & ~3;   // PC with bit1 forced to 0
        regs[rd] = dataRead32(base + offset);
        return 3;
    }

    /**
     * ARM7TDMI word load with the mandatory unaligned-access rotation.
     */
    private int ldrWord(int addr) {
        return dataRead32(addr); // dataRead32 already applies the rotation
    }

    private int thumbLoadStore(int instr) {
        boolean load   = (instr & (1 << 11)) != 0;
        boolean byte_  = (instr & (1 << 12)) != 0;
        int offset = ((instr >>> 6) & 0x1F) << (byte_ ? 0 : 2);
        int rb = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int addr = regs[rb] + offset;
        if (load) {
            regs[rd] = byte_ ? dataRead8(addr) : ldrWord(addr);
        } else {
            if (byte_) bus.write8(addr, (byte) regs[rd]);
            else       bus.write32(addr, regs[rd]);
        }
        return load ? 3 : 2;
    }

    private int thumbLoadStoreReg(int instr) {
        boolean load = (instr & (1 << 11)) != 0;
        boolean byte_ = (instr & (1 << 10)) != 0;
        int ro = (instr >>> 6) & 0x7;
        int rb = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int addr = regs[rb] + regs[ro];
        if (load) {
            regs[rd] = byte_ ? dataRead8(addr) : ldrWord(addr);
        } else {
            if (byte_) bus.write8(addr, (byte) regs[rd]);
            else       bus.write32(addr, regs[rd]);
        }
        return load ? 3 : 2;
    }

    private int thumbLoadStoreSH(int instr) {
        int op = (instr >>> 10) & 0x3;
        int ro = (instr >>> 6) & 0x7;
        int rb = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int addr = regs[rb] + regs[ro];
        switch (op) {
            case 0: bus.write16(addr, (short) regs[rd]); break;  // STRH
            case 1: regs[rd] = (byte)(dataRead8(addr)); break;   // LDSB
            case 2: regs[rd] = dataRead16(addr); break;          // LDRH
            case 3: regs[rd] = (short)(dataRead16(addr)); break; // LDSH
        }
        return op == 0 ? 2 : 3;
    }

    private int thumbHalfword(int instr) {
        boolean load   = (instr & (1 << 11)) != 0;
        int offset = ((instr >>> 6) & 0x1F) << 1;
        int rb = (instr >>> 3) & 0x7;
        int rd = instr & 0x7;
        int addr = regs[rb] + offset;
        if (load) regs[rd] = dataRead16(addr);
        else      bus.write16(addr, (short) regs[rd]);
        return load ? 3 : 2;
    }

    private int thumbSPRelLoad(int instr) {
        boolean load = (instr & (1 << 11)) != 0;
        int rd  = (instr >>> 8) & 0x7;
        int offset = (instr & 0xFF) << 2;
        int addr = regs[13] + offset;
        if (load) regs[rd] = ldrWord(addr);
        else      bus.write32(addr, regs[rd]);
        return load ? 3 : 2;
    }

    private int thumbLoadAddr(int instr) {
        boolean sp = (instr & (1 << 11)) != 0;
        int rd  = (instr >>> 8) & 0x7;
        int offset = (instr & 0xFF) << 2;
        if (sp) regs[rd] = regs[13] + offset;
        else    regs[rd] = (regs[15] & ~3) + offset;
        return 1;
    }

    private int thumbAddSP(int instr) {
        boolean neg = (instr & (1 << 7)) != 0;
        int offset  = (instr & 0x7F) << 2;
        regs[13] += neg ? -offset : offset;
        return 1;
    }

    private int thumbPushPop(int instr) {
        boolean load  = (instr & (1 << 11)) != 0;
        boolean lr    = (instr & (1 << 8))  != 0;
        int regList   = instr & 0xFF;

        if (!load) {
            if (lr) regs[13] -= 4;
            for (int i = 7; i >= 0; i--) {
                if ((regList & (1 << i)) != 0) regs[13] -= 4;
            }
            int addr = regs[13];
            for (int i = 0; i < 8; i++) {
                if ((regList & (1 << i)) != 0) { bus.write32(addr, regs[i]); addr += 4; }
            }
            if (lr) { bus.write32(addr, regs[14]); }
        } else {
            int addr = regs[13];
            for (int i = 0; i < 8; i++) {
                if ((regList & (1 << i)) != 0) { regs[i] = dataRead32(addr); addr += 4; regs[13] += 4; }
            }
            if (lr) {
                int target = dataRead32(addr);
                regs[13] += 4;
                if ((target & 1) != 0) { regs[15] = target & ~1; }
                else { cpsr &= ~FLAG_T; regs[15] = target & ~3; }
                branchTaken = true;
            }
        }
        return Integer.bitCount(regList) + (lr ? 1 : 0) + (load ? 2 : 1);
    }

    private int thumbLDMSTM(int instr) {
        boolean load  = (instr & (1 << 11)) != 0;
        int rb      = (instr >>> 8) & 0x7;
        int regList = instr & 0xFF;
        int addr    = regs[rb];

        for (int i = 0; i < 8; i++) {
            if ((regList & (1 << i)) != 0) {
                if (load) regs[i] = dataRead32(addr);
                else      bus.write32(addr, regs[i]);
                addr += 4;
            }
        }
        regs[rb] = addr;
        return Integer.bitCount(regList) + (load ? 2 : 1);
    }

    private int thumbCondBranch(int instr) {
        int cond   = (instr >>> 8) & 0xF;
        if (!checkCondition(cond)) return 1;
        int offset = (int)(byte)(instr & 0xFF);
        regs[15] += offset << 1;
        branchTaken = true;
        return 3;
    }

    private int thumbBranch(int instr) {
        int offset = ((instr & 0x7FF) << 21) >> 20;
        regs[15] += offset;
        branchTaken = true;
        return 3;
    }

    private int thumbBL(int instr, int pc) {
        boolean second = (instr & (1 << 11)) != 0;
        if (!second) {
            int off = ((instr & 0x7FF) << 21) >> 9;
            regs[14] = regs[15] + off;
        } else {
            int off = (instr & 0x7FF) << 1;
            int target = regs[14] + off;
            regs[14] = (curInstrAddr + 2) | 1;  // return address | thumb bit
            regs[15] = target & ~1;
            branchTaken = true;
            return 3;
        }
        return 1;
    }

    private int thumbSWI(int instr) {
        // Thumb SWI comment is the low 8 bits.
        int swiNum = instr & 0xFF;
        if (useHleBios && hleBios != null) {
            hleBios.handle(swiNum);
            if (hleBios.intrWaitActive) { regs[15] = curInstrAddr; branchTaken = true; }
            return 3;
        }
        enterException(MODE_SVC, curInstrAddr + 2, true);
        return 3;
    }

    // ── Interrupts ─────────────────────────────────────────────────────────
    // Sentinel return address used by the HLE BIOS IRQ dispatcher. When the
    // game's IRQ handler returns to this address, we restore the saved context.
    private static final int IRQ_RETURN_SENTINEL = 0xF000F000;
    private boolean inHleIrq = false;
    private int hleIrqSavedCpsr = 0;
    private final int[] hleIrqSaved = new int[7]; // r0-r3, r12, lr, and old mode marker

    public void triggerIRQ() {
        if ((cpsr & FLAG_I) != 0) return;        // IRQs masked
        halted = false;

        if (useHleBios) {
            hleIrqEnter();
            return;
        }
        enterException(MODE_IRQ, curInstrAddr + 4, false);
    }

    /**
     * Emulates the GBA BIOS IRQ handler entirely in Java:
     *   - switch to IRQ mode, save SPSR
     *   - save r0-r3,r12 of the interrupted code (the real BIOS pushes these to
     *     the IRQ stack before calling the user handler, and pops them after)
     *   - jump to the user handler stored at [0x03007FFC]
     *   - return address is a sentinel we intercept to unwind.
     */
    private void hleIrqEnter() {
        if (inHleIrq) return; // simple non-nested model
        int oldCpsr = cpsr;
        int oldMode = cpsr & 0x1F;
        // IRQs are checked between instructions, so regs[15] already points at
        // the next instruction to execute. That is exactly where we resume.
        int returnPc = regs[15];

        // The real BIOS preserves r0-r3 and r12 across the IRQ. The user handler
        // (e.g. Pokémon's dispatcher) freely clobbers them — and crucially it
        // overwrites r1-r3 with IE/IF *before* doing its own push, so without
        // the BIOS-level save the interrupted code's r0-r3/r12 are lost. Capture
        // them here and restore on unwind.
        hleIrqSaved[1] = regs[0];
        hleIrqSaved[2] = regs[1];
        hleIrqSaved[3] = regs[2];
        hleIrqSaved[4] = regs[3];
        hleIrqSaved[5] = regs[12];

        switchMode(oldMode, MODE_IRQ);
        spsr = oldCpsr;
        int si = spsrIndex(MODE_IRQ);
        if (si >= 0) bankedSPSR[si] = oldCpsr;
        cpsr = (cpsr & ~0x1F) | MODE_IRQ;
        cpsr |= FLAG_I;
        cpsr &= ~FLAG_T;

        // Save context for the unwind (we keep it in Java rather than the stack
        // to stay robust; the user handler still sees a valid SP).
        hleIrqSavedCpsr = oldCpsr;
        hleIrqSaved[0] = returnPc;     // where to resume after the IRQ
        inHleIrq = true;

        // The real BIOS sets the IRQ-pending mirror at 0x03007FF8 (REG_IFBIOS)
        // by OR-ing in the IF bits THAT ARE ENABLED (IE & IF). Games read this
        // mirror in their handler to decide which callback to run.
        int ie = bus.read16(0x04000200);
        int ifNow = bus.read16(0x04000202) & ie;
        int mirror = bus.read16(0x03007FF8);
        bus.write16(0x03007FF8, (short)(mirror | ifNow));

        int handler = bus.read32(0x03007FFC);
        if (handler == 0) {
            // No handler installed: just return immediately.
            inHleIrq = false;
            cpsr = oldCpsr;
            switchMode(MODE_IRQ, oldMode);
            return;
        }
        regs[14] = IRQ_RETURN_SENTINEL;  // handler returns here (bx lr / mov pc,lr)
        regs[15] = handler & ~3;
        branchTaken = true;
        if (tracer != null) tracer.onIrqHandlerRun();
    }

    /** Called from step() when PC hits the sentinel: unwind the HLE IRQ. */
    private void hleIrqReturn() {
        int oldCpsr = hleIrqSavedCpsr;
        int oldMode = oldCpsr & 0x1F;
        switchMode(MODE_IRQ, oldMode);
        cpsr = oldCpsr;
        // Restore r0-r3,r12 of the interrupted code (BIOS-level preservation).
        regs[0]  = hleIrqSaved[1];
        regs[1]  = hleIrqSaved[2];
        regs[2]  = hleIrqSaved[3];
        regs[3]  = hleIrqSaved[4];
        regs[12] = hleIrqSaved[5];
        regs[15] = hleIrqSaved[0];
        inHleIrq = false;
        branchTaken = true;
    }

    // ── Mode switching ─────────────────────────────────────────────────────
    private void switchMode(int oldMode, int newMode) {
        if (oldMode == newMode) return;
        saveMode(oldMode);
        // R8-R12 are banked ONLY for FIQ. They are shared across User/SYS/IRQ/
        // SVC/ABT/UND. Swap them exclusively when crossing the FIQ boundary, so
        // a plain IRQ<->System switch (as the BIOS IRQ dispatcher does) must NOT
        // disturb R8-R12. Corrupting R12 here was breaking Pokémon's interrupt
        // dispatcher (it keeps the gIntrTable index in R12 across the switch).
        boolean toFiq   = newMode == MODE_FIQ;
        boolean fromFiq = oldMode == MODE_FIQ;
        if (toFiq && !fromFiq) {
            // Entering FIQ: save the shared R8-R12, load the FIQ bank.
            bankedR8Fiq[0]  = regs[8];  bankedR9Fiq[0]  = regs[9];
            bankedR10Fiq[0] = regs[10]; bankedR11Fiq[0] = regs[11];
            bankedR12Fiq[0] = regs[12];
            regs[8]  = bankedR8Fiq[1];  regs[9]  = bankedR9Fiq[1];
            regs[10] = bankedR10Fiq[1]; regs[11] = bankedR11Fiq[1];
            regs[12] = bankedR12Fiq[1];
        } else if (fromFiq && !toFiq) {
            // Leaving FIQ: save the FIQ bank, restore the shared R8-R12.
            bankedR8Fiq[1]  = regs[8];  bankedR9Fiq[1]  = regs[9];
            bankedR10Fiq[1] = regs[10]; bankedR11Fiq[1] = regs[11];
            bankedR12Fiq[1] = regs[12];
            regs[8]  = bankedR8Fiq[0];  regs[9]  = bankedR9Fiq[0];
            regs[10] = bankedR10Fiq[0]; regs[11] = bankedR11Fiq[0];
            regs[12] = bankedR12Fiq[0];
        }
        loadMode(newMode);
    }

    private void saveMode(int mode) {
        int idx = modeIndex(mode);
        bankedR13[idx] = regs[13];
        bankedR14[idx] = regs[14];
    }

    private void loadMode(int mode) {
        int idx = modeIndex(mode);
        regs[13] = bankedR13[idx];
        regs[14] = bankedR14[idx];
        int si = spsrIndex(mode);
        if (si >= 0) spsr = bankedSPSR[si];
    }

    private int modeIndex(int mode) {
        switch (mode) {
            case MODE_USER: case MODE_SYS: return 0;
            case MODE_FIQ: return 1;
            case MODE_IRQ: return 2;
            case MODE_SVC: return 3;
            case MODE_ABT: return 4;
            case MODE_UND: return 5;
            default:       return 0;
        }
    }

    private int spsrIndex(int mode) {
        switch (mode) {
            case MODE_FIQ: return 0;
            case MODE_IRQ: return 1;
            case MODE_SVC: return 2;
            case MODE_ABT: return 3;
            case MODE_UND: return 4;
            default:       return -1;
        }
    }

    public boolean isThumb() { return (cpsr & FLAG_T) != 0; }

    public int getPC() { return regs[15]; }
    public void setPC(int pc) { regs[15] = pc; branchTaken = true; }

    // ── Inner record ──────────────────────────────────────────────────────
    private static class ShifterResult {
        final int value;
        final boolean carry;
        ShifterResult(int v, boolean c) { value = v; carry = c; }
    }
}
