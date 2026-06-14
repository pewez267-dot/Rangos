package com.gbaminecraft.emulator.memory;

import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.input.GBAInput;
import com.gbaminecraft.emulator.timer.TimerController;
import com.gbaminecraft.emulator.dma.DMAController;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;

/**
 * GBA Memory Bus — handles all memory-mapped I/O and memory regions.
 *
 * GBA Memory Map:
 *   0x00000000-0x00003FFF  BIOS ROM (16 KB)
 *   0x02000000-0x0203FFFF  External Work RAM (256 KB)
 *   0x03000000-0x03007FFF  Internal Work RAM (32 KB)
 *   0x04000000-0x040003FF  I/O Registers
 *   0x05000000-0x050003FF  Palette RAM (1 KB)
 *   0x06000000-0x06017FFF  VRAM (96 KB)
 *   0x07000000-0x070003FF  OAM (1 KB)
 *   0x08000000-0x09FFFFFF  ROM (up to 32 MB)
 *   0x0E000000-0x0E00FFFF  Cart RAM / SRAM (64 KB)
 */
public class MemoryBus {

    // Memory regions
    private byte[] bios   = new byte[0x4000];       // 16 KB BIOS
    private byte[] ewram  = new byte[0x40000];      // 256 KB External WRAM
    private byte[] iwram  = new byte[0x8000];       // 32 KB Internal WRAM
    private byte[] palette= new byte[0x400];        // 1 KB Palette
    private byte[] vram   = new byte[0x18000];      // 96 KB VRAM
    private byte[] oam    = new byte[0x400];        // 1 KB OAM
    private byte[] rom    = new byte[0];            // ROM (variable)
    private byte[] sram   = new byte[0x10000];      // 64 KB SRAM

    // I/O registers (0x04000000 - 0x040003FF)
    private byte[] io = new byte[0x400];

    // Subsystem references
    private PPU ppu;
    private APU apu;
    private GBAInput input;
    private TimerController timers;
    private DMAController dma;
    private ARM7TDMI cpu;

    // Open bus value
    private int lastOpenBus = 0;

    // WAITCNT (REG 0x04000204): controls cartridge access waitstates and the
    // GamePak prefetch buffer. Pokémon Emerald writes 0x4014 very early in
    // boot (ws_n0=3, ws_s0=1, prefetch on), which speeds up ROM accesses by
    // ~3x compared to the BIOS reset value of 0. We model the waitstates so
    // CPU instruction timing matches real hardware (mGBA semantics: each
    // sequential code fetch costs 1 + activeSeqCycles); without this every
    // ROM Thumb instruction was undercosted by 1-2 master cycles, making the
    // emulator run code ~4x faster than real GBA in some places and 4x slower
    // in others — this exact mismatch is what forced the load-bearing "* 4"
    // hack in runFrame and it broke Pokémon Emerald's boot the moment we
    // tried to remove it.
    private int waitCnt = 0;

    // ── Waitstate tables (mGBA src/gba/memory.c) ──────────────────────────
    // Indexed by memory region (addr >>> 24) & 0xF. Stored as MASTER cycles
    // to ADD to a single bus access (so IWRAM = 0 means no extra wait).
    //   regions: 0=BIOS 1=- 2=EWRAM 3=IWRAM 4=IO 5=PRAM 6=VRAM 7=OAM
    //            8-9=ROM0 A-B=ROM1 C-D=ROM2 E-F=SRAM
    private final int[] waitSeq16    = new int[16];
    private final int[] waitNonseq16 = new int[16];
    private final int[] waitSeq32    = new int[16];
    private final int[] waitNonseq32 = new int[16];

    // mGBA's GBA_BASE_WAITSTATES* (with WAITCNT=0; the BIOS reset value).
    private static final int[] BASE_NONSEQ_16 = {0,0,2,0,0,0,0,0, 4,4,4,4,4,4,4,4};
    private static final int[] BASE_SEQ_16    = {0,0,2,0,0,0,0,0, 2,2,4,4,8,8,4,4};
    private static final int[] BASE_NONSEQ_32 = {0,0,5,0,0,1,1,0, 7,7,9,9,13,13,9,9};
    private static final int[] BASE_SEQ_32    = {0,0,5,0,0,1,1,0, 5,5,9,9,17,17,9,9};

