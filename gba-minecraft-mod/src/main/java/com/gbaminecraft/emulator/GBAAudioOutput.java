package com.gbaminecraft.emulator;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.apu.APU;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
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

    // FBA z11 — abrir la línea a la tasa NATIVA del dispositivo para evitar que
    // el SO resamplee nuestro stream de forma continua.
    //
    // Causa probable del siseo constante (z8..z10): el dispositivo/Windows del
    // usuario corre a 44100 Hz, pero abríamos la línea a 48000 (orden de 13z3).
    // Windows entonces resamplea 48000→44100 en modo compartido con filtros
    // pobres, TODO el rato que el stream está abierto → ruido de banda ancha
    // constante desde que arranca el audio, independiente del contenido (encaja
    // con lo que reportó el usuario). Si abrimos a la tasa real del dispositivo,
    // el SO no resamplea.
    //
    // Estrategia: detectar las tasas concretas que el dispositivo por defecto
    // declara soportar (su tasa nativa) y probarlas PRIMERO; si la detección no
    // da nada (algunos mixers reportan NOT_SPECIFIED), caer a 44100 antes que
    // 48000 (44100 es la tasa compartida más común en headsets/Windows).
    private static final int[] CANDIDATE_RATES = buildRateCandidates();

    private static int[] buildRateCandidates() {
        java.util.LinkedHashSet<Integer> rates = new java.util.LinkedHashSet<>();
        try {
            Mixer mixer = AudioSystem.getMixer(null);   // dispositivo de salida por defecto
            for (Line.Info li : mixer.getSourceLineInfo()) {
                if (li instanceof DataLine.Info) {
                    for (AudioFormat f : ((DataLine.Info) li).getFormats()) {
                        float sr = f.getSampleRate();
                        if (sr != AudioSystem.NOT_SPECIFIED && sr >= 8000 && sr <= 192000
                                && f.getChannels() == 2 && f.getSampleSizeInBits() == 16) {
                            rates.add((int) sr);            // tasa nativa del dispositivo
                        }
                    }
                }
            }
        } catch (Throwable ignored) { /* detección best-effort */ }
        // Fallbacks en orden de preferencia (44100 antes que 48000):
        for (int r : new int[] { 44100, 48000, APU.SAMPLE_RATE, 22050 }) rates.add(r);
        int[] out = new int[rates.size()];
        int i = 0;
        for (int r : rates) out[i++] = r;
        return out;
    }

    // FBA 13z9 — núcleo windowed-sinc (polyphase) para el resampleo
    // APU.SAMPLE_RATE (32768 Hz) -> deviceRate. La interpolación lineal anterior
    // es un paso-bajo pobre: dejaba pasar ~3.2 % de energía de imaging por encima
    // de 16.4 kHz (el contenido de 8 bits del Direct Sound se replica como ruido
    // de banda ancha = el siseo/"estática" tipo TV). Un kernel sinc con ventana
    // Hann de 16 taps baja ese imaging a ~0.03 % (medido con la ROM real, ver
    // emulator-tests/measure_resampler.py). Es el mismo enfoque de mGBA
    // (.mgba-ref/audio-resampler.c, mINTERPOLATOR_SINC). Coste: 16 taps × 2
    // canales × deviceRate ≈ 1.5 M MAC/s — despreciable en el hilo de audio.
    private static final int SINC_HALF   = 8;             // 2*HALF = 16 taps
    private static final int SINC_TAPS   = SINC_HALF * 2;
    private static final int SINC_PHASES = 512;
    private static final float[][] SINC_TABLE = buildSincTable();

    private static float[][] buildSincTable() {
        float[][] table = new float[SINC_PHASES][SINC_TAPS];
        for (int p = 0; p < SINC_PHASES; p++) {
            double frac = (double) p / SINC_PHASES;
            double[] h = new double[SINC_TAPS];
            double sum = 0.0;
            for (int k = 0; k < SINC_TAPS; k++) {
                double t = frac - (k - SINC_HALF + 1);          // distancia (en frames) al sample k
                double sinc = (t == 0.0) ? 1.0 : Math.sin(Math.PI * t) / (Math.PI * t);
                double win  = (Math.abs(t) < SINC_HALF) ? 0.5 + 0.5 * Math.cos(Math.PI * t / SINC_HALF) : 0.0;
                h[k] = sinc * win;
                sum += h[k];
            }
            double norm = (sum != 0.0) ? sum : 1.0;             // ganancia DC = 1
            for (int k = 0; k < SINC_TAPS; k++) table[p][k] = (float) (h[k] / norm);
        }
        return table;
    }

    private static short clampShort(double v) {
        if (v >  32767.0) return  32767;
        if (v < -32768.0) return -32768;
        return (short) Math.round(v);
    }

    // FBA z12 — DC-blocking high-pass on the FINAL device-rate output stream.
    //
    // Diagnóstico (síntoma): el siseo de banda ancha NO empieza al abrir la
    // línea (eso ocurre al arrancar el emulador, ~1300 frames antes), sino
    // EXACTAMENTE cuando el GBA produce su primer audio (el "ding" de GameFreak,
    // cuando arranca MP2K). Es decir, el ruido está GATEADO por la presencia de
    // contenido no-cero, no por la apertura de la línea. Eso descarta las
    // hipótesis de "estado inicial del ring / cushion / buffer size" (producirían
    // ruido desde la apertura, sin contenido) y apunta a un offset de DC en la
    // mezcla (hipótesis 4).
    //
    // Divergencia con mGBA: en .mgba-ref/gba-audio.c, _applyBias() calcula
    // (sample - bias), restando el pedestal de SOUNDBIAS para CENTRAR la salida
    // en 0. Este emulador nunca resta bias y, además, su PSG es UNIPOLAR (los
    // canales suman 0..15, ver APU.generateSample), por lo que la mezcla final
    // (psgL*20 + dsLpL2*110) arrastra un pedestal de DC dependiente del contenido
    // que aparece en cuanto suena cualquier cosa.
    //
    // Por qué ese DC se OYE como siseo en el equipo del usuario (Realtek onboard
    // + EarPods, Windows 11, modo compartido WASAPI): los APO de Realtek (p.ej.
    // "Loudness Equalization") aplican un AGC/compresor. Un pedestal de DC
    // continuo hace que el AGC reevalúe su ganancia contra ese pedestal y module
    // el piso de ruido (cuantización de 8 bits del Direct Sound), produciendo un
    // siseo constante e independiente del contenido por encima de la música.
    // Headless no pasa por WASAPI/APO, por eso las métricas no lo veían; y z9/z10/
    // z11 no tocaban el DC, por eso el oído no notó cambio.
    //
    // Solución: un filtro paso-alto de 1 polo (bloqueador de DC) a ~10 Hz sobre
    // el stream YA resampleado, justo antes de escribir a la SourceDataLine. Solo
    // elimina DC y sub-graves < ~10 Hz (inaudibles en GBA); no altera nada del
    // contenido audible y deja el silencio en silencio (entrada 0 -> salida 0).
    // No toca el APU, el PSG, ni los fixes 13g/13h/13z2.
    private static final double DC_BLOCK_HZ = 10.0;
    private double dcInL = 0.0, dcOutL = 0.0, dcInR = 0.0, dcOutR = 0.0;

    /** One-pole DC blocker for the left channel: y[n] = x[n] - x[n-1] + R*y[n-1]. */
    private short dcBlockLeft(double x, double r) {
        double y = x - dcInL + r * dcOutL;
        dcInL = x;
        dcOutL = y;
        return clampShort(y);
    }

    /** One-pole DC blocker for the right channel: y[n] = x[n] - x[n-1] + R*y[n-1]. */
    private short dcBlockRight(double x, double r) {
        double y = x - dcInR + r * dcOutR;
        dcInR = x;
        dcOutR = y;
        return clampShort(y);
    }

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
        // FBA z12 — DC-blocker pole, computed for THIS device rate so the cutoff
        // is ~10 Hz regardless of whether the line opened at 44100/48000/32768.
        final double dcR = Math.max(0.0, 1.0 - (2.0 * Math.PI * DC_BLOCK_HZ / deviceRate));
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
                    // FBA z12 — DC-block before packing (removes the content-gated
                    // DC pedestal; silence stays silence).
                    short lo = dcBlockLeft(l, dcR);
                    short ro = dcBlockRight(rr, dcR);
                    buf[i*4]   = (byte)(lo & 0xFF);   buf[i*4+1] = (byte)((lo >> 8) & 0xFF);
                    buf[i*4+2] = (byte)(ro & 0xFF);   buf[i*4+3] = (byte)((ro >> 8) & 0xFF);
                }
                readPos = r + outFrames * 2;
                writtenTotal += outFrames * 2L;
            } else {
                // Band-limited resample 32768 Hz -> deviceRate (FBA 13z9).
                // Windowed-sinc polyphase kernel; see SINC_TABLE above. Replaces
                // the old linear interpolation that produced the broadband hiss.
                while (outFrames < maxOut) {
                    int idx = (int) pos;
                    if (idx + SINC_HALF > availFrames - 1) break;     // need lookahead taps
                    int phase = (int) ((pos - idx) * SINC_PHASES);
                    if (phase >= SINC_PHASES) phase = SINC_PHASES - 1;
                    float[] taps = SINC_TABLE[phase];
                    short l, rr;
                    if (mute) { l = 0; rr = 0; }
                    else {
                        double accL = 0.0, accR = 0.0;
                        for (int k = 0; k < SINC_TAPS; k++) {
                            int s = idx - SINC_HALF + 1 + k;          // source frame, relative to r
                            if (s < 0) continue;                       // pre-start history treated as 0
                            int bi = (r + s*2) & ringMask;
                            float c = taps[k];
                            accL += ring[bi]     * c;
                            accR += ring[bi + 1] * c;
                        }
                        l  = clampShort(accL);
                        rr = clampShort(accR);
                    }
                    // FBA z12 — DC-block before packing (removes the content-gated
                    // DC pedestal; silence stays silence).
                    short lo = dcBlockLeft(l, dcR);
                    short ro = dcBlockRight(rr, dcR);
                    buf[outFrames*4]   = (byte)(lo & 0xFF);   buf[outFrames*4+1] = (byte)((lo >> 8) & 0xFF);
                    buf[outFrames*4+2] = (byte)(ro & 0xFF);   buf[outFrames*4+3] = (byte)((ro >> 8) & 0xFF);
                    outFrames++;
                    pos += step;
                }
                // Consume whole source frames but retain SINC_HALF-1 frames of
                // history so the kernel always has past samples to convolve.
                int consumed = (int) pos - (SINC_HALF - 1);
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
