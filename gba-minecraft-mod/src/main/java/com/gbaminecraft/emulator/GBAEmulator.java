package com.gbaminecraft.emulator;

import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.input.GBAInput;
import com.gbaminecraft.emulator.timer.TimerController;
import com.gbaminecraft.emulator.dma.DMAController;
import com.gbaminecraft.emulator.cartridge.Cartridge;
import com.gbaminecraft.GBAMod;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main GBA Emulator — orchestrates all subsystems.
 * Runs on a dedicated thread at ~16.78 MHz (≈280,896 cycles per 60Hz frame).
 * The PPU drives VBlank/HBlank timing; the CPU runs in bursts between scanlines.
 */
public class GBAEmulator {

    // GBA runs at 16,777,216 Hz; ~280,896 cycles per frame at 59.7275 FPS
    public static final int CPU_FREQ         = 16_777_216;
    public static final double FRAME_RATE    = 59.7275;
    public static final int CYCLES_PER_FRAME = (int)(CPU_FREQ / FRAME_RATE);

    // ── Subsystems ─────────────────────────────────────────────────────────
    private final MemoryBus      bus;
    private final ARM7TDMI       cpu;
    private final PPU            ppu;
    private final APU            apu;
    private final GBAInput       input;
    private final TimerController timers;
    private final DMAController  dma;
    private Cartridge            cartridge;

    // ── Thread control ─────────────────────────────────────────────────────
    private Thread emulatorThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean paused = false;
    private volatile double  speedMultiplier = 1.0;

    // ── Frame output ───────────────────────────────────────────────────────
    private volatile int[]   latestFrame  = null;
    private volatile boolean hasNewFrame  = false;

    // ── Audio output ───────────────────────────────────────────────────────
    private GBAAudioOutput   audioOut = null;
    private final short[]    audioDrain = new short[APU.BUFFER_SIZE * 2];
    private volatile boolean audioEnabled = true;
    public void setAudioEnabled(boolean v) {
        audioEnabled = v;
        if (audioOut != null) audioOut.setMuted(!v);
    }
    public boolean isAudioEnabled() { return audioEnabled; }

    // ── State ──────────────────────────────────────────────────────────────
    private boolean romLoaded = false;
    private String  romName   = "No ROM loaded";

    // FPS tracking
    private long    lastFpsTime    = 0;
    private int     frameCount     = 0;
    private volatile double currentFps = 0;

    // Adaptive frame skip
    private volatile boolean frameSkipEnabled = true;
    private boolean lastFrameSkipped = false;
    public void setFrameSkipEnabled(boolean v) { this.frameSkipEnabled = v; }
    public boolean isFrameSkipEnabled() { return frameSkipEnabled; }

    // Optional boot/diagnostics tracer (near-zero cost when disabled)
    private final com.gbaminecraft.emulator.debug.BootTracer tracer =
            new com.gbaminecraft.emulator.debug.BootTracer(256);
    public com.gbaminecraft.emulator.debug.BootTracer getTracer() { return tracer; }

    public GBAEmulator() {
        bus    = new MemoryBus();
        cpu    = new ARM7TDMI(bus);
        ppu    = new PPU(bus);
        apu    = new APU(bus);
        input  = new GBAInput();
        timers = new TimerController(bus);
        dma    = new DMAController(bus);

        bus.connectSubsystems(ppu, apu, input, timers, dma, cpu);
        dma.connectAPU(apu);

        // High-Level Emulation of the BIOS SWI calls (Div, CpuSet, LZ77, VBlankIntrWait, ...)
        com.gbaminecraft.emulator.bios.HleBios hle =
                new com.gbaminecraft.emulator.bios.HleBios(cpu, bus);
        hle.setTracer(tracer);
        cpu.setHleBios(hle);

        // Sound FIFO: a timer overflow advances the DMA-fed audio sample and
        // refills the FIFO from memory (DMA1/DMA2 in special/FIFO mode).
        timers.setOverflowListener(idx -> {
            int soundCntH = bus.read16(0x04000082);
            int timerA = (soundCntH >> 10) & 1; // FIFO A timer select
            int timerB = (soundCntH >> 14) & 1; // FIFO B timer select
            if (idx == timerA) apu.popFifoA();
            if (idx == timerB) apu.popFifoB();
            dma.onTimerOverflow(idx);
        });
    }