    // mGBA's GBA_ROM_WAITSTATES (n) and GBA_ROM_WAITSTATES_SEQ (s, per region).
    private static final int[] ROM_WAITS_NONSEQ = {4, 3, 2, 8};
    private static final int[] ROM_WAITS_SEQ_0  = {2, 1};
    private static final int[] ROM_WAITS_SEQ_1  = {4, 1};
    private static final int[] ROM_WAITS_SEQ_2  = {8, 1};

    // Optional Flash save chip (Pokémon RSE/FRLG). When set, the 0x0E region
    // is served by the Flash command protocol instead of plain SRAM.
    private FlashMemory flash = null;

    // Cartridge GPIO + RTC (Pokémon RSE). Mapped at 0x080000C4-0x080000C8 inside
    // the ROM region. Emerald polls this during boot; without it the game stalls.
    private com.gbaminecraft.emulator.cartridge.GpioRtc gpio = null;
    public void setGpioRtc(com.gbaminecraft.emulator.cartridge.GpioRtc g) { this.gpio = g; }
    public com.gbaminecraft.emulator.cartridge.GpioRtc getGpioRtc() { return gpio; }

    public void setFlash(FlashMemory f) { this.flash = f; }
    public FlashMemory getFlash()       { return flash; }

    // Optional EEPROM save chip. Accessed serially through the 0x0D region.
    private Eeprom eeprom = null;
    public void setEeprom(Eeprom e) { this.eeprom = e; }
    public Eeprom getEeprom()       { return eeprom; }

    // Serial I/O (link cable) controller. Owned by the bus because it needs to
    // request interrupts and mirror SIOCNT back into io[]. Drives transfer
    // completion so games that init the link cable on boot don't hang.
    private final com.gbaminecraft.emulator.serial.SerialController serial =
            new com.gbaminecraft.emulator.serial.SerialController(this);
    public com.gbaminecraft.emulator.serial.SerialController getSerial() { return serial; }
    /** Advance the serial clock; call once per cycle batch from the main loop. */
    public void tickSerial(int cpuCycles) { serial.tick(cpuCycles); }

    public MemoryBus() {
        initBIOS();
        initWaitstates();
    }

    public void connectSubsystems(PPU ppu, APU apu, GBAInput input,
                                   TimerController timers, DMAController dma, ARM7TDMI cpu) {
        this.ppu    = ppu;
        this.apu    = apu;
        this.input  = input;
        this.timers = timers;
        this.dma    = dma;
        this.cpu    = cpu;
    }

    public void loadROM(byte[] romData) {
        this.rom = romData;
    }

    public void loadBIOS(byte[] biosData) {
        System.arraycopy(biosData, 0, bios, 0, Math.min(biosData.length, bios.length));
    }

    // ── 32-bit read ────────────────────────────────────────────────────────
    public int read32(int addr) {
        addr &= ~3;
        int page = (addr >>> 24) & 0xFF;
        switch (page) {
            case 0x00: return readBIOS32(addr);
            case 0x02: return readBytes32(ewram, addr & 0x3FFFF);
            case 0x03: return readBytes32(iwram, addr & 0x7FFF);
            case 0x04: return readIO32(addr & 0x3FF);
            case 0x05: return readBytes32(palette, addr & 0x3FF);
            case 0x06: return readBytes32(vram, vramOffset(addr));
            case 0x07: return readBytes32(oam, addr & 0x3FF);
            case 0x08: case 0x09:
            case 0x0A: case 0x0B:
            case 0x0C: case 0x0D: {
                if (gpio != null && com.gbaminecraft.emulator.cartridge.GpioRtc.isGpioAddr(addr)) {
                    int lo = gpio.read16(addr);
                    int hi = gpio.read16(addr + 2);
                    if (lo >= 0) return (lo & 0xFFFF) | ((hi >= 0 ? hi : 0) << 16);
                }
                return readROM32(addr);
            }
            case 0x0E: case 0x0F:
                if (flash != null) { int b = flash.read(addr); return b | (b<<8) | (b<<16) | (b<<24); }
                return readBytes32(sram, addr & 0xFFFF);
            default:   return lastOpenBus;
        }
    }

