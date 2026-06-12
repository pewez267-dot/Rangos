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
        // Wire the tracer through the CPU too so it can record actual IRQ
        // handler dispatches (otherwise the report's "Handler-juego ejecutado"
        // counter stays at 0 even when the handler is running normally).
        cpu.setTracer(tracer);

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

        // Raise the JVM timer resolution (Windows defaults to ~15.6 ms, which
        // otherwise wrecks frame pacing — see emulatorLoop). A daemon thread that
        // sleeps forever keeps the process timer at ~1 ms for its whole lifetime.
        ensureHighResTimer();

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

    // Keeps the JVM/OS timer at high resolution for accurate frame pacing.
    // On Windows, Thread.sleep granularity is ~15.6 ms unless something holds the
    // timer at a finer period; a daemon thread parked in an indefinite sleep does
    // exactly that for the whole process. Started once, lazily.
    private static volatile Thread timerResThread;
    private static void ensureHighResTimer() {
        if (timerResThread != null) return;
        synchronized (GBAEmulator.class) {
            if (timerResThread != null) return;
            Thread t = new Thread(() -> {
                try { Thread.sleep(Long.MAX_VALUE); } catch (InterruptedException ignored) {}
            }, "FBA-timer-res");
            t.setDaemon(true);
            t.start();
            timerResThread = t;
        }
    }

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

            // Run one full frame worth of cycles
            runFrame();

            // Drain this frame's audio samples to the sound device.
            if (audioOut != null && audioOut.isEnabled()) {
                int n = apu.drainInto(audioDrain);
                if (n > 0) audioOut.submit(audioDrain, n / 2);
            }

            // Adaptive frame skip based on ACTUAL work time (not wall-clock frame
            // time, which includes the intentional idle wait — the old version
            // measured the latter and so triggered on every 16.7 ms frame even at
            // a perfect 60 FPS, causing render stutter while walking). We skip the
            // next frame's drawing only when the emulation itself can't fit in the
            // frame budget. On capable hardware this never triggers.
            long workNs = System.nanoTime() - frameStart;
            if (frameSkipEnabled) {
                if (workNs > targetNsPerFrame && !lastFrameSkipped) {
                    ppu.setSkipRender(true);  lastFrameSkipped = true;
                } else {
                    ppu.setSkipRender(false); lastFrameSkipped = false;
                }
            }
            lastFrameTime = frameStart;

            // FPS tracking
            frameCount++;
            long now = System.nanoTime();
            if (now - lastFpsTime >= 1_000_000_000L) {
                currentFps = frameCount;
                frameCount = 0;
                lastFpsTime = now;
            }

            // ── Precise frame pacing ────────────────────────────────────────
            // Thread.sleep() has ~15.6 ms granularity on Windows, so the old
            // "sleep(remaining)" pacing quantized the emulator to ~30 FPS there
            // even on a fast CPU — while the headless Linux tests (1 ms timer)
            // always hit 60 and hid the bug. We now sleep only while we have a
            // comfortable margin and busy-spin the final stretch, which lands
            // every frame on the 16.74 ms cadence regardless of OS timer
            // granularity. A daemon "timer-res" thread (see start()) also keeps
            // the JVM's timer at 1 ms so the coarse sleep stays cheap.
            long targetNs = (long)(1_000_000_000.0 / FRAME_RATE / speedMultiplier);
            long deadline = frameStart + targetNs;
            while (true) {
                long rem = deadline - System.nanoTime();
                if (rem <= 0) break;
                // Only coarse-sleep when we're far enough ahead that even a
                // worst-case ~15.6 ms Windows sleep overshoot can't pass the
                // deadline; otherwise busy-spin. At 1x speed the remaining time
                // (~13 ms) is always below this threshold, so we spin the idle
                // time and hit a rock-solid 60 FPS on every OS — verified to hold
                // even with a 15.6 ms timer granularity. The spin costs ~one core
                // for a few ms per frame, negligible on modern multi-core CPUs.
                if (rem > 18_000_000L) {
                    try { Thread.sleep((rem - 16_000_000L) / 1_000_000L); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                } else {
                    Thread.onSpinWait();
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