    // ── ROM loading ────────────────────────────────────────────────────────
    public boolean loadROM(byte[] romData, String name) {
        boolean wasRunning = running.get();
        if (wasRunning) stop();

        cartridge = new Cartridge();
        if (!cartridge.loadROM(romData)) {
            GBAMod.LOGGER.error("Failed to load GBA ROM: {}", name);
            return false;
        }

        bus.loadROM(cartridge.getROMData());
        romName = name;
        romLoaded = true;

        // Cartridge GPIO + RTC: Pokémon Ruby/Sapphire/Emerald (game codes
        // AXV*/AXP*/BPE*) carry a Seiko RTC on the GPIO lines. Emerald polls it
        // during boot and stalls before the menu if it never answers.
        String code = cartridge.getGameCode();
        if (code != null && (code.startsWith("BPE") || code.startsWith("AXV")
                || code.startsWith("AXP") || code.startsWith("BPR") || code.startsWith("BPG"))) {
            bus.setGpioRtc(new com.gbaminecraft.emulator.cartridge.GpioRtc());
        } else {
            bus.setGpioRtc(null);
        }

        // Set up the save chip backing. Flash (RSE/FRLG) uses a command protocol;
        // SRAM/EEPROM fall back to the plain SRAM array in the bus.
        switch (cartridge.getSaveType()) {
            case FLASH_1M:
                bus.setFlash(new com.gbaminecraft.emulator.memory.FlashMemory(
                        com.gbaminecraft.emulator.memory.FlashMemory.Size.M1));
                bus.setEeprom(null);
                break;
            case FLASH_512K:
                bus.setFlash(new com.gbaminecraft.emulator.memory.FlashMemory(
                        com.gbaminecraft.emulator.memory.FlashMemory.Size.K512));
                bus.setEeprom(null);
                break;
            case EEPROM_4K:
                bus.setEeprom(new com.gbaminecraft.emulator.memory.Eeprom(
                        com.gbaminecraft.emulator.memory.Eeprom.Size.K4));
                bus.setFlash(null);
                break;
            case EEPROM_64K:
                bus.setEeprom(new com.gbaminecraft.emulator.memory.Eeprom(
                        com.gbaminecraft.emulator.memory.Eeprom.Size.K64));
                bus.setFlash(null);
                break;
            default:
                bus.setFlash(null);
                bus.setEeprom(null);
                break;
        }

        reset();

        GBAMod.LOGGER.info("Loaded GBA ROM: {}", cartridge);

        if (wasRunning) start();
        return true;
    }

    // ── Reset ──────────────────────────────────────────────────────────────
    public void reset() {
        cpu.reset();
        ppu.reset();
        apu.reset();
        input.reset();
        timers.reset();
        dma.reset();
        bus.reset();
        if (cartridge != null && cartridge.getROMData() != null) {
            bus.loadROM(cartridge.getROMData());
        }
    }

    // ── Start / stop emulator thread ───────────────────────────────────────
    public synchronized void start() {
        if (!romLoaded) {
            GBAMod.LOGGER.warn("Cannot start GBA: no ROM loaded.");
            return;
        }
        if (running.get()) return;

        running.set(true);
        paused = false;

        // Reset the boot tracer but DO NOT auto-enable it: tracing executes an
        // extra memory read + bookkeeping call on EVERY CPU instruction (~16M
        // per second), which crushes the frame rate. It stays off by default and
        // the player can turn it on from the "Trace" button only when diagnosing
        // a boot problem.
        tracer.reset();

        emulatorThread = new Thread(this::emulatorLoop, "GBA-Emulator");
        emulatorThread.setDaemon(true);
        emulatorThread.setPriority(Thread.NORM_PRIORITY + 1);
        emulatorThread.start();
        GBAMod.LOGGER.info("GBA emulator started: {}", romName);
    }

    public synchronized void stop() {
        running.set(false);
        if (audioOut != null) { audioOut.close(); audioOut = null; }
        if (emulatorThread != null) {
            emulatorThread.interrupt();
            try { emulatorThread.join(1000); } catch (InterruptedException ignored) {}
            emulatorThread = null;
        }
        GBAMod.LOGGER.info("GBA emulator stopped.");
    }

    public void pause()  { paused = true; }
    public void resume() { paused = false; }
    public boolean isPaused() { return paused; }