    // ── 16-bit read ────────────────────────────────────────────────────────
    public int read16(int addr) {
        addr &= ~1;
        int page = (addr >>> 24) & 0xFF;
        switch (page) {
            case 0x00: return readBIOS16(addr);
            case 0x02: return readBytes16(ewram, addr & 0x3FFFF);
            case 0x03: return readBytes16(iwram, addr & 0x7FFF);
            case 0x04: return readIO16(addr & 0x3FF);
            case 0x05: return readBytes16(palette, addr & 0x3FF);
            case 0x06: return readBytes16(vram, vramOffset(addr));
            case 0x07: return readBytes16(oam, addr & 0x3FF);
            case 0x08: case 0x09:
            case 0x0A: case 0x0B:
            case 0x0C:
                if (gpio != null && com.gbaminecraft.emulator.cartridge.GpioRtc.isGpioAddr(addr)) {
                    int g = gpio.read16(addr);
                    if (g >= 0) return g;
                }
                return readROM16(addr);
            case 0x0D:
                if (eeprom != null) return eeprom.readBit() & 1;
                return readROM16(addr);
            case 0x0E: case 0x0F:
                if (flash != null) { int b = flash.read(addr); return b | (b<<8); }
                return readBytes16(sram, addr & 0xFFFF);
            default:   return lastOpenBus & 0xFFFF;
        }
    }

    // ── 8-bit read ─────────────────────────────────────────────────────────
    public int read8(int addr) {
        int page = (addr >>> 24) & 0xFF;
        switch (page) {
            case 0x00: return bios[addr & 0x3FFF] & 0xFF;
            case 0x02: return ewram[addr & 0x3FFFF] & 0xFF;
            case 0x03: return iwram[addr & 0x7FFF] & 0xFF;
            case 0x04: return readIO8(addr & 0x3FF);
            case 0x05: return palette[addr & 0x3FF] & 0xFF;
            case 0x06: return vram[vramOffset(addr)] & 0xFF;
            case 0x07: return oam[addr & 0x3FF] & 0xFF;
            case 0x08: case 0x09:
            case 0x0A: case 0x0B:
            case 0x0C: case 0x0D: {
                int off = addr - 0x08000000;
                return (off < rom.length) ? (rom[off] & 0xFF) : 0xFF;
            }
            case 0x0E: case 0x0F:
                if (flash != null) return flash.read(addr);
                return sram[addr & 0xFFFF] & 0xFF;
            default:   return lastOpenBus & 0xFF;
        }
    }

    // ── 32-bit write ───────────────────────────────────────────────────────
    public void write32(int addr, int val) {
        addr &= ~3;
        int page = (addr >>> 24) & 0xFF;
        if (gpio != null && com.gbaminecraft.emulator.cartridge.GpioRtc.isGpioAddr(addr)) {
            gpio.write16(addr, val & 0xFFFF);
            gpio.write16(addr + 2, (val >>> 16) & 0xFFFF);
            return;
        }
        switch (page) {
            case 0x02: writeBytes32(ewram,   addr & 0x3FFFF, val); break;
            case 0x03: writeBytes32(iwram,   addr & 0x7FFF,  val); break;
            case 0x04: writeIO32(addr & 0x3FF, val); break;
            case 0x05: writeBytes32(palette, addr & 0x3FF,   val); break;
            case 0x06: writeBytes32(vram, vramOffset(addr), val); break;
            case 0x07: writeBytes32(oam,     addr & 0x3FF,   val); break;
            case 0x0E: case 0x0F:
                if (flash != null) flash.write(addr, val & 0xFF);
                else writeBytes32(sram, addr & 0xFFFF, val);
                break;
        }
    }

