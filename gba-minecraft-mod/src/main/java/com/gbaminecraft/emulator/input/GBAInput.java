package com.gbaminecraft.emulator.input;

/**
 * GBA input handler.
 * KEYINPUT register (0x04000130): active LOW (0 = pressed, 1 = released).
 * Bits: A=0, B=1, Select=2, Start=3, Right=4, Left=5, Up=6, Down=7, R=8, L=9
 */
public class GBAInput {

    public static final int KEY_A      = 0;
    public static final int KEY_B      = 1;
    public static final int KEY_SELECT = 2;
    public static final int KEY_START  = 3;
    public static final int KEY_RIGHT  = 4;
    public static final int KEY_LEFT   = 5;
    public static final int KEY_UP     = 6;
    public static final int KEY_DOWN   = 7;
    public static final int KEY_R      = 8;
    public static final int KEY_L      = 9;

    // All bits high = no keys pressed
    private int keyState = 0x03FF;

    // KEYCNT register (interrupt/wake-up conditions)
    private int keyCnt = 0;

    public void press(int key) {
        if (key >= 0 && key <= 9) {
            keyState &= ~(1 << key);
        }
    }

    public void release(int key) {
        if (key >= 0 && key <= 9) {
            keyState |= (1 << key);
        }
    }

    public void releaseAll() {
        keyState = 0x03FF;
    }

    public boolean isPressed(int key) {
        return (keyState & (1 << key)) == 0;
    }

    public int readRegister(int offset) {
        switch (offset) {
            case 0x130: return keyState & 0xFF;
            case 0x131: return (keyState >>> 8) & 0x03;
            case 0x132: return keyCnt & 0xFF;
            case 0x133: return (keyCnt >>> 8) & 0xFF;
            default:    return 0;
        }
    }

    public void writeRegister(int offset, int val) {
        if (offset == 0x132)      keyCnt = (keyCnt & 0xFF00) | val;
        else if (offset == 0x133) keyCnt = (keyCnt & 0x00FF) | (val << 8);
    }

    /** Returns true if the key interrupt condition is met */
    public boolean checkKeyInterrupt() {
        if ((keyCnt & (1 << 14)) == 0) return false;
        int mask = keyCnt & 0x3FF;
        boolean or  = (keyCnt & (1 << 15)) == 0;
        int pressed = (~keyState) & 0x3FF & mask;
        return or ? pressed != 0 : pressed == mask;
    }

    public void reset() {
        keyState = 0x03FF;
        keyCnt   = 0;
    }
}
