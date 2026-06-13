import com.gbaminecraft.emulator.apu.APU;
import java.io.*;
import java.util.*;

/**
 * AudioProbe — analizador headless de la salida del APU.
 *
 * No depende de Minecraft ni del ROM: maneja el APU directamente con escenarios
 * deterministas para medir la CALIDAD de la mezcla (offset DC, pico, clipping,
 * RMS, cruces por cero) antes y despues de cambios en generateSample().
 *
 * Escenarios:
 *   1) PSG CH1 square  -> mide el offset de DC (las cuadradas son unipolares 0..15).
 *   2) Direct Sound seno full-scale (un FIFO) -> nivel/headroom.
 *   3) Direct Sound DUAL full-scale (A+B mismo lado) -> peor caso de clipping.
 *   4) Direct Sound con offset constante -> prueba directa del DC-blocker.
 *
 * Cada escenario imprime metricas y vuelca un WAV en emulator-tests/.audio/.
 */
public class AudioProbe {

    static final int SR = APU.SAMPLE_RATE; // 32768

    public static void main(String[] args) throws Exception {
        File outDir = new File("emulator-tests/.audio");
        outDir.mkdirs();

        System.out.println("==> AudioProbe (sampleRate=" + SR + " Hz)\n");

        scenarioPsgSquare(outDir);
        scenarioDirectSoundSine(outDir, false);
        scenarioDirectSoundSine(outDir, true);
        scenarioDirectSoundDC(outDir);

        System.out.println("\n==> Listo. WAVs en " + outDir.getPath());
    }

    // ── Escenario 1: PSG CH1 square (mide DC offset / simetria) ──────────────
    static void scenarioPsgSquare(File outDir) throws Exception {
        APU apu = new APU(null);
        apu.writeRegister(0x84, 0x80);          // master enable (NR52 bit7)
        apu.writeRegister(0x80, 0x77);          // NR50: master vol L=7,R=7
        apu.writeRegister(0x81, 0x11);          // NR51: CH1 -> L (bit12) + R (bit8)
        apu.writeRegister(0x82, 0x02);          // SOUNDCNT_H: PSG vol = 100% (bits0-1=2)
        apu.writeRegister(0x62, 0x80);          // NR11: duty 50% (bits6-7=2)
        apu.writeRegister(0x63, 0xF0);          // NR12: volumen inicial 15, sin envolvente
        apu.writeRegister(0x64, 0x80);          // NR14: trigger (bit7)

        short[] buf = capture(apu, 16384);
        Metrics m = analyze(buf);
        System.out.println("[1] PSG CH1 square 50% vol=15:");
        m.print();
        writeWav(new File(outDir, "01_psg_square.wav"), buf);
        System.out.println();
    }

    // ── Escenario 2/3: Direct Sound seno (1 o 2 FIFOs) ───────────────────────
    static void scenarioDirectSoundSine(File outDir, boolean dual) throws Exception {
        APU apu = new APU(null);
        apu.writeRegister(0x84, 0x80);          // master enable
        // SOUNDCNT_H: vol PSG=100% (bits0-1=2), chA 100% (bit2), chB 100% (bit3),
        // chA L+R (bits8,9), chB L+R (bits12,13)
        int soundcntH = 0x02 | (1 << 2) | (1 << 8) | (1 << 9);
        if (dual) soundcntH |= (1 << 3) | (1 << 12) | (1 << 13);
        apu.writeRegister(0x82, soundcntH & 0xFF);
        apu.writeRegister(0x83, (soundcntH >> 8) & 0xFF);

        int n = 16384;
        short[] all = new short[n * 2];
        short[] drain = new short[16];
        double freq = 440.0;
        int pos = 0;
        for (int i = 0; i < n; i++) {
            int s8 = (int) Math.round(127 * Math.sin(2 * Math.PI * freq * i / SR));
            apu.pushFifoA(new byte[]{(byte) s8});
            apu.popFifoA();
            if (dual) { apu.pushFifoB(new byte[]{(byte) s8}); apu.popFifoB(); }
            apu.tick(512);
            int got = apu.drainInto(drain);
            for (int k = 0; k < got && pos < all.length; k++) all[pos++] = drain[k];
        }
        short[] buf = Arrays.copyOf(all, pos);
        Metrics m = analyze(buf);
        System.out.println("[" + (dual ? "3] Direct Sound DUAL" : "2] Direct Sound 1ch") + " seno 440Hz full-scale:");
        m.print();
        writeWav(new File(outDir, dual ? "03_directsound_dual.wav" : "02_directsound_1ch.wav"), buf);
        System.out.println();
    }

