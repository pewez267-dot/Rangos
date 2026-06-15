package com.gbaminecraft.emulator;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.apu.APU;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Real-time audio output, decoupled from the emulation thread.
 *
 * The emulation thread calls {@link #submit} (non-blocking, writes into a ring
 * buffer); a dedicated daemon thread drains the ring into a {@link SourceDataLine}
 * with a blocking write. That keeps the emulator from ever stalling on audio.
 *
 * Robustness added after a "0 audio" report on real hardware:
 *  - The device line is opened by actually attempting {@code open()} on a few
 *    candidate sample rates (32768 → 48000 → 44100 → 22050) instead of trusting
 *    {@code isLineSupported}, which returns false-negatives on some mixers.
 *  - If the device runs at a rate other than the GBA's 32768 Hz, the audio
 *    thread linearly resamples on the fly, so the pitch stays correct.
 *  - Diagnostic counters ({@link #status}) are surfaced in the in-game trace so
 *    we can see, on the user's machine, whether the line opened, at what rate,
 *    and how many samples actually flowed.
 */
public final class GBAAudioOutput {

    private SourceDataLine line;
    private volatile boolean enabled = false;
    private volatile boolean muted = false;

    // SPSC ring of interleaved L,R 16-bit samples at the GBA's 32768 Hz.
    private final short[] ring = new short[1 << 16];
    private final int ringMask = ring.length - 1;
    private volatile int writePos = 0;   // producer (emulation thread)
    private volatile int readPos  = 0;   // consumer (audio thread)

    private Thread audioThread;
    private volatile boolean running = false;

    // ── Diagnostics (read by the in-game trace) ─────────────────────────────
    private volatile long submittedTotal = 0;   // samples accepted into the ring
    private volatile long writtenTotal   = 0;    // samples written to the device
    private volatile long droppedTotal   = 0;    // samples dropped (ring full)
    private volatile int  deviceRate     = 0;    // 0 = not open
    private volatile String openError    = null; // why the line failed to open

    // FBA 13z3 — preferir 48 kHz nativo del DAC y dejar que NUESTRO resampler
    // interno haga 32 768 → 48 000. Antes el orden empezaba por 32 768 (rate
    // del GBA): Windows lo aceptaba pero internamente resampleaba a 48 kHz
    // con filtros pobres para tasas raras (no múltiplos de 48 000) -> meta
    // de aliasing/aspereza audible (la "distorsion a veces" del 25 % residual
    // que quedó tras el fix del DMA en 13z2). 48 kHz es nativo en
    // prácticamente cualquier DAC moderno: el SO no resamplea, y nuestro
    // resampler lineal de audioLoop introduce <0.5 % de aspereza (verificado
    // espectralmente con resample_experiment.py vs. sinc/ZOH).
    private static final int[] CANDIDATE_RATES = { 48000, 44100, APU.SAMPLE_RATE, 22050 };

    public GBAAudioOutput() {
        for (int rate : CANDIDATE_RATES) {
            try {
                AudioFormat fmt = new AudioFormat(rate, 16, 2, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
                int bufBytes = (rate * 4) / 3;    // ~0.33 s line buffer (room for the cushion below)
                l.open(fmt, bufBytes);
                l.start();
                line = l;
                deviceRate = rate;
                enabled = true;
                break;
            } catch (Throwable t) {
                openError = t.getClass().getSimpleName() + ": " + t.getMessage();
                line = null;
            }
        }
        if (!enabled) {
            GBAMod.LOGGER.warn("FBA: audio unavailable, running muted ({}).", openError);
            return;
        }
        running = true;
        audioThread = new Thread(this::audioLoop, "GBA-Audio");
        audioThread.setDaemon(true);
        audioThread.setPriority(Thread.NORM_PRIORITY + 1);
        audioThread.start();
        GBAMod.LOGGER.info("FBA: audio output started at {} Hz.", deviceRate);
    }

    public boolean isEnabled() { return enabled; }
    public void setMuted(boolean m) { this.muted = m; }
    public boolean isMuted() { return muted; }

    // FBA 13z: stubs para mantener compatibilidad con GBAEmulator (PI controller,
    // cushion config, diag continuo) sin re-introducir las features que tocaban
    // el audio. bufferedMs<0 desactiva el PI controller. cushion config queda
    // congelado en el valor de 13b.
    public int bufferedMs() { return -1; }
    public int configuredCushionMs() { return 150; }
    public void setCushionMs(int ms) { /* no-op: 13b tenía cushion fijo */ }
    public int cycleCushionPreset() { return 150; }

    /** One-line audio status for the diagnostics trace. */
    public String status() {
        if (!enabled) return "DISABLED (" + openError + ")";
        int fill = writePos - readPos;
        return String.format("rate=%dHz submitted=%d written=%d dropped=%d ringFill=%d muted=%b",
                deviceRate, submittedTotal, writtenTotal, droppedTotal, fill, muted);
    }

    /** Submit interleaved L,R signed-16 samples. Non-blocking. */
    public void submit(short[] samples, int sampleCount) {
        if (!enabled || muted || samples == null || sampleCount <= 0) return;
        int w = writePos;
        int free = ring.length - (w - readPos);
        int n = Math.min(sampleCount, free);
        for (int i = 0; i < n; i++) ring[(w + i) & ringMask] = samples[i];
        writePos = w + n;
        submittedTotal += n;
        if (n < sampleCount) droppedTotal += (sampleCount - n);
    }

    private void audioLoop() {
        final boolean resample = deviceRate != APU.SAMPLE_RATE;
        final double step = (double) APU.SAMPLE_RATE / deviceRate; // source frames per output frame
        byte[] buf = new byte[8192];
        double pos = 0.0; // fractional read position within available source frames

        // Pre-fill the device with ~0.15 s of silence to establish a playback
        // cushion. Because the emulator produces and the device consumes at the
        // same 32768 Hz, this cushion is maintained for the whole session — and
        // it is what makes the audio gap-free: when the JVM briefly pauses (GC,
        // scheduler), the sound hardware keeps playing the buffered cushion
        // instead of underrunning (which is the clicking/choppiness). Without it
        // the line buffer sits near-empty and every micro-stall is audible.
        try {
            int cushionFrames = deviceRate * 15 / 100;
            byte[] sil = new byte[Math.min(buf.length, cushionFrames * 4)];
            int remaining = cushionFrames * 4;
            while (remaining > 0) {
                int chunk = Math.min(remaining, sil.length);
                line.write(sil, 0, chunk);
                remaining -= chunk;
            }
        } catch (Throwable ignored) {}

        while (running) {
            int availSamples = writePos - readPos;       // interleaved shorts
            int availFrames  = availSamples >> 1;
            if (availFrames < 2) {
                try { Thread.sleep(1); } catch (InterruptedException e) { break; }
                continue;
            }
            int r = readPos;
            boolean mute = muted;
            int outFrames = 0;
            int maxOut = buf.length / 4;                  // 4 bytes per output stereo frame

            if (!resample) {
                // 1:1 — never split a stereo pair.
                outFrames = Math.min(availFrames, maxOut);
                for (int i = 0; i < outFrames; i++) {
                    short l = mute ? 0 : ring[(r + i*2)     & ringMask];
                    short rr= mute ? 0 : ring[(r + i*2 + 1) & ringMask];
                    buf[i*4]   = (byte)(l & 0xFF);   buf[i*4+1] = (byte)((l >> 8) & 0xFF);
                    buf[i*4+2] = (byte)(rr & 0xFF);  buf[i*4+3] = (byte)((rr >> 8) & 0xFF);
                }
                readPos = r + outFrames * 2;
                writtenTotal += outFrames * 2L;
            } else {
                // Linear resample 32768 Hz -> deviceRate. Keep one frame of slack
                // so interpolation always has idx+1 available.
                while (outFrames < maxOut && (pos + step) < (availFrames - 1)) {
                    int idx = (int) pos;
                    double frac = pos - idx;
                    int b0 = (r + idx*2) & ringMask, b1 = (r + (idx+1)*2) & ringMask;
                    short l, rr;
                    if (mute) { l = 0; rr = 0; }
                    else {
                        l  = (short)(ring[b0]   + (ring[b1]   - ring[b0])   * frac);
                        rr = (short)(ring[b0+1] + (ring[b1+1] - ring[b0+1]) * frac);
                    }
                    buf[outFrames*4]   = (byte)(l & 0xFF);   buf[outFrames*4+1] = (byte)((l >> 8) & 0xFF);
                    buf[outFrames*4+2] = (byte)(rr & 0xFF);  buf[outFrames*4+3] = (byte)((rr >> 8) & 0xFF);
                    outFrames++;
                    pos += step;
                }
                int consumed = (int) pos;                 // whole source frames used
                if (consumed > 0) { readPos = r + consumed * 2; pos -= consumed; writtenTotal += consumed * 2L; }
                if (outFrames == 0) { try { Thread.sleep(1); } catch (InterruptedException e) { break; } continue; }
            }

            try {
                line.write(buf, 0, outFrames * 4);
            } catch (Throwable t) {
                openError = "write failed: " + t.getMessage();
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
