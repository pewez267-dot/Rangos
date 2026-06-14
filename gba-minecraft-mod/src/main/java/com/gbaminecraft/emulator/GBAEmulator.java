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

    // ── Frame output (FBA 13f: race-free triple-buffered handoff) ───────────
    // The old design used two buffers and published a bare reference without a
    // lock. The render thread reads the published int[] pixel-by-pixel (~38 KB)
    // straight into the GL texture, which takes long enough that the emulator
    // thread — running on its own clock — could start overwriting that very
    // buffer mid-read. The result was horizontal tearing in the GBA texture
    // (independent of the monitor's own no-VSync tearing).
    //
    // Fix: three buffers plus a small lock that guards only the pointer
    // handoff (never the pixel copy itself, so neither thread blocks on the
    // other's heavy work). The producer always renders into a buffer that is
    // neither the last one it published nor the one the consumer currently
    // holds; with three buffers such a buffer always exists. So the consumer
    // can hold its buffer for as long as it needs and the producer will never
    // write into it. No per-frame allocation (buffers are preallocated).
    private final Object  frameLock     = new Object();
    private final int[][] frameBuffers  =
            new int[3][PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT];
    private int     publishedBuf  = -1;    // last buffer published (guarded by frameLock)
    private int     checkedOutBuf = -1;    // buffer the consumer currently holds (guarded)
    private boolean hasNewFrame   = false; // guarded by frameLock

    // ── Robust diagnostics (FBA 13l) ────────────────────────────────────────
    // Everything here only MEASURES; it never changes emulation behaviour. A
    // periodic line is written to the Minecraft log (logs/latest.log) so the
    // exact latency breakdown (video handoff, audio buffer, input, pacing) can
    // be read off the user's own machine instead of guessed at.
    private final long[] framePublishNs = new long[3]; // when each buffer was published (guarded by frameLock)
    private long lastProducedNs = 0;                   // emulator thread only
    private long ivSumNs = 0, ivMaxNs = 0; private int ivCnt = 0;          // produced-frame wall interval (pacing)
    private long vwSumNs = 0, vwMaxNs = 0; private long vwCnt = 0;          // publish -> render pickup (guarded by frameLock)
    private long consumeCount = 0;                                          // fresh frames handed to the screen (guarded)
    private volatile long lastKeyPressNs = 0;                               // set by pressKey (render thread)
    private volatile boolean keyPressPending = false;
    private long ifSumNs = 0, ifMaxNs = 0; private int ifCnt = 0;          // key press -> next frame published (emulator thread)
    private long diagLastLogNs = 0; private long diagFramesProduced = 0;
    private volatile boolean diagLogging = true;
    // FBA 13p: count Minecraft render-thread frames so the diagnostic shows the
    // ACTUAL Minecraft FPS while playing. If this is far below the monitor rate
    // (e.g. <60 on a 144 Hz screen), Minecraft itself is stuttering — felt as
    // input lag even though the emulator hits 60 fps.
    private volatile long renderCalls = 0;
    public void noteRender() { renderCalls++; }
    public void setDiagLogging(boolean v) { diagLogging = v; }
    public boolean isDiagLogging() { return diagLogging; }

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
    private volatile long   avgWorkNs  = 0; // rolling avg of per-frame work time
    private volatile long   sleepGranNsReport = 0; // measured Thread.sleep granularity

    /** Build marker so the in-game diagnostics confirm exactly which version is
     *  running (rules out a stale JAR when behaviour seems unchanged). */
    public static final String BUILD = "FBA-2026-06-14 cpu-prefetch-waitstates+remove-x4-hack";

    // Adaptive frame skip. Off by default: on capable hardware it is unnecessary
    // and its on/off toggling near the budget boundary produced a visible
    // "smooth / stutter / smooth" cadence. Kept available for very weak setups.
    private volatile boolean frameSkipEnabled = false;
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
        // 13p bajó esto a NORM-1 y el usuario reportó que EMPEORÓ (el hilo se
        // despertaba tarde tras dormir -> el frame y el input salían con retraso
        // variable). Revertido a NORM+1. (renderFps de Minecraft se mantuvo en
        // 144 en todos los casos, así que subirlo no ahoga a Minecraft.)
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

        // Measure how coarse Thread.sleep actually is on this machine (Windows is
        // ~15.6 ms unless the timer is raised; Linux ~1 ms). We then sleep only
        // while we're further from the deadline than that granularity, and
        // busy-spin the rest. This yields the CPU when the OS timer is fine
        // (smooth, low CPU — no spin-induced scheduler jitter that was hitching
        // audio+video together) yet still guarantees an exact 60 FPS when the
        // timer is coarse (pure spin).
        long sleepGranNs;
        {
            long worst = 0;
            for (int i = 0; i < 8; i++) {
                long t = System.nanoTime();
                try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                long d = System.nanoTime() - t;
                if (d > worst) worst = d;
            }
            sleepGranNs = Math.max(1_500_000L, worst + 500_000L); // granularity + 0.5 ms safety
        }
        sleepGranNsReport = sleepGranNs;
        GBAMod.LOGGER.info("FBA: sleep granularity ~{} ms", sleepGranNs / 1_000_000.0);

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
            // drainInto() returns the number of INTERLEAVED SHORTS (L,R,L,R...),
            // and submit() expects that same short count. The old code passed
            // n/2, submitting only HALF of every frame's audio -> the device
            // (32768 Hz) was fed at ~16 kHz and starved constantly (underruns),
            // which is what made playback sound horrible/distorted even though
            // the generated samples themselves were clean. Pass the full count.
            if (audioOut != null && audioOut.isEnabled()) {
                int n = apu.drainInto(audioDrain);
                if (n > 0) audioOut.submit(audioDrain, n);
            }

            // Adaptive frame skip based on ACTUAL work time (not wall-clock frame
            // time, which includes the intentional idle wait — the old version
            // measured the latter and so triggered on every 16.7 ms frame even at
            // a perfect 60 FPS, causing render stutter while walking). We skip the
            // next frame's drawing only when the emulation itself can't fit in the
            // frame budget. On capable hardware this never triggers.
            long workNs = System.nanoTime() - frameStart;
            // Rolling average of actual emulation work per frame (excludes the
            // idle wait). Surfaced in diagnostics so we can tell, on the user's
            // own machine, whether a low FPS is the emulator being CPU-bound
            // (work ~16 ms+) or a pacing problem (work small but FPS still low).
            avgWorkNs = (avgWorkNs * 15 + workNs) / 16;
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
            maybeLogDiagnostics(now);

            // ── Precise frame pacing ────────────────────────────────────────
            // Thread.sleep() has ~15.6 ms granularity on Windows, so the old
            // "sleep(remaining)" pacing quantized the emulator to ~30 FPS there
            // even on a fast CPU — while the headless Linux tests (1 ms timer)
            // always hit 60 and hid the bug. We now sleep only while we have a
            // comfortable margin and busy-spin the final stretch, which lands
            // every frame on the 16.74 ms cadence regardless of OS timer
            // granularity. A daemon "timer-res" thread (see start()) also keeps
            // the JVM's timer at 1 ms so the coarse sleep stays cheap.
            // ── Precise frame pacing + audio-clock sync (FBA 13m) ───────────
            // Base target: one GBA frame.
            long targetNs = (long)(1_000_000_000.0 / FRAME_RATE / speedMultiplier);

            // Dynamic rate control. The sound card consumes at its own fixed
            // crystal rate (e.g. 32768 Hz), which is NOT exactly our
            // 59.7275 fps * 548.6 samples/frame. The diagnostics on real
            // hardware showed the device buffer drifting steadily down
            // (264 ms -> 0 over a few minutes -> underruns/crackle), and the
            // A/V latency drifting with it. So we nudge the frame period by up
            // to +/-1.5% to hold the audio buffer near the cushion target: this
            // removes the drift, prevents the underruns, AND keeps the A/V
            // latency stable so it can be synced. The speed change is far below
            // perceptible (<1.5%). Only while playing at 1x and not muted.
            if (audioOut != null && audioOut.isEnabled() && !audioOut.isMuted()
                    && speedMultiplier == 1.0) {
                int bufMs    = audioOut.bufferedMs();
                int targetMs = audioOut.configuredCushionMs();
                if (bufMs >= 0) {
                    double errSec = (bufMs - targetMs) / 1000.0; // +ve = too much buffered
                    double adj = errSec * 0.08;                  // gentle proportional gain
                    if (adj >  0.015) adj =  0.015;
                    if (adj < -0.015) adj = -0.015;
                    // too much buffered -> lengthen frame (slow) to drain;
                    // too little -> shorten frame (faster) to refill.
                    targetNs = (long)(targetNs * (1.0 + adj));
                }
            }

            long deadline = frameStart + targetNs;
            while (true) {
                long rem = deadline - System.nanoTime();
                if (rem <= 0) break;
                // Sleep while we're more than one sleep-granularity from the
                // deadline (yields the CPU, avoids the spin-induced scheduler
                // jitter that hitched audio+video); busy-spin the final stretch
                // for exact timing. When the OS timer is coarse, sleepGranNs is
                // large so this is effectively a pure spin = guaranteed 60 FPS.
                if (rem > sleepGranNs) {
                    try { Thread.sleep(1); }
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
                // FBA-2026-06-14 (post-13r): cpu.step() now returns the full
                // master-cycle count for the executed instruction, INCLUDING
                // the per-region sequential prefetch waitstate (mGBA semantics:
                // THUMB_PREFETCH_CYCLES = 1 + activeSeqCycles16). The earlier
                // "* 4" multiplier was a sledgehammer that worked at boot only
                // because it accidentally compensated for these missing ROM
                // waitstates — at the cost of running the entire game at ~25%
                // of real-GBA speed (the felt LAG: walking, dialogues, music
                // pacing). Now ROM/EWRAM Thumb fetches charge their proper
                // waits via WAITCNT, IWRAM stays at 1 master cycle/instr, and
                // average per-frame work matches a real GBA (~110-170k instr
                // for Pokémon Emerald). See diagnose notes in MemoryBus for
                // the full reasoning.
                cycles = cpu.step();
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
            // FBA 13f: publish into a buffer the consumer is not reading.
            if (!ppu.isSkipRender()) {
                int work;
                synchronized (frameLock) {
                    // Choose any buffer index that is neither the last published
                    // one nor the one the consumer currently holds. With three
                    // buffers and at most two excluded, one is always free.
                    work = 0;
                    while (work == publishedBuf || work == checkedOutBuf) work++;
                }
                int[] dst = frameBuffers[work];
                // Heavy copy done OUTSIDE the lock so the render thread never
                // blocks waiting for it (and vice-versa).
                System.arraycopy(ppu.getFramebuffer(), 0, dst, 0, dst.length);
                long nowNs = System.nanoTime();
                synchronized (frameLock) {
                    publishedBuf = work;
                    hasNewFrame  = true;
                    framePublishNs[work] = nowNs;
                }
                // ── Diagnostics ──────────────────────────────────────────
                // Pacing: wall interval between produced frames.
                if (lastProducedNs != 0) {
                    long iv = nowNs - lastProducedNs;
                    ivSumNs += iv; if (iv > ivMaxNs) ivMaxNs = iv; ivCnt++;
                }
                lastProducedNs = nowNs;
                diagFramesProduced++;
                // Input -> first frame published after a key press.
                if (keyPressPending) {
                    long lat = nowNs - lastKeyPressNs;
                    ifSumNs += lat; if (lat > ifMaxNs) ifMaxNs = lat; ifCnt++;
                    keyPressPending = false;
                }
            }
            if (trace) tracer.onFrame();
        }
    }

    // ── Frame access ───────────────────────────────────────────────────────
    /** Returns the latest rendered frame (ARGB pixels, 240×160) or null.
     *  FBA 13f: the returned buffer is "checked out" under the lock so the
     *  emulator thread will not overwrite it while the caller reads it. The
     *  caller may hold it until its next pollFrame() call. */
    public int[] pollFrame() {
        synchronized (frameLock) {
            if (!hasNewFrame) return null;
            hasNewFrame   = false;
            checkedOutBuf = publishedBuf;
            // Diagnostics: how long this frame waited between being published by
            // the emulator and being picked up here by the render thread = the
            // video-handoff latency we control (the rest is Minecraft's GPU path).
            long wait = System.nanoTime() - framePublishNs[checkedOutBuf];
            if (wait > 0) { vwSumNs += wait; if (wait > vwMaxNs) vwMaxNs = wait; vwCnt++; }
            consumeCount++;
            return frameBuffers[checkedOutBuf];
        }
    }

    public int[] getLatestFrame() {
        synchronized (frameLock) {
            int idx = checkedOutBuf >= 0 ? checkedOutBuf : publishedBuf;
            return idx >= 0 ? frameBuffers[idx] : null;
        }
    }

    // ── Input ──────────────────────────────────────────────────────────────
    public void pressKey(int key)  {
        lastKeyPressNs = System.nanoTime();
        keyPressPending = true;
        input.press(key);
    }
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

    /** FBA 13l — periodic robust diagnostics line written to the Minecraft log
     *  (logs/latest.log). Measures only; resets its accumulators each interval.
     *  Lets us read the real latency breakdown off the user's machine. */
    private void maybeLogDiagnostics(long nowNs) {
        if (!diagLogging) return;
        if (diagLastLogNs == 0) { diagLastLogNs = nowNs; lastProducedNs = 0; return; }
        long elapsed = nowNs - diagLastLogNs;
        if (elapsed < 2_000_000_000L) return;          // every ~2 s
        double secs = elapsed / 1e9;

        double producedFps = diagFramesProduced / secs;
        double ivAvgMs = ivCnt > 0 ? (ivSumNs / (double) ivCnt) / 1e6 : 0;
        double ivMaxMs = ivMaxNs / 1e6;
        double ifAvgMs = ifCnt > 0 ? (ifSumNs / (double) ifCnt) / 1e6 : 0;
        double ifMaxMs = ifMaxNs / 1e6;

        long vwSum, vwMx, vwC, cons;
        synchronized (frameLock) {
            vwSum = vwSumNs; vwMx = vwMaxNs; vwC = vwCnt; cons = consumeCount;
            vwSumNs = 0; vwMaxNs = 0; vwCnt = 0; consumeCount = 0;
        }
        double vwAvgMs = vwC > 0 ? (vwSum / (double) vwC) / 1e6 : 0;
        double vwMaxMs = vwMx / 1e6;
        double consumeFps = cons / secs;

        long[] inLat = input.sampleInputLatencyNs();   // {avgNs, maxNs, count}
        double inAvgMs = inLat[0] / 1e6, inMaxMs = inLat[1] / 1e6; long inC = inLat[2];

        long rc = renderCalls; renderCalls = 0;
        double renderFps = rc / secs;

        int audioBufMs = audioOut != null ? audioOut.bufferedMs() : -1;
        String audio = audioOut != null ? audioOut.status() : "no-init";

        GBAMod.LOGGER.info(String.format(
            "[FBA-DIAG] %s%n"
          + "  velocidad : emuFps=%.1f (objetivo 59.7)  trabajo/frame=%.2fms  pacing intervalo avg=%.1fms max=%.1fms%n"
          + "  MINECRAFT : renderFps=%.1f  (si << tu monitor => Minecraft se entrecorta = lag)%n"
          + "  VIDEO     : handoff(publicar->recoger) avg=%.1fms max=%.1fms   consumo(GBA)=%.1f fps%n"
          + "  INPUT     : press->lee KEYINPUT avg=%.1fms max=%.1fms (n=%d)   press->frame avg=%.1fms max=%.1fms%n"
          + "  AUDIO     : buffer=%dms  %s",
            BUILD, producedFps, avgWorkNs / 1e6, ivAvgMs, ivMaxMs,
            renderFps,
            vwAvgMs, vwMaxMs, consumeFps,
            inAvgMs, inMaxMs, inC, ifAvgMs, ifMaxMs,
            audioBufMs, audio));

        ivSumNs = 0; ivMaxNs = 0; ivCnt = 0;
        ifSumNs = 0; ifMaxNs = 0; ifCnt = 0;
        diagFramesProduced = 0;
        diagLastLogNs = nowNs;
    }

    public String getDiagnostics()     {
        String base = tracer.report(cpu, bus);
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== FBA HOST / PERF / AUDIO =====\n");
        sb.append("Build: ").append(BUILD).append('\n');
        sb.append(String.format("Emulador FPS=%.1f  trabajo/frame=%.2f ms  (presupuesto 16.74 ms)%n",
                currentFps, avgWorkNs / 1_000_000.0));
        sb.append("speedMultiplier=").append(speedMultiplier)
          .append("  frameSkip=").append(frameSkipEnabled)
          .append(String.format("  sleepGran=%.1f ms (%s)", sleepGranNsReport / 1_000_000.0,
                  sleepGranNsReport > 5_000_000L ? "spin" : "sleep"))
          .append('\n');
        sb.append("Audio: ").append(audioOut == null ? "no inicializado" : audioOut.status()).append('\n');
        sb.append("audioEnabled(usuario)=").append(audioEnabled);
        if (audioOut != null) {
            sb.append(String.format("  cushionConfig=%dms  bufferActual=%dms",
                    audioOut.configuredCushionMs(), audioOut.bufferedMs()));
        }
        sb.append('\n');
        sb.append("Diagnostico continuo: se escribe una linea [FBA-DIAG] cada 2s en logs/latest.log ")
          .append("(video handoff, input, audio, pacing). diagLogging=").append(diagLogging).append('\n');
        sb.append("Pistas: si trabajo/frame << 16ms pero FPS<60 => pacing/SO; si trabajo/frame>=16ms => CPU.\n");
        sb.append("        si Audio submitted=0 => el APU no entrega; si written=0 con submitted>0 => el dispositivo no consume.\n");
        sb.append("=====================================\n");
        return base + sb;
    }
}