    // ── Main emulator loop ─────────────────────────────────────────────────
    private void emulatorLoop() {
        final long targetNsPerFrame = (long)(1_000_000_000.0 / FRAME_RATE / speedMultiplier);
        long lastFrameTime = System.nanoTime();

        // Open the audio device on the emulator thread (degrades to muted if
        // unavailable, e.g. headless test environments).
        if (audioOut == null) {
            audioOut = new GBAAudioOutput();
            audioOut.setMuted(!audioEnabled);
        }

        while (running.get()) {
            if (paused) {
                try { Thread.sleep(16); } catch (InterruptedException e) { break; }
                lastFrameTime = System.nanoTime();
                continue;
            }

            long frameStart = System.nanoTime();

            // Adaptive frame skip: when we can't keep up with realtime, render
            // every other frame (logic + audio still run full speed). This is
            // cheap (the PPU still ticks for IRQs/timing, only the actual scan
            // line drawing is skipped) and is what keeps audio gap-free on
            // hardware that can't sustain 60 FPS rendered.
            //
            // Triggers when the previous frame took more than ~14 ms (i.e. would
            // give <72 FPS); recovers automatically once frames are fast again.
            if (frameSkipEnabled) {
                long lastFrameNs = frameStart - lastFrameTime;
                if (lastFrameNs > 14_000_000L && !lastFrameSkipped) {
                    ppu.setSkipRender(true);
                    lastFrameSkipped = true;
                } else {
                    ppu.setSkipRender(false);
                    lastFrameSkipped = false;
                }
            }
            lastFrameTime = frameStart;

            // Run one full frame worth of cycles
            runFrame();

            // Drain this frame's audio samples to the sound device.
            if (audioOut != null && audioOut.isEnabled()) {
                int n = apu.drainInto(audioDrain);
                if (n > 0) audioOut.submit(audioDrain, n / 2);
            }

            // FPS tracking
            frameCount++;
            long now = System.nanoTime();
            if (now - lastFpsTime >= 1_000_000_000L) {
                currentFps = frameCount;
                frameCount = 0;
                lastFpsTime = now;
            }

            // Timing: sleep to maintain target FPS
            long elapsed = System.nanoTime() - frameStart;
            long targetNs = (long)(targetNsPerFrame / speedMultiplier);
            long sleepNs  = targetNs - elapsed;
            if (sleepNs > 1_000_000L) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int)(sleepNs % 1_000_000L));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    // ── Single frame execution ─────────────────────────────────────────────
    private void runFrame() {
        int cyclesLeft = CYCLES_PER_FRAME;
        // Hoist the tracer flag out of the per-instruction path (cheaper than a
        // virtual call ~16M times/second).
        final boolean trace = tracer.isEnabled();

        while (cyclesLeft > 0) {
            int cycles;

            // Check for pending IRQs
            if (bus.isIRQPending()) {
                if (trace) tracer.onIrq(bus.read16(0x04000202));
                cpu.triggerIRQ();
            }

            // Advance CPU
            if (cpu.halted) {
                cycles = 4; // Stall until IRQ wakes it up
            } else {
                if (trace) {
                    int pc = cpu.getPC();
                    int instr = cpu.isThumb() ? bus.read16(pc & ~1) : bus.read32(pc & ~3);
                    tracer.onStep(pc, instr, cpu);
                }
                cycles = cpu.step() * 4; // Convert to master cycles
            }

            // Tick all subsystems
            ppu.tick(cycles);
            apu.tick(cycles);
            timers.tick(cycles);
            bus.tickSerial(cycles);

            // Fire VBlank-triggered DMA exactly once per frame
            if (ppu.pollVBlankEdge()) {
                dma.onVBlank();
            }
            // Fire HBlank-triggered DMA once per visible line
            if (ppu.pollHBlankEdge()) {
                dma.onHBlank();
            }

            // Check key interrupt
            if (input.checkKeyInterrupt()) bus.requestInterrupt(1 << 12);

            cyclesLeft -= cycles;
        }

        // Capture frame when PPU signals new frame
        if (ppu.pollNewFrame()) {
            // Skip the 38KB array clone on frames the PPU didn't actually draw
            // (frame-skip) — the player still sees the previous frame, which is
            // what we want.
            if (!ppu.isSkipRender()) {
                latestFrame = ppu.getFramebuffer().clone();
                hasNewFrame = true;
            }
            if (trace) tracer.onFrame();
        }
    }

    // ── Frame access ───────────────────────────────────────────────────────
    /** Returns the latest rendered frame (ARGB pixels, 240×160) or null */
    public int[] pollFrame() {
        if (hasNewFrame) {
            hasNewFrame = false;
            return latestFrame;
        }
        return null;
    }

    public int[] getLatestFrame() { return latestFrame; }

    // ── Input ──────────────────────────────────────────────────────────────
    public void pressKey(int key)  { input.press(key); }
    public void releaseKey(int key){ input.release(key); }
    public void releaseAllKeys()   { input.releaseAll(); }
    public GBAInput getInput()     { return input; }

    // ── State queries ──────────────────────────────────────────────────────
    public boolean isRunning()     { return running.get(); }
    public boolean isRomLoaded()   { return romLoaded; }
    public String  getRomName()    { return romName; }
    public double  getCurrentFps() { return currentFps; }
    public Cartridge getCartridge(){ return cartridge; }
    public PPU     getPPU()        { return ppu; }
    public ARM7TDMI getCPU()       { return cpu; }
    public MemoryBus getBus()      { return bus; }

    public void setSpeedMultiplier(double mult) {
        this.speedMultiplier = Math.max(0.1, Math.min(8.0, mult));
    }
    public double getSpeedMultiplier() { return speedMultiplier; }

    /** Enable/disable the boot tracer and return a diagnostic report. */
    public void setTracing(boolean on) { tracer.setEnabled(on); if (on) tracer.reset(); }
    public boolean isTracing()         { return tracer.isEnabled(); }
    public String getDiagnostics()     { return tracer.report(cpu, bus); }
}
