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
 * The APU produces signed 16-bit stereo PCM at 32768 Hz into a buffer that is
 * marked "ready" each frame. This class drains that buffer to a Java Sound
 * {@link SourceDataLine} so the player actually hears the game. Audio output is
 * optional: if no audio device is available (headless tests), it degrades to a
 * silent no-op instead of throwing.
 */
public final class GBAAudioOutput {

    private SourceDataLine line;
    private boolean enabled = false;
    private volatile boolean muted = false;
    private byte[] byteBuf = new byte[0];

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
            // ~0.18s of stereo audio (≈11 frames). Big enough to absorb frame-time
            // jitter without underrunning (which is what caused the choppy/beeping
            // sound), small enough to keep latency low.
            int bufBytes = (APU.SAMPLE_RATE * 4) * 18 / 100; // sampleRate * 4 bytes/frame * 0.18s
            line.open(fmt, bufBytes);
            line.start();
            enabled = true;
            GBAMod.LOGGER.info("FBA: audio output started.");
        } catch (Throwable t) {
            // No mixer / headless / denied — keep running without sound.
            GBAMod.LOGGER.warn("FBA: audio unavailable, running muted.");
            line = null;
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setMuted(boolean m) {
        this.muted = m;
        // Drop any queued audio immediately so unmuting doesn't replay stale sound.
        if (m && line != null) { try { line.flush(); } catch (Throwable ignored) {} }
    }
    public boolean isMuted() { return muted; }

    /**
     * Submit one frame of stereo samples (interleaved L,R, signed 16-bit).
     *
     * We write the WHOLE frame to the line. Because the APU produces samples at
     * exactly the same rate the line consumes them (32768 Hz), the line's own
     * buffer paces the emulator: when it's full the write blocks just long
     * enough to stay in sync, giving gap-free audio. The previous code dropped
     * any samples that didn't fit, which produced constant clicks/interference.
     */
    public void submit(short[] samples, int sampleCount) {
        if (!enabled || line == null || muted || samples == null || sampleCount <= 0) return;
        int needed = sampleCount * 2; // 2 bytes per sample
        if (byteBuf.length < needed) byteBuf = new byte[needed];
        for (int i = 0; i < sampleCount; i++) {
            short s = samples[i];
            byteBuf[i * 2]     = (byte) (s & 0xFF);
            byteBuf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        try {
            // Keep it sample-aligned (multiple of 4 bytes for 16-bit stereo).
            int len = needed & ~3;
            if (len > 0) line.write(byteBuf, 0, len);
        } catch (Throwable ignored) {
            // If the line dies mid-session, fall silent rather than crash the
            // emulator thread.
        }
    }

    public void close() {
        if (line != null) {
            try { line.stop(); line.close(); } catch (Throwable ignored) {}
            line = null;
        }
        enabled = false;
    }
}
