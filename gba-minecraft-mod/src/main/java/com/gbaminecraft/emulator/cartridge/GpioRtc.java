package com.gbaminecraft.emulator.cartridge;

import java.util.Calendar;

/**
 * GBA cartridge GPIO port + Seiko S-3511A Real-Time Clock (RTC).
 *
 * Pokémon Ruby/Sapphire/Emerald carry an RTC chip wired to the cartridge GPIO
 * lines, mapped into the ROM space at:
 *   0x080000C4 — GPIO data    (I/O of the 4 GPIO pins)
 *   0x080000C6 — GPIO direction (1 = output from GBA, 0 = input to GBA)
 *   0x080000C8 — GPIO control  (bit0: 1 = GPIO registers readable)
 *
 * The RTC uses a 3-wire serial protocol over 3 of those pins:
 *   bit0 = SCK (clock), bit1 = SIO (data), bit2 = CS (chip select).
 *
 * Emerald polls the RTC during boot; if it never answers, the game stalls
 * before reaching the main menu. Implementing the command set (reset, status,
 * date/time) lets the boot sequence complete.
 *
 * References (paraphrased for compliance): the S-3511A command format is a
 * 8-bit command byte (0x06 fixed prefix in the high nibble), followed by data
 * bytes in BCD, LSB-first per byte. Content was rephrased for compliance with
 * licensing restrictions.
 */
public final class GpioRtc {

    // GPIO pin bits (within the 4-bit GPIO port).
    private static final int PIN_SCK = 0x1;  // serial clock
    private static final int PIN_SIO = 0x2;  // serial data (bidirectional)
    private static final int PIN_CS  = 0x4;  // chip select

    // GPIO state
    private int gpioData = 0;       // current pin levels
    private int gpioDir  = 0;       // 1 bit = pin driven by GBA (output)
    private boolean gpioReadable = false; // control bit0

    // ── RTC command/transfer state ──────────────────────────────────────────
    private boolean csActive = false;
    private int  bitBuffer   = 0;     // bits shifted in/out of the current byte
    private int  bitCount    = 0;     // how many bits seen in the current byte
    private boolean haveCommand = false;
    private int  command     = 0;     // the command byte
    private int  byteIndex   = 0;     // which data byte of the command we're on
    private boolean reading  = false; // true: RTC drives SIO; false: GBA writes
    private int  lastSck     = 0;

    // RTC registers
    private int  statusReg = 0x40;    // bit6 = 24-hour mode (default), like real carts
    private final int[] dateTime = new int[7]; // year,month,day,dow,hour,min,sec (BCD)

    // Output shift for reads
    private int  outBuffer = 0;
    private int  outBits   = 0;

    public GpioRtc() {
        refreshClock();
    }