    // ── 16-bit write ───────────────────────────────────────────────────────
    public void write16(int addr, short val) {
        addr &= ~1;
        int page = (addr >>> 24) & 0xFF;
        if (gpio != null && com.gbaminecraft.emulator.cartridge.GpioRtc.isGpioAddr(addr)) {
            gpio.write16(addr, val & 0xFFFF);
            return;
        }
        switch (page) {
            case 0x02: writeBytes16(ewram,   addr & 0x3FFFF, val); break;
            case 0x03: writeBytes16(iwram,   addr & 0x7FFF,  val); break;
            case 0x04: writeIO16(addr & 0x3FF, val & 0xFFFF); break;
            case 0x05: writeBytes16(palette, addr & 0x3FF,   val); break;
            case 0x06: writeBytes16(vram, vramOffset(addr), val); break;
            case 0x07: writeBytes16(oam,     addr & 0x3FF,   val); break;
            case 0x0D:
                if (eeprom != null) eeprom.writeBit(val & 1);
                break;
            case 0x0E: case 0x0F:
                if (flash != null) flash.write(addr, val & 0xFF);
                else writeBytes16(sram, addr & 0xFFFF, val);
                break;
        }
    }

    // ── 8-bit write ────────────────────────────────────────────────────────
    public void write8(int addr, byte val) {
        int page = (addr >>> 24) & 0xFF;
        switch (page) {
            case 0x02: ewram[addr & 0x3FFFF]  = val; break;
            case 0x03: iwram[addr & 0x7FFF]   = val; break;
            case 0x04: writeIO8(addr & 0x3FF, val & 0xFF); break;
            case 0x05: // Palette byte writes are replicated to both bytes
                palette[addr & 0x3FE]     = val;
                palette[(addr & 0x3FE) + 1] = val;
                break;
            case 0x06: { // VRAM byte writes replicate to both bytes of the halfword
                int vo = vramOffset(addr) & ~1;
                vram[vo]     = val;
                vram[vo + 1] = val;
                break;
            }
            case 0x07: break; // OAM ignores byte writes
            case 0x0E: case 0x0F:
                if (flash != null) flash.write(addr, val & 0xFF);
                else sram[addr & 0xFFFF] = val;
                break;
        }
    }

    // ── VRAM address mapping ─────────────────────────────────────────────────
    // VRAM is 96 KB. The 0x06000000 region mirrors every 128 KB, and inside each
    // 128 KB block the upper 32 KB (0x18000-0x1FFFF) mirror the 0x10000-0x17FFF
    // range. A naive (addr & 0x17FFF) wraps 0x18000 to 0 and corrupts tile/map
    // placement (Pokémon's menu tiles landed at the wrong base, leaving an empty
    // charBase and a blank screen). This maps it correctly.
    private static int vramOffset(int addr) {
        int a = addr & 0x1FFFF;          // 128 KB mirror
        if (a >= 0x18000) a -= 0x8000;   // upper 32 KB mirror the previous 32 KB
        return a;
    }

    // ── ROM read helpers ───────────────────────────────────────────────────
    private int readROM32(int addr) {
        int off = addr - 0x08000000;
        if (off + 3 >= rom.length) return 0;
        return (rom[off] & 0xFF) | ((rom[off+1] & 0xFF) << 8)
             | ((rom[off+2] & 0xFF) << 16) | ((rom[off+3] & 0xFF) << 24);
    }

    private int readROM16(int addr) {
        int off = addr - 0x08000000;
        if (off + 1 >= rom.length) return 0;
        return (rom[off] & 0xFF) | ((rom[off+1] & 0xFF) << 8);
    }

    // ── BIOS read ──────────────────────────────────────────────────────────
    private int readBIOS32(int addr) {
        int off = addr & 0x3FFF;
        return readBytes32(bios, off);
    }

    private int readBIOS16(int addr) {
        int off = addr & 0x3FFF;
        return readBytes16(bios, off);
    }

