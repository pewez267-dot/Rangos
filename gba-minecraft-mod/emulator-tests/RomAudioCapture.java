import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.input.GBAInput;
import java.io.*;
import java.util.*;

/**
 * RomAudioCapture — arranca un ROM real, lo deja correr (pulsando Start/A para
 * pasar la intro) y captura la salida del APU a un WAV, reportando metricas de
 * la onda. Sirve para validar end-to-end que la mezcla del APU produce audio
 * sano (centrado, sin clipping, a buen nivel) sobre el flujo real del juego.
 *
 * Uso: java -cp ... RomAudioCapture <rom> [bootFrames] [captureFrames]
 */
public class RomAudioCapture {
    public static void main(String[] a) throws Exception {
        String rom = a[0];
        int boot = a.length > 1 ? Integer.parseInt(a[1]) : 4200;
        int cap  = a.length > 2 ? Integer.parseInt(a[2]) : 1800; // ~30 s @60fps
        H h = new H(rom);

        // Arranque + intentar entrar a una pantalla con musica.
        h.frames(boot);
        for (int i = 0; i < 8; i++) { h.pressStart(); h.frames(30); }

        // Capturar audio frame a frame.
        ArrayList<short[]> chunks = new ArrayList<>();
        short[] drain = new short[APU.BUFFER_SIZE * 2];
        long total = 0;
        for (int f = 0; f < cap; f++) {
            h.frame();
            if (f % 90 == 45) { h.tap(GBAInput.KEY_A, 3, 2); } // avanzar dialogos
            int n = h.apu.drainInto(drain);
            if (n > 0) { chunks.add(Arrays.copyOf(drain, n)); total += n; }
        }

        short[] all = new short[(int) total];
        int p = 0;
        for (short[] c : chunks) { System.arraycopy(c, 0, all, p, c.length); p += c.length; }

        Metrics m = analyze(all);
        System.out.println("==> RomAudioCapture: " + rom);
        System.out.printf("    boot=%d cap=%d  shorts=%d (%.2f s estereo)%n",
                boot, cap, all.length, all.length / 2.0 / APU.SAMPLE_RATE);
        m.print();

        File out = new File("emulator-tests/.audio/rom_capture.wav");
        out.getParentFile().mkdirs();
        writeWav(out, all);
        System.out.println("    WAV -> " + out.getPath());
    }

    static class Metrics {
        int nFrames; double meanL, peakL, rmsL; long clipL, zcL, silent;
        void print() {
            System.out.printf("    frames=%d meanDC=%.1f peak=%.0f rms=%.1f clip=%d zeroCross/s=%.0f silentFrac=%.1f%%%n",
                nFrames, meanL, peakL, rmsL, clipL,
                zcL * (double) APU.SAMPLE_RATE / Math.max(1, nFrames),
                100.0 * silent / Math.max(1, nFrames));
        }
    }
    static Metrics analyze(short[] inter) {
        Metrics m = new Metrics();
        int frames = inter.length / 2; m.nFrames = frames;
        double sum = 0, sumSq = 0, peak = 0; long clip = 0, zc = 0, sil = 0; int prev = 0;
        for (int i = 0; i < frames; i++) {
            int l = inter[i * 2];
            sum += l; sumSq += (double) l * l;
            if (Math.abs(l) > peak) peak = Math.abs(l);
            if (l >= 32767 || l <= -32768) clip++;
            if (l == 0) sil++;
            int s = Integer.signum(l);
            if (s != 0 && prev != 0 && s != prev) zc++;
            if (s != 0) prev = s;
        }
        m.meanL = frames > 0 ? sum / frames : 0;
        m.rmsL = frames > 0 ? Math.sqrt(sumSq / frames) : 0;
        m.peakL = peak; m.clipL = clip; m.zcL = zc; m.silent = sil;
        return m;
    }
    static void writeWav(File f, short[] inter) throws Exception {
        int dataBytes = inter.length * 2;
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            o.writeBytes("RIFF"); le(o, 36 + dataBytes); o.writeBytes("WAVE");
            o.writeBytes("fmt "); le(o, 16); le16(o, 1); le16(o, 2);
            le(o, APU.SAMPLE_RATE); le(o, APU.SAMPLE_RATE * 4); le16(o, 4); le16(o, 16);
            o.writeBytes("data"); le(o, dataBytes);
            for (short s : inter) { o.writeByte(s & 0xFF); o.writeByte((s >> 8) & 0xFF); }
        }
    }
    static void le(DataOutputStream o, int v) throws IOException {
        o.writeByte(v); o.writeByte(v >> 8); o.writeByte(v >> 16); o.writeByte(v >> 24);
    }
    static void le16(DataOutputStream o, int v) throws IOException { o.writeByte(v); o.writeByte(v >> 8); }
}
