package com.gbaminecraft.emulator.timer;

import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * GBA Timer Controller — 4 timers (TM0-TM3).
 * Each timer has a 16-bit counter, prescaler, cascade mode, and IRQ enable.
 * Prescalers: 0=1, 1=64, 2=256, 3=1024 CPU cycles per tick.
 */
public class TimerController {

    private static final int[] PRESCALERS = {1, 64, 256, 1024};

    // Timer registers (4 timers)
    private final int[]     reload  = new int[4];
    private final int[]     counter = new int[4];
    private final int[]     control = new int[4];
    private final int[]     cycles  = new int[4];
    private final boolean[] enabled = new boolean[4];
    private final boolean[] cascade = new boolean[4];
    private final boolean[] irqEn   = new boolean[4];
    private final int[]     prescaler = new int[4];

    // Interrupt bits for each timer
    private static final int[] IRQ_BITS = {1 << 3, 1 << 4, 1 << 5, 1 << 6};

    private MemoryBus bus;

    // Optional listener for FIFO-audio: called when timer 0 or 1 overflows.
    public interface OverflowListener { void onTimerOverflow(int timerIndex); }
    private OverflowListener overflowListener;
    public void setOverflowListener(OverflowListener l) { this.overflowListener = l; }

    public TimerController(MemoryBus bus) {
        this.bus = bus;
    }

    public void tick(int cpuCycles) {
        for (int i = 0; i < 4; i++) {
            if (!enabled[i]) continue;
            if (cascade[i] && i > 0) continue; // cascade timer ticked by overflow

            // Advance the prescaler arithmetically instead of looping once per
            // CPU cycle. With prescaler=1 (common for Direct Sound timers) the
            // old per-cycle while-loop ran ~280k times per frame per timer and
            // dominated the frame budget; this computes the same result in O(1)
            // plus one iteration per *actual* overflow (rare).
            int acc = cycles[i] + cpuCycles;
            int ps  = prescaler[i];
            int inc = acc / ps;            // counter increments this batch
            cycles[i] = acc - inc * ps;    // leftover sub-tick cycles
            if (inc == 0) continue;

            int c = counter[i] + inc;
            // Each wrap past 0xFFFF is one overflow; after wrapping the counter
            // restarts at reload[i], so the period is (0x10000 - reload[i]).
            while (c > 0xFFFF) {
                c -= (0x10000 - reload[i]);
                overflow(i);
            }
            counter[i] = c;
        }
    }

    private void overflow(int i) {
        // NOTE: the counter value itself is maintained by the arithmetic in
        // tick(); this only performs the per-overflow side effects.
        if (irqEn[i]) bus.requestInterrupt(IRQ_BITS[i]);

        // Sound FIFO: timers 0 and 1 drive the audio DMA replenishment.
        if ((i == 0 || i == 1) && overflowListener != null) {
            overflowListener.onTimerOverflow(i);
        }

        // Cascade next timer (one increment per overflow of this timer)
        if (i < 3 && enabled[i + 1] && cascade[i + 1]) {
            if (++counter[i + 1] > 0xFFFF) {
                counter[i + 1] = reload[i + 1];
                overflow(i + 1);
            }
        }
    }

    public int readRegister(int offset) {
        // TM0CNT_L = 0x100, TM0CNT_H = 0x102, TM1... etc.
        int timer = (offset - 0x100) / 4;
        int reg   = (offset - 0x100) % 4;
        if (timer < 0 || timer > 3) return 0;
        switch (reg) {
            case 0: return counter[timer] & 0xFF;
            case 1: return (counter[timer] >>> 8) & 0xFF;
            case 2: return control[timer] & 0xFF;
            case 3: return 0;
            default: return 0;
        }
    }

    public void writeRegister(int offset, int val) {
        int timer = (offset - 0x100) / 4;
        int reg   = (offset - 0x100) % 4;
        if (timer < 0 || timer > 3) return;
        switch (reg) {
            case 0: // Reload low
                reload[timer] = (reload[timer] & 0xFF00) | (val & 0xFF);
                break;
            case 1: // Reload high
                reload[timer] = (reload[timer] & 0x00FF) | ((val & 0xFF) << 8);
                break;
            case 2: // Control
                boolean wasEnabled = enabled[timer];
                control[timer] = val & 0xFF;
                prescaler[timer] = PRESCALERS[val & 0x3];
                cascade[timer]   = (val & (1 << 2)) != 0;
                irqEn[timer]     = (val & (1 << 6)) != 0;
                enabled[timer]   = (val & (1 << 7)) != 0;
                if (!wasEnabled && enabled[timer]) {
                    // On start: reload counter
                    counter[timer] = reload[timer];
                    cycles[timer]  = 0;
                }
                break;
        }
    }

    public boolean isTimerEnabled(int i) { return enabled[i]; }
    public int getTimerCounter(int i)    { return counter[i]; }

    public void reset() {
        java.util.Arrays.fill(reload, 0);
        java.util.Arrays.fill(counter, 0);
        java.util.Arrays.fill(control, 0);
        java.util.Arrays.fill(cycles, 0);
        java.util.Arrays.fill(enabled, false);
        java.util.Arrays.fill(cascade, false);
        java.util.Arrays.fill(irqEn, false);
        java.util.Arrays.fill(prescaler, 1);
    }
}