    // ── I/O read ───────────────────────────────────────────────────────────
    private int readIO8(int offset) {
        if (ppu   != null && offset <= 0x56)  return ppu.readRegister(offset) & 0xFF;
        if (apu   != null && offset >= 0x60  && offset <= 0xA8)  return apu.readRegister(offset) & 0xFF;
        if (timers!= null && offset >= 0x100 && offset <= 0x10F) return timers.readRegister(offset) & 0xFF;
        if (dma   != null && offset >= 0x0B0 && offset <= 0x0DF) return dma.readRegister(offset) & 0xFF;
        if (input != null && (offset == 0x130 || offset == 0x131)) return input.readRegister(offset) & 0xFF;
        // Serial I/O (SIO): SIODATA/SIOCNT (0x120-0x12B) and RCNT (0x134-0x135)
        if ((offset >= 0x120 && offset <= 0x12B) || offset == 0x134 || offset == 0x135)
            return serial.readRegister(offset) & 0xFF;
        // IE, IF, WAITCNT, IME
        if (offset == 0x200) return io[0x200] & 0xFF;
        if (offset == 0x201) return io[0x201] & 0xFF;
        if (offset == 0x202) return io[0x202] & 0xFF;
        if (offset == 0x203) return io[0x203] & 0xFF;
        if (offset == 0x204) return waitCnt & 0xFF;
        if (offset == 0x205) return (waitCnt >>> 8) & 0xFF;
        if (offset == 0x208) return io[0x208] & 0xFF;
        if (offset >= 0 && offset < io.length) return io[offset] & 0xFF;
        return 0;
    }

    private int readIO16(int offset) {
        return readIO8(offset) | (readIO8(offset + 1) << 8);
    }

    private int readIO32(int offset) {
        return readIO16(offset) | (readIO16(offset + 2) << 16);
    }

    // ── I/O write ──────────────────────────────────────────────────────────
    private void writeIO8(int offset, int val) {
        val &= 0xFF;
        if (ppu   != null && offset <= 0x56)  { ppu.writeRegister(offset, val); return; }
        if (apu   != null && offset >= 0x60  && offset <= 0xA8)  { apu.writeRegister(offset, val); return; }
        if (timers!= null && offset >= 0x100 && offset <= 0x10F) { timers.writeRegister(offset, val); return; }
        if (dma   != null && offset >= 0x0B0 && offset <= 0x0DF) { dma.writeRegister(offset, val); return; }

        // Serial I/O (SIO): SIODATA/SIOCNT (0x120-0x12B) and RCNT (0x134-0x135).
        // Writing SIOCNT with the start bit kicks off a transfer that completes
        // on its own (no link partner), clearing start and raising the IRQ.
        if ((offset >= 0x120 && offset <= 0x12B) || offset == 0x134 || offset == 0x135) {
            serial.writeRegister(offset, val);
            if (offset >= 0 && offset < io.length) io[offset] = (byte)val; // read-back mirror
            return;
        }

        // Interrupt enable (IE)
        if (offset == 0x200) { io[0x200] = (byte)val; return; }
        if (offset == 0x201) { io[0x201] = (byte)val; return; }
        // Interrupt flag (IF) — writing 1 clears bit
        if (offset == 0x202) { io[0x202] &= (byte)~val; return; }
        if (offset == 0x203) { io[0x203] &= (byte)~val; return; }
        // WAITCNT — controls cart waitstates and the GamePak prefetch buffer.
        // Re-derive the per-region waitstate tables (used by the CPU's per-
        // instruction prefetch cycle accounting) on every change so games like
        // Pokémon Emerald that write 0x4014 early in boot actually start
        // running their ROM Thumb code at the optimised 1S/3N timing.
        if (offset == 0x204) { waitCnt = (waitCnt & 0xFF00) |  val;        applyWaitcnt(); return; }
        if (offset == 0x205) { waitCnt = (waitCnt & 0x00FF) | (val << 8);  applyWaitcnt(); return; }
        // IME
        if (offset == 0x208) { io[0x208] = (byte)val; return; }
        // HALTCNT
        if (offset == 0x301) {
            if ((val & 0x80) == 0) { if (cpu != null) cpu.halted = true; }
            else { if (cpu != null) cpu.stopped = true; }
            return;
        }
        if (offset >= 0 && offset < io.length) io[offset] = (byte)val;
    }

    private void writeIO16(int offset, int val) {
        writeIO8(offset, val & 0xFF);
        writeIO8(offset + 1, (val >>> 8) & 0xFF);
    }

    private void writeIO32(int offset, int val) {
        writeIO16(offset, val & 0xFFFF);
        writeIO16(offset + 2, (val >>> 16) & 0xFFFF);
    }

