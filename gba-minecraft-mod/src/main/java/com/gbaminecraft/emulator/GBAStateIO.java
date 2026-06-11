package com.gbaminecraft.emulator;

import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;

import java.io.*;

/**
 * Save-state (snapshot) and battery-save serialization for the GBA emulator.
 *
 * NOTE (honest scope): this v1 snapshot captures the main mutable RAM regions
 * (EWRAM, IWRAM, Palette, VRAM, OAM, SRAM, I/O block) plus the visible CPU
 * register file (R0-R15, CPSR, SPSR, cycle counter, halt/stop flags).
 *
 * It does NOT yet capture internal PPU/APU/Timer/DMA latches or the CPU's
 * banked registers, so a restored state may glitch in edge cases until the
 * core exposes full subsystem serialization. It is a working foundation, not
 * cycle-perfect persistence.
 */
public final class GBAStateIO {

    private static final int MAGIC   = 0xFB0A5747; // "FBA"+state marker
    private static final int VERSION = 1;

    private GBAStateIO() {}

    // ── Full save-state ──────────────────────────────────────────────────
    public static void saveState(GBAEmulator emu, OutputStream out) throws IOException {
        MemoryBus bus = emu.getBus();
        ARM7TDMI  cpu = emu.getCPU();
        DataOutputStream o = new DataOutputStream(new BufferedOutputStream(out));

        o.writeInt(MAGIC);
        o.writeInt(VERSION);

        writeBlock(o, bus.getEWRAM());
        writeBlock(o, bus.getIWRAM());
        writeBlock(o, bus.getPalette());
        writeBlock(o, bus.getVRAM());
        writeBlock(o, bus.getOAM());
        writeBlock(o, bus.getSRAM());
        writeBlock(o, bus.getIO());

        for (int i = 0; i < 16; i++) o.writeInt(cpu.regs[i]);
        o.writeInt(cpu.cpsr);
        o.writeInt(cpu.spsr);
        o.writeLong(cpu.cycles);
        o.writeBoolean(cpu.halted);
        o.writeBoolean(cpu.stopped);

        // Active save chip (Flash/EEPROM) so a save-state is self-contained.
        writeBlock(o, activeSaveData(emu));

        o.flush();
    }

    public static boolean loadState(GBAEmulator emu, InputStream in) throws IOException {
        MemoryBus bus = emu.getBus();
        ARM7TDMI  cpu = emu.getCPU();
        DataInputStream i = new DataInputStream(new BufferedInputStream(in));

        if (i.readInt() != MAGIC)   return false;
        if (i.readInt() != VERSION) return false;

        readBlockInto(i, bus.getEWRAM());
        readBlockInto(i, bus.getIWRAM());
        readBlockInto(i, bus.getPalette());
        readBlockInto(i, bus.getVRAM());
        readBlockInto(i, bus.getOAM());
        readBlockInto(i, bus.getSRAM());
        readBlockInto(i, bus.getIO());

        for (int r = 0; r < 16; r++) cpu.regs[r] = i.readInt();
        cpu.cpsr    = i.readInt();
        cpu.spsr    = i.readInt();
        cpu.cycles  = i.readLong();
        cpu.halted  = i.readBoolean();
        cpu.stopped = i.readBoolean();

        // Active save chip data (Flash/EEPROM). May be absent in older states.
        if (i.available() > 0) {
            readBlockInto(i, activeSaveData(emu));
        }
        return true;
    }

    // ── Battery save (SRAM / Flash / EEPROM) ─────────────────────────────
    /** Persist whichever save backing the cartridge actually uses. */
    public static void saveBattery(GBAEmulator emu, OutputStream out) throws IOException {
        out.write(activeSaveData(emu));
        out.flush();
    }

    public static void loadBattery(GBAEmulator emu, InputStream in) throws IOException {
        byte[] dest = activeSaveData(emu);
        byte[] data = in.readAllBytes();
        System.arraycopy(data, 0, dest, 0, Math.min(data.length, dest.length));
    }

    /** Returns the live byte array of the active save chip (Flash > EEPROM > SRAM). */
    private static byte[] activeSaveData(GBAEmulator emu) {
        var bus = emu.getBus();
        if (bus.getFlash()  != null) return bus.getFlash().getData();
        if (bus.getEeprom() != null) return bus.getEeprom().getData();
        return bus.getSRAM();
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private static void writeBlock(DataOutputStream o, byte[] data) throws IOException {
        o.writeInt(data.length);
        o.write(data);
    }

    private static void readBlockInto(DataInputStream i, byte[] dest) throws IOException {
        int len = i.readInt();
        byte[] tmp = new byte[len];
        i.readFully(tmp);
        System.arraycopy(tmp, 0, dest, 0, Math.min(len, dest.length));
    }
}
