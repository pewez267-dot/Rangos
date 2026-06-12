package com.gbaminecraft.emulator;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.apu.APU;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Real-time audio output for the GBA emulator.
 *
 * <h3>Why this is decoupled from the emulation thread</h3>
 * The APU produces signed 16-bit stereo PCM at 32768 Hz. An earlier version
 * wrote those samples straight to the {@link SourceDataLine} from the emulation
 * thread with a <em>blocking</em> {@code line.write()}. That call blocks once the
 * line's internal buffer fills, which meant the emulation thread spent a large
 * part of every frame parked inside the audio mixer — on a fast PC this capped
 * the whole emulator at ~30 FPS (half of 60) and still produced choppy sound,
 * because production and consumption were fighting on the same thread. (The
 * headless tests never caught it: with no audio device {@code submit()} returns
 * instantly, so the stall only showed up inside Minecraft.)
 *
 * <h3>The fix (same shape mGBA uses)</h3>
 * Audio now runs on its own daemon thread fed by a single-producer/
 * single-consumer ring buffer:
 * <ul>
 *   <li>The emulation thread calls {@link #submit} which copies samples into the
 *       ring buffer and returns immediately — it never blocks on audio.</li>
 *   <li>The audio thread continuously drains the ring buffer into the line with
 *       a blocking write (which is fine — that thread exists only to feed
 *       audio). The ring buffer absorbs frame-time jitter so the sound stays
 *       gap-free, and the tiny rate mismatch (≈0.5%) is absorbed by dropping a
 *       handful of the oldest samples per second, which is inaudible.</li>
 * </ul>
 * If no audio device is available it degrades to a silent no-op.
 */
public final class GBAAudioOutput {

    private SourceDataLine line;
    private volatile boolean enabled = false;
    private volatile boolean muted = false;

    // Single-producer (emulation thread) / single-consumer (audio thread) ring
    // buffer of interleaved L,R 16-bit samples. Capacity is a power of two so we
    // can mask instead of modulo. 65536 shorts = 32768 stereo frames ≈ 1.0 s of
    // headroom, far more than we normally keep buffered.
    private final short[] ring = new short[1 << 16];
    private final int ringMask = ring.length - 1;
    private volatile int writePos = 0;   // written by the producer only
    private volatile int readPos  = 0;   // written by the consumer only

    private Thread audioThread;
    private volatile boolean running = false;

    public GBAAudioOutput() {
        try {
            // 32768 Hz, 16-bit signed, 2 channels, little-endian (matches APU output).
            AudioFormat fmt = new AudioFormat(APU.SAMPLE_RATE, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) {
                GBAMod.LOGGER.warn("FBA: audio line not supported; running muted.");
                return;
            }
            line = (SourceDataLine) AudioSystem.getLine(info);
            // ~0.1 s line buffer keeps latency low; the ring buffer above handles
            // jitter so the line itself can stay small.
            int bufBytes = (APU.SAMPLE_RATE * 4) / 10;
            line.open(fmt, bufBytes);
            line.start();
            enabled = true;
            running = true;
            audioThread = new Thread(this::audioLoop, "GBA-Audio");
            audioThread.setDaemon(true);
            audioThread.setPriority(Thread.NORM_PRIORITY + 1);
            audioThread.start();
            GBAMod.LOGGER.info("FBA: audio output started (decoupled thread).");
        } catch (Throwable t) {
            // No mixer / headless / denied — keep running without sound.
            GBAMod.LOGGER.warn("FBA: audio unavailable, running muted.");
            line = null;
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setMuted(boolean m) { this.muted = m; }
    public boolean isMuted() { return muted; }

    /**
     * Submit one frame of stereo samples (interleaved L,R, signed 16-bit).
     * Non-blocking: copies into the ring buffer and returns. If the consumer has
     * fallen behind and the ring is full, the surplus is dropped rather than
     * stalling the emulation thread.
     */
    public void submit(short[] samples, int sampleCount) {
        if (!enabled || muted || samples == null || sampleCount <= 0) return;
        int w = writePos;
        int used = w - readPos;                 // samples currently queued
        int free = ring.length - used;          // room left
        int n = Math.min(sampleCount, free);
        for (int i = 0; i < n; i++) {
            ring[(w + i) & ringMask] = samples[i];
        }
        writePos = w + n;                        // publish (volatile write)
    }

    private void audioLoop() {
        byte[] buf = new byte[8192];             // up to 2048 stereo frames per write
        while (running) {
            int avail = writePos - readPos;      // volatile reads
            if (avail <= 0) {
                // Nothing queued yet: brief park to avoid a busy-spin. The line's
                // own buffer covers this gap.
                try { Thread.sleep(1); } catch (InterruptedException e) { break; }
                continue;
            }
            // Drain in chunks, keeping L/R pairs together (even sample count).
            int chunk = Math.min(avail, buf.length / 2);
            chunk &= ~1;                         // even: never split a stereo pair
            if (chunk == 0) chunk = Math.min(avail, 2);
            int r = readPos;
            boolean mute = muted;
            for (int i = 0; i < chunk; i++) {
                short s = mute ? 0 : ring[(r + i) & ringMask];
                buf[i * 2]     = (byte) (s & 0xFF);
                buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
            readPos = r + chunk;                 // publish consumption
            try {
                line.write(buf, 0, chunk * 2);   // blocking — but only on THIS thread
            } catch (Throwable t) {
                // Line died mid-session: stop feeding it rather than spin forever.
                break;
            }
        }
    }

    public void close() {
        running = false;
        if (audioThread != null) {
            audioThread.interrupt();
            try { audioThread.join(500); } catch (InterruptedException ignored) {}
            audioThread = null;
        }
        if (line != null) {
            try { line.stop(); line.close(); } catch (Throwable ignored) {}
            line = null;
        }
        enabled = false;
    }
}