    // ── Interrupt helpers ──────────────────────────────────────────────────
    public void requestInterrupt(int irqBit) {
        io[0x202] |= (byte)(irqBit & 0xFF);
        io[0x203] |= (byte)((irqBit >>> 8) & 0xFF);
    }

    public boolean isIRQPending() {
        int ie  = ((io[0x201] & 0xFF) << 8) | (io[0x200] & 0xFF);
        int ifl = ((io[0x203] & 0xFF) << 8) | (io[0x202] & 0xFF);
        int ime = io[0x208] & 0xFF;
        return ime != 0 && (ie & ifl) != 0;
    }

    // ── Waitstate accessors (used by ARM7TDMI for prefetch cycle accounting) ─
    /** Sequential 16-bit access waitstate cycles for this address's region. */
    public int seqCycles16(int addr)    { return waitSeq16[(addr >>> 24) & 0xF]; }
    /** Sequential 32-bit access waitstate cycles for this address's region. */
    public int seqCycles32(int addr)    { return waitSeq32[(addr >>> 24) & 0xF]; }
    /** Non-sequential 16-bit access waitstate cycles (for branch/load turn-around). */
    public int nonseqCycles16(int addr) { return waitNonseq16[(addr >>> 24) & 0xF]; }
    /** Non-sequential 32-bit access waitstate cycles. */
    public int nonseqCycles32(int addr) { return waitNonseq32[(addr >>> 24) & 0xF]; }
    /** Same as {@link #seqCycles16(int)} but takes the pre-extracted region 0..15. */
    public int seqCycles16Region(int region) { return waitSeq16[region & 0xF]; }
    public int seqCycles32Region(int region) { return waitSeq32[region & 0xF]; }

    /** Initialise the waitstate tables to their BIOS-reset values
     *  (== mGBA's GBA_BASE_WAITSTATES with WAITCNT = 0). */
    private void initWaitstates() {
        System.arraycopy(BASE_SEQ_16,    0, waitSeq16,    0, 16);
        System.arraycopy(BASE_NONSEQ_16, 0, waitNonseq16, 0, 16);
        System.arraycopy(BASE_SEQ_32,    0, waitSeq32,    0, 16);
        System.arraycopy(BASE_NONSEQ_32, 0, waitNonseq32, 0, 16);
    }

    /**
     * Re-derive the cartridge / SRAM waitstate tables from the WAITCNT register.
     * Faithful Java port of mGBA's GBAAdjustWaitstates (src/gba/memory.c). Called
     * each time the game writes to REG_WAITCNT (0x04000204/5).
     *
     * The 32-bit cart values are computed from the 16-bit ones: a 32-bit access
     * on the 16-bit cart bus = one nonseq + one seq halfword (n + 1 + s), and a
     * 32-bit sequential access = two seq halfwords (2*s + 1 with the +1 internal).
     */
    private void applyWaitcnt() {
        int sram  = waitCnt & 0x3;
        int ws0   = (waitCnt >>> 2) & 0x3;
        int ws0s  = (waitCnt >>> 4) & 0x1;
        int ws1   = (waitCnt >>> 5) & 0x3;
        int ws1s  = (waitCnt >>> 7) & 0x1;
        int ws2   = (waitCnt >>> 8) & 0x3;
        int ws2s  = (waitCnt >>> 10) & 0x1;

        int sn   = ROM_WAITS_NONSEQ[sram];
        int n0   = ROM_WAITS_NONSEQ[ws0];
        int s0   = ROM_WAITS_SEQ_0[ws0s];
        int n1   = ROM_WAITS_NONSEQ[ws1];
        int s1   = ROM_WAITS_SEQ_1[ws1s];
        int n2   = ROM_WAITS_NONSEQ[ws2];
        int s2   = ROM_WAITS_SEQ_2[ws2s];

        // ROM 0  -> regions 0x8 / 0x9
        waitNonseq16[0x8] = waitNonseq16[0x9] = n0;
        waitSeq16   [0x8] = waitSeq16   [0x9] = s0;
        waitNonseq32[0x8] = waitNonseq32[0x9] = n0 + 1 + s0;
        waitSeq32   [0x8] = waitSeq32   [0x9] = 2 * s0 + 1;
        // ROM 1  -> regions 0xA / 0xB
        waitNonseq16[0xA] = waitNonseq16[0xB] = n1;
        waitSeq16   [0xA] = waitSeq16   [0xB] = s1;
        waitNonseq32[0xA] = waitNonseq32[0xB] = n1 + 1 + s1;
        waitSeq32   [0xA] = waitSeq32   [0xB] = 2 * s1 + 1;
        // ROM 2  -> regions 0xC / 0xD
        waitNonseq16[0xC] = waitNonseq16[0xD] = n2;
        waitSeq16   [0xC] = waitSeq16   [0xD] = s2;
        waitNonseq32[0xC] = waitNonseq32[0xD] = n2 + 1 + s2;
        waitSeq32   [0xC] = waitSeq32   [0xD] = 2 * s2 + 1;
        // SRAM   -> regions 0xE / 0xF (8-bit bus, same cost both directions)
        waitNonseq16[0xE] = waitNonseq16[0xF] = sn;
        waitSeq16   [0xE] = waitSeq16   [0xF] = sn;
        waitNonseq32[0xE] = waitNonseq32[0xF] = 2 * sn + 1;
        waitSeq32   [0xE] = waitSeq32   [0xF] = 2 * sn + 1;
    }

