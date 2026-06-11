package com.gbaminecraft.emulator.memory;

/**
 * GBA EEPROM save chip (512 bytes or 8 KB). Used by many cartridges as their
 * save backing (e.g. several first-party titles). Accessed through the cart
 * bus via a serial bit protocol (usually driven by DMA) at the top of the ROM
 * region (0x0Dxxxxxx).
 *
 * Protocol (per Nintendo):
 *   Write:  "10" + address bits + 64 data bits + "0"
 *   Read:   "11" + address bits + "0", then 4 ignored bits + 64 data bits
 * Address width is 6 bits for 512B (4Kbit) and 14 bits for 8KB (64Kbit).
 */
public final class Eeprom {

    public enum Size { K4, K64 } // 512 bytes, 8 KB

    private final byte[] data;
    private final int addrBits;

    // Serial state machine
    private enum State { IDLE, READ_ADDR, READ_DATA, WRITE_ADDR, WRITE_DATA, WRITE_END }
    private State state = State.IDLE;

    private long shiftIn = 0;     // bits clocked in from the CPU/DMA
    private int  bitsIn = 0;
    private int  address = 0;
    private long readBuffer = 0;  // 64 data bits + lead bits to clock out
    private int  readBitPos = 0;
    private int  cmdBits = 0;     // first two command bits

    public Eeprom(Size size) {
        if (size == Size.K64) { data = new byte[8192];  addrBits = 14; }
        else                  { data = new byte[512];   addrBits = 6;  }
        java.util.Arrays.fill(data, (byte) 0xFF);
    }

    public byte[] getData() { return data; }
    public void loadData(byte[] src) { System.arraycopy(src, 0, data, 0, Math.min(src.length, data.length)); }

    /** Read a single serial bit (games DMA-read halfwords; bit0 carries data). */
    public int readBit() {
        if (state == State.READ_DATA) {
            // First 4 clocked reads are ignored (return 0), then 64 data bits MSB-first.
            if (readBitPos < 4) { readBitPos++; return 0; }
            int dataIdx = readBitPos - 4;
            int bit = (int)((readBuffer >> (63 - dataIdx)) & 1);
            readBitPos++;
            if (readBitPos >= 4 + 64) { state = State.IDLE; }
            return bit;
        }
        return 1; // ready/idle high
    }

    /** Write a single serial bit (bit0 of the halfword the game writes). */
    public void writeBit(int bit) {
        bit &= 1;
        switch (state) {
            case IDLE:
                shiftIn = (shiftIn << 1) | bit;
                bitsIn++;
                if (bitsIn == 2) {
                    cmdBits = (int) shiftIn;
                    shiftIn = 0; bitsIn = 0;
                    if (cmdBits == 0b11)      state = State.READ_ADDR;
                    else if (cmdBits == 0b10) state = State.WRITE_ADDR;
                }
                break;
            case READ_ADDR:
                shiftIn = (shiftIn << 1) | bit; bitsIn++;
                if (bitsIn == addrBits) {
                    address = (int) shiftIn & ((1 << addrBits) - 1);
                    prepareRead();
                    shiftIn = 0; bitsIn = 0;
                    state = State.READ_DATA; readBitPos = 0;
                }
                break;
            case WRITE_ADDR:
                shiftIn = (shiftIn << 1) | bit; bitsIn++;
                if (bitsIn == addrBits) {
                    address = (int) shiftIn & ((1 << addrBits) - 1);
                    shiftIn = 0; bitsIn = 0;
                    state = State.WRITE_DATA;
                }
                break;
            case WRITE_DATA:
                shiftIn = (shiftIn << 1) | bit; bitsIn++;
                if (bitsIn == 64) {
                    commitWrite();
                    shiftIn = 0; bitsIn = 0;
                    state = State.WRITE_END;
                }
                break;
            case WRITE_END:
                state = State.IDLE; // trailing 0 bit
                break;
            default:
                break;
        }
    }

    private void prepareRead() {
        int base = address * 8;
        readBuffer = 0;
        for (int i = 0; i < 8; i++) {
            int idx = base + i;
            long b = (idx < data.length) ? (data[idx] & 0xFFL) : 0xFFL;
            readBuffer = (readBuffer << 8) | b;
        }
    }

    private void commitWrite() {
        int base = address * 8;
        for (int i = 0; i < 8; i++) {
            int idx = base + i;
            if (idx < data.length) {
                data[idx] = (byte)((shiftIn >> (56 - i*8)) & 0xFF);
            }
        }
    }

    public void reset() {
        state = State.IDLE; shiftIn = 0; bitsIn = 0; address = 0;
        readBuffer = 0; readBitPos = 0; cmdBits = 0;
    }
}
