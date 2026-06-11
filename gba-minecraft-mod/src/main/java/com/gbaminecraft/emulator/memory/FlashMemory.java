package com.gbaminecraft.emulator.memory;

/**
 * GBA Flash memory (the save chip used by Pokémon Ruby/Sapphire/Emerald/FRLG).
 *
 * Flash is accessed through the cart-RAM window at 0x0E000000 using a command
 * protocol written to 0x0E005555 / 0x0E002AAA. We model:
 *   - ID mode (returns manufacturer + device id; games probe this at boot)
 *   - Erase (chip + 4KB sector)
 *   - Single-byte program
 *   - Bank switching for 128KB parts (two 64KB banks)
 *
 * Default IDs: Macronix 64KB (0x1CC2) and Sanyo/Macronix 128KB (0x1362 / 0x09C2).
 * We use Sanyo 128KB id (0x1362) for FLASH_1M and Panasonic 64KB (0x1B32)
 * for FLASH_512K — values commonly accepted by games.
 */
public final class FlashMemory {

    public enum Size { K512, M1 } // 64KB, 128KB

    private final byte[] data;
    private final int bankSize = 0x10000;        // 64KB per bank
    private final int manufacturerId;
    private final int deviceId;

    private int  cmdPhase = 0;       // command sequence state
    private boolean idMode = false;
    private boolean eraseArmed = false;
    private boolean writeArmed = false;
    private boolean bankArmed  = false;
    private int  bank = 0;

    public FlashMemory(Size size) {
        if (size == Size.M1) {
            data = new byte[128 * 1024];
            manufacturerId = 0x62;   // Sanyo
            deviceId       = 0x13;
        } else {
            data = new byte[64 * 1024];
            manufacturerId = 0x32;   // Panasonic
            deviceId       = 0x1B;
        }
        java.util.Arrays.fill(data, (byte) 0xFF);
    }

    public byte[] getData() { return data; }

    public void loadData(byte[] src) {
        System.arraycopy(src, 0, data, 0, Math.min(src.length, data.length));
    }

    // ── Read ─────────────────────────────────────────────────────────────
    public int read(int addr) {
        int off = addr & 0xFFFF;
        if (idMode) {
            // Manufacturer at 0x0000, device at 0x0001
            if (off == 0x0000) return manufacturerId & 0xFF;
            if (off == 0x0001) return deviceId & 0xFF;
        }
        int idx = bank * bankSize + off;
        if (idx >= data.length) return 0xFF;
        return data[idx] & 0xFF;
    }

    // ── Write (command protocol) ─────────────────────────────────────────
    public void write(int addr, int value) {
        int off = addr & 0xFFFF;
        value &= 0xFF;

        // A pending single-byte program?
        if (writeArmed) {
            int idx = bank * bankSize + off;
            if (idx < data.length) data[idx] = (byte) value;
            writeArmed = false;
            cmdPhase = 0;
            return;
        }
        // A pending bank switch (only the 0x0000 address matters)?
        if (bankArmed) {
            bank = value & 1;
            bankArmed = false;
            cmdPhase = 0;
            return;
        }

        // Command unlock sequence: 0x5555=0xAA, 0x2AAA=0x55, then 0x5555=cmd
        if (off == 0x5555 && value == 0xAA && cmdPhase == 0) { cmdPhase = 1; return; }
        if (off == 0x2AAA && value == 0x55 && cmdPhase == 1) { cmdPhase = 2; return; }

        if (cmdPhase == 2) {
            switch (value) {
                case 0x90: idMode = true;  cmdPhase = 0; return;   // enter ID mode
                case 0xF0: idMode = false; cmdPhase = 0; return;   // exit ID mode
                case 0x80: eraseArmed = true; cmdPhase = 0; return;// erase prelude
                case 0xA0: writeArmed = true; cmdPhase = 0; return;// program single byte
                case 0xB0: bankArmed  = true; cmdPhase = 0; return;// bank switch (128KB)
                case 0x10:                                          // chip erase
                    if (eraseArmed) { java.util.Arrays.fill(data, (byte)0xFF); eraseArmed = false; }
                    cmdPhase = 0; return;
                default:
                    // Could be a sector erase: cmd 0x30 with the sector address
                    if (value == 0x30 && eraseArmed) {
                        int sector = (bank * bankSize) + (off & 0xF000);
                        for (int i = 0; i < 0x1000 && sector + i < data.length; i++) {
                            data[sector + i] = (byte) 0xFF;
                        }
                        eraseArmed = false;
                    }
                    cmdPhase = 0;
                    return;
            }
        }
        // Sector erase command can also arrive after the 0x80 prelude with its own
        // unlock; handle the common 0x30 case at any unlocked phase.
        if (value == 0x30 && eraseArmed) {
            int sector = (bank * bankSize) + (off & 0xF000);
            for (int i = 0; i < 0x1000 && sector + i < data.length; i++) data[sector + i] = (byte) 0xFF;
            eraseArmed = false;
            cmdPhase = 0;
        }
    }

    public void reset() {
        cmdPhase = 0; idMode = false; eraseArmed = false;
        writeArmed = false; bankArmed = false; bank = 0;
    }
}