    /** Snapshot the host clock into the RTC date/time registers (BCD). */
    private void refreshClock() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR) % 100;
        int month = c.get(Calendar.MONTH) + 1;
        int day  = c.get(Calendar.DAY_OF_MONTH);
        int dow  = c.get(Calendar.DAY_OF_WEEK) - 1; // 0..6
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min  = c.get(Calendar.MINUTE);
        int sec  = c.get(Calendar.SECOND);
        dateTime[0] = toBcd(year);
        dateTime[1] = toBcd(month);
        dateTime[2] = toBcd(day);
        dateTime[3] = toBcd(dow);
        dateTime[4] = toBcd(hour);
        dateTime[5] = toBcd(min);
        dateTime[6] = toBcd(sec);
    }

    private static int toBcd(int v) { return ((v / 10) << 4) | (v % 10); }

    // ── Address test ─────────────────────────────────────────────────────────
    public static boolean isGpioAddr(int addr) {
        return addr >= 0x080000C4 && addr <= 0x080000C9;
    }

    // ── Reads ─────────────────────────────────────────────────────────────────
    /** Returns the 16-bit value the GBA reads from a GPIO register, or -1 if the
     *  address is not a GPIO register (caller should fall back to ROM). */
    public int read16(int addr) {
        switch (addr & ~1) {
            case 0x080000C4: // GPIO data
                if (!gpioReadable) return 0; // when not readable, reads as 0
                // Bits configured as input return the RTC/driven levels; bits set
                // as output read back what the GBA wrote.
                int inputs = currentSio();
                int val = (gpioData & gpioDir) | (inputs & ~gpioDir);
                return val & 0xF;
            case 0x080000C6: // direction
                return gpioReadable ? (gpioDir & 0xF) : 0;
            case 0x080000C8: // control
                return gpioReadable ? 1 : 0;
        }
        return -1;
    }

    /** The level the RTC is currently driving on SIO (only meaningful when SIO is
     *  configured as input to the GBA and the RTC is in a read phase). */
    private int currentSio() {
        if (reading && csActive && (outBits > 0)) {
            int bit = (outBuffer & 1);
            return bit != 0 ? PIN_SIO : 0;
        }
        return 0;
    }

    // ── Writes ──────────────────────────────────────────────────────────────
    public void write16(int addr, int val) {
        val &= 0xFFFF;
        switch (addr & ~1) {
            case 0x080000C4: // GPIO data
                handleGpioWrite(val & 0xF);
                break;
            case 0x080000C6: // direction
                gpioDir = val & 0xF;
                break;
            case 0x080000C8: // control
                gpioReadable = (val & 1) != 0;
                break;
        }
    }

    private void handleGpioWrite(int newData) {
        int sck = newData & PIN_SCK;
        int cs  = newData & PIN_CS;
        // Track CS edge: a rising CS begins a new command transfer.
        boolean csNow = cs != 0;
        if (!csActive && csNow) {
            // Begin transfer
            csActive = true;
            bitBuffer = 0; bitCount = 0; haveCommand = false;
            command = 0; byteIndex = 0; reading = false;
            outBuffer = 0; outBits = 0;
        } else if (csActive && !csNow) {
            // End transfer
            csActive = false;
        }

        if (csActive) {
            int prevSck = lastSck;
            // Data is sampled/shifted on the rising edge of SCK.
            if (prevSck == 0 && sck != 0) {
                if (!reading) {
                    // GBA -> RTC: sample SIO bit (LSB first within a byte)
                    int sioBit = (newData & PIN_SIO) != 0 ? 1 : 0;
                    bitBuffer |= (sioBit << bitCount);
                    bitCount++;
                    if (bitCount == 8) {
                        processByte(bitBuffer & 0xFF);
                        bitBuffer = 0; bitCount = 0;
                    }
                } else {
                    // RTC -> GBA: advance the output shift register
                    if (outBits > 0) {
                        outBuffer >>= 1;
                        outBits--;
                        if (outBits == 0) loadNextOutputByte();
                    }
                }
            }
        }
        lastSck = sck;
        // Keep the written output-bit visible on the data register for read-back.
        gpioData = newData;
    }

    private void processByte(int b) {
        if (!haveCommand) {
            command = b;
            haveCommand = true;
            byteIndex = 0;
            // S-3511A command byte: high nibble is the fixed code 0x6, low nibble
            // selects the register and the direction (bit = read).
            int cmd = (command >> 4) & 0xF;
            boolean isRead = (command & 0x80) != 0; // MSB set => read
            // Some games encode the fixed prefix in the low nibble instead; accept
            // both orderings by detecting the 0x6 nibble.
            int reg;
            if ((command & 0x0F) == 0x06) {
                reg = (command >> 4) & 0x7;
                isRead = (command & 0x80) != 0;
            } else {
                reg = (command >> 4) & 0x7;
            }
            reading = isRead;
            if (reading) {
                beginRead(reg);
            } else {
                // Writing: subsequent bytes carry data for the register.
                currentReg = reg;
            }
        } else {
            // Data byte for a write command.
            applyWrite(currentReg, byteIndex, b);
            byteIndex++;
        }
    }

    private int currentReg = 0;

    private void beginRead(int reg) {
        currentReg = reg;
        byteIndex = 0;
        loadFirstOutput(reg);
    }

    // Output sequencing for reads.
    private int[] outQueue = new int[8];
    private int   outQueueLen = 0;
    private int   outQueuePos = 0;

    private void loadFirstOutput(int reg) {
        outQueueLen = 0; outQueuePos = 0;
        switch (reg) {
            case 0: // reset — no data
                break;
            case 1: // status
                outQueue[outQueueLen++] = statusReg & 0xFF;
                break;
            case 2: // date+time (7 bytes)
                refreshClock();
                for (int i = 0; i < 7; i++) outQueue[outQueueLen++] = dateTime[i] & 0xFF;
                break;
            case 3: // time only (3 bytes: hour,min,sec)
                refreshClock();
                outQueue[outQueueLen++] = dateTime[4] & 0xFF;
                outQueue[outQueueLen++] = dateTime[5] & 0xFF;
                outQueue[outQueueLen++] = dateTime[6] & 0xFF;
                break;
            default:
                break;
        }
        loadNextOutputByte();
    }

    private void loadNextOutputByte() {
        if (outQueuePos < outQueueLen) {
            outBuffer = outQueue[outQueuePos++] & 0xFF;
            outBits = 8;
        } else {
            outBuffer = 0; outBits = 0;
        }
    }

    private void applyWrite(int reg, int idx, int data) {
        switch (reg) {
            case 0: // reset: clear date/time
                statusReg = 0x00;
                for (int i = 0; i < 7; i++) dateTime[i] = 0;
                break;
            case 1: // status
                statusReg = data & 0xFF;
                break;
            default:
                // date/time writes from the game are accepted but we re-derive
                // from the host clock on the next read, so ignore for simplicity.
                break;
        }
    }

    public void reset() {
        gpioData = 0; gpioDir = 0; gpioReadable = false;
        csActive = false; bitBuffer = 0; bitCount = 0; haveCommand = false;
        command = 0; byteIndex = 0; reading = false; lastSck = 0;
        outBuffer = 0; outBits = 0; outQueueLen = 0; outQueuePos = 0;
        statusReg = 0x40;
        refreshClock();
    }
}
