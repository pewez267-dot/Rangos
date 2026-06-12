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

    // WAITCNT
    private int waitCnt = 0;

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
        // WAITCNT
        if (offset == 0x204) { waitCnt = (waitCnt & 0xFF00) | val; return; }
        if (offset == 0x205) { waitCnt = (waitCnt & 0x00FF) | (val << 8); return; }
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
        serial.reset();
        initBIOS();
    }
}