    // ── Escenario 4: Direct Sound con offset DC constante (prueba DC-blocker) ─
    static void scenarioDirectSoundDC(File outDir) throws Exception {
        APU apu = new APU(null);
        apu.writeRegister(0x84, 0x80);
        int soundcntH = 0x02 | (1 << 2) | (1 << 8) | (1 << 9);
        apu.writeRegister(0x82, soundcntH & 0xFF);
        apu.writeRegister(0x83, (soundcntH >> 8) & 0xFF);

        int n = 16384;
        short[] all = new short[n * 2];
        short[] drain = new short[16];
        int pos = 0;
        for (int i = 0; i < n; i++) {
            int s8 = 60; // offset DC constante
            apu.pushFifoA(new byte[]{(byte) s8});
            apu.popFifoA();
            apu.tick(512);
            int got = apu.drainInto(drain);
            for (int k = 0; k < got && pos < all.length; k++) all[pos++] = drain[k];
        }
        short[] buf = Arrays.copyOf(all, pos);
        Metrics m = analyze(buf);
        System.out.println("[4] Direct Sound DC constante (+60): el DC-blocker debe llevar la media ~0:");
        m.print();
        writeWav(new File(outDir, "04_directsound_dc.wav"), buf);
        System.out.println();
    }

    // ── Captura via tick + drain ─────────────────────────────────────────────
    static short[] capture(APU apu, int samples) {
        short[] all = new short[samples * 2];
        short[] drain = new short[16];
        int pos = 0;
        for (int i = 0; i < samples; i++) {
            apu.tick(512);
            int got = apu.drainInto(drain);
            for (int k = 0; k < got && pos < all.length; k++) all[pos++] = drain[k];
        }
        return Arrays.copyOf(all, pos);
    }

    // ── Metricas ─────────────────────────────────────────────────────────────
    static class Metrics {
        double meanL, peakL, rmsL; long clipL; long zcL; int nFrames;
        void print() {
            System.out.printf("    frames=%d  meanDC(L)=%.1f  peak(L)=%.0f  rms(L)=%.1f  clip(L)=%d  zeroCross/s=%.0f%n",
                    nFrames, meanL, peakL, rmsL, clipL, zcL * (double) SR / Math.max(1, nFrames));
        }
    }

    static Metrics analyze(short[] interleaved) {
        Metrics m = new Metrics();
        int frames = interleaved.length / 2;
        m.nFrames = frames;
        double sum = 0, sumSq = 0; double peak = 0; long clip = 0, zc = 0;
        int prevSign = 0;
        for (int i = 0; i < frames; i++) {
            int l = interleaved[i * 2];
            sum += l; sumSq += (double) l * l;
            if (Math.abs(l) > peak) peak = Math.abs(l);
            if (l >= 32767 || l <= -32768) clip++;
            int sign = Integer.signum(l);
            if (sign != 0 && prevSign != 0 && sign != prevSign) zc++;
            if (sign != 0) prevSign = sign;
        }
        m.meanL = frames > 0 ? sum / frames : 0;
        m.rmsL = frames > 0 ? Math.sqrt(sumSq / frames) : 0;
        m.peakL = peak;
        m.clipL = clip;
        m.zcL = zc;
        return m;
    }

    // ── WAV (PCM 16-bit estereo) ─────────────────────────────────────────────
    static void writeWav(File f, short[] interleaved) throws Exception {
        int dataBytes = interleaved.length * 2;
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            o.writeBytes("RIFF"); writeLE(o, 36 + dataBytes); o.writeBytes("WAVE");
            o.writeBytes("fmt "); writeLE(o, 16); writeLE16(o, 1); writeLE16(o, 2);
            writeLE(o, SR); writeLE(o, SR * 2 * 2); writeLE16(o, 4); writeLE16(o, 16);
            o.writeBytes("data"); writeLE(o, dataBytes);
            for (short s : interleaved) { o.writeByte(s & 0xFF); o.writeByte((s >> 8) & 0xFF); }
        }
    }
    static void writeLE(DataOutputStream o, int v) throws IOException {
        o.writeByte(v & 0xFF); o.writeByte((v >> 8) & 0xFF); o.writeByte((v >> 16) & 0xFF); o.writeByte((v >> 24) & 0xFF);
    }
    static void writeLE16(DataOutputStream o, int v) throws IOException {
        o.writeByte(v & 0xFF); o.writeByte((v >> 8) & 0xFF);
    }
}