    // ── Byte array helpers ─────────────────────────────────────────────────
    private int readBytes32(byte[] mem, int off) {
        if (off + 3 >= mem.length) return 0;
        return (mem[off] & 0xFF) | ((mem[off+1] & 0xFF) << 8)
             | ((mem[off+2] & 0xFF) << 16) | ((mem[off+3] & 0xFF) << 24);
    }

    private int readBytes16(byte[] mem, int off) {
        if (off + 1 >= mem.length) return 0;
        return (mem[off] & 0xFF) | ((mem[off+1] & 0xFF) << 8);
    }

    private void writeBytes32(byte[] mem, int off, int val) {
        if (off + 3 >= mem.length) return;
        mem[off]   = (byte)val;
        mem[off+1] = (byte)(val >>> 8);
        mem[off+2] = (byte)(val >>> 16);
        mem[off+3] = (byte)(val >>> 24);
    }

    private void writeBytes16(byte[] mem, int off, int val) {
        if (off + 1 >= mem.length) return;
        mem[off]   = (byte)(val & 0xFF);
        mem[off+1] = (byte)((val >>> 8) & 0xFF);
    }

    // ── BIOS stub ──────────────────────────────────────────────────────────
    private void initBIOS() {
        // With HLE enabled, IRQ entry and SWIs are emulated in Java (see CPU),
        // so the BIOS area only needs harmless filler. We keep an "infinite
        // loop" word at the vectors so stray jumps don't execute garbage.
        for (int v = 0x00; v <= 0x1C; v += 4) {
            writeBytes32(bios, v, 0xEAFFFFFE); // b . (self)
        }
    }

    // ── Direct memory accessors (for DMA, PPU) ────────────────────────────
    public byte[] getVRAM()    { return vram; }
    public byte[] getPalette() { return palette; }
    public byte[] getOAM()     { return oam; }
    public byte[] getIWRAM()   { return iwram; }
    public byte[] getIO()      { return io; }
    public byte[] getEWRAM()   { return ewram; }
    public byte[] getSRAM()    { return sram; }
    public byte[] getROM()     { return rom; }

    public void reset() {
        java.util.Arrays.fill(ewram, (byte)0);
        java.util.Arrays.fill(iwram, (byte)0);
        java.util.Arrays.fill(palette, (byte)0);
        java.util.Arrays.fill(vram, (byte)0);
        java.util.Arrays.fill(oam, (byte)0);
        java.util.Arrays.fill(io, (byte)0);
        java.util.Arrays.fill(sram, (byte)0);
        // BIOS soft-reset clears WAITCNT to 0; restore the default waitstate
        // tables so a fresh boot starts from the same state as power-on.
        waitCnt = 0;
        initWaitstates();
        serial.reset();
        initBIOS();
    }
}
