package com.gbaminecraft.emulator.apu;

import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * GBA Audio Processing Unit (APU).
 * Implements 4 DMG channels (Tone1, Tone2, Wave, Noise) + 2 FIFO DMA channels (A, B).
 * Outputs 16-bit stereo samples at 32768 Hz.
 */
public class APU {

    public static final int SAMPLE_RATE     = 32768;
    public static final int BUFFER_SIZE     = 2048; // stereo samples
    public static final int CPU_FREQ        = 16777216;

    // ─ Registers ───────────────────────────────────────────────────────────
    private int SOUND1CNT_L, SOUND1CNT_H, SOUND1CNT_X;
    private int SOUND2CNT_L, SOUND2CNT_H;
    private int SOUND3CNT_L, SOUND3CNT_H, SOUND3CNT_X;
    private int SOUND4CNT_L, SOUND4CNT_H;
    private int SOUNDCNT_L, SOUNDCNT_H, SOUNDBIAS;

    // Wave RAM (32 bytes = 64 nibbles)
    private byte[] waveRAM = new byte[32];

    // FIFO buffers A and B
    private final int[] fifoA = new int[32];
    private final int[] fifoB = new int[32];
    private int fifoAHead = 0, fifoATail = 0, fifoASize = 0;
    private int fifoBHead = 0, fifoBTail = 0, fifoBSize = 0;

    // Output buffer (stereo interleaved)
    private final short[] audioBuffer = new short[BUFFER_SIZE * 2];
    private int audioBufferPos = 0;
    private volatile boolean bufferReady = false;

    // Channel state
    private int ch1Freq = 0, ch1DutyPos = 0, ch1Volume = 0, ch1EnvTimer = 0, ch1EnvDir = 0, ch1EnvPeriod = 0;
    private int ch1SweepTimer = 0, ch1SweepPeriod = 0, ch1SweepDir = 0, ch1SweepShift = 0;
    private boolean ch1Running = false;
    private int ch1LenTimer = 0; boolean ch1LenEnabled = false;

    private int ch2Freq = 0, ch2DutyPos = 0, ch2Volume = 0, ch2EnvTimer = 0, ch2EnvDir = 0, ch2EnvPeriod = 0;
    private boolean ch2Running = false;
    private int ch2LenTimer = 0; boolean ch2LenEnabled = false;

    private int ch3Freq = 0, ch3Pos = 0, ch3Volume = 0;
    private boolean ch3Running = false, ch3On = false;
    private int ch3LenTimer = 0; boolean ch3LenEnabled = false;

    private int ch4Shift = 0, ch4Div = 0, ch4LFSR = 0, ch4Width = 0, ch4Volume = 0;
    private boolean ch4Running = false;
    private int ch4LenTimer = 0; boolean ch4LenEnabled = false;

    // Cycle accumulators (integer: CYCLES_PER_SAMPLE is exactly 512, no need for
    // floating point in the per-instruction hot path).
    private int ch1CycAcc = 0, ch2CycAcc = 0, ch3CycAcc = 0, ch4CycAcc = 0;
    private int sampleCycAcc = 0;
    private static final int CYCLES_PER_SAMPLE = CPU_FREQ / SAMPLE_RATE; // = 512 exactly

    // Frame sequencer (512 Hz from timer)
    private int frameSeqTimer = 0;
    private int frameSeqStep  = 0;
    private static final int FRAME_SEQ_CYCLES = CPU_FREQ / 512;

    // DMA channel current sample
    private byte dmaASample = 0, dmaBSample = 0;

    private MemoryBus bus;
    private boolean enabled = true;

    public APU(MemoryBus bus) {
        this.bus = bus;
        ch4LFSR = 0x7FFF;
    }

    public void tick(int cycles) {
        if (!enabled) return;

        frameSeqTimer += cycles;
        while (frameSeqTimer >= FRAME_SEQ_CYCLES) {
            frameSeqTimer -= FRAME_SEQ_CYCLES;
            stepFrameSequencer();
        }

        sampleCycAcc += cycles;
        while (sampleCycAcc >= CYCLES_PER_SAMPLE) {
            sampleCycAcc -= CYCLES_PER_SAMPLE;
            generateSample();
        }
    }

    private void stepFrameSequencer() {
        switch (frameSeqStep & 7) {
            case 0: case 4: stepLength(); break;
            case 2: case 6: stepLength(); stepSweep(); break;
            case 7: stepEnvelope(); break;
        }
        frameSeqStep++;
    }

    private void stepLength() {
        if (ch1LenEnabled && ch1LenTimer > 0 && --ch1LenTimer == 0) ch1Running = false;
        if (ch2LenEnabled && ch2LenTimer > 0 && --ch2LenTimer == 0) ch2Running = false;
        if (ch3LenEnabled && ch3LenTimer > 0 && --ch3LenTimer == 0) ch3Running = false;
        if (ch4LenEnabled && ch4LenTimer > 0 && --ch4LenTimer == 0) ch4Running = false;
    }

    private void stepEnvelope() {
        if (ch1EnvPeriod > 0) {
            if (++ch1EnvTimer >= ch1EnvPeriod) {
                ch1EnvTimer = 0;
                if (ch1EnvDir == 1 && ch1Volume < 15) ch1Volume++;
                else if (ch1EnvDir == 0 && ch1Volume > 0) ch1Volume--;
            }
        }
        if (ch2EnvPeriod > 0) {
            if (++ch2EnvTimer >= ch2EnvPeriod) {
                ch2EnvTimer = 0;
                if (ch2EnvDir == 1 && ch2Volume < 15) ch2Volume++;
                else if (ch2EnvDir == 0 && ch2Volume > 0) ch2Volume--;
            }
        }
        if (ch4EnvPeriod > 0) {
            if (++ch4EnvTimer >= ch4EnvPeriod) {
                ch4EnvTimer = 0;
                if (ch4EnvDir == 1 && ch4Volume < 15) ch4Volume++;
                else if (ch4EnvDir == 0 && ch4Volume > 0) ch4Volume--;
            }
        }
    }
    private int ch4EnvTimer = 0, ch4EnvDir = 0, ch4EnvPeriod = 0;

    private void stepSweep() {
        if (ch1SweepPeriod > 0) {
            if (++ch1SweepTimer >= ch1SweepPeriod) {
                ch1SweepTimer = 0;
                int delta = ch1Freq >> ch1SweepShift;
                ch1Freq += ch1SweepDir == 0 ? delta : -delta;
                if (ch1Freq > 2047) { ch1Running = false; ch1Freq = 2047; }
                if (ch1Freq < 0)   ch1Freq = 0;
            }
        }
    }

    private static final int[] DUTY_TABLE = {0b00000001, 0b10000001, 0b10000111, 0b01111110};

    private void generateSample() {
        int psgL = 0, psgR = 0;   // PSG channels (each 0..15), scaled by master volume
        int dmaL = 0, dmaR = 0;   // Direct Sound FIFO channels (signed)

        // CH1 Square
        if (ch1Running) {
            int period = (2048 - ch1Freq) * 4;
            ch1CycAcc += CYCLES_PER_SAMPLE;
            while (ch1CycAcc >= period) { ch1CycAcc -= period; ch1DutyPos = (ch1DutyPos + 1) & 7; }
            int duty = (SOUND1CNT_H >>> 6) & 3;
            int s = ((DUTY_TABLE[duty] >>> (7 - ch1DutyPos)) & 1) * ch1Volume;
            if ((SOUNDCNT_L & (1 << 8)) != 0) psgL += s;
            if ((SOUNDCNT_L & (1 << 12)) != 0) psgR += s;
        }

        // CH2 Square
        if (ch2Running) {
            int period = (2048 - ch2Freq) * 4;
            ch2CycAcc += CYCLES_PER_SAMPLE;
            while (ch2CycAcc >= period) { ch2CycAcc -= period; ch2DutyPos = (ch2DutyPos + 1) & 7; }
            int duty = (SOUND2CNT_L >>> 6) & 3;
            int s = ((DUTY_TABLE[duty] >>> (7 - ch2DutyPos)) & 1) * ch2Volume;
            if ((SOUNDCNT_L & (1 << 9)) != 0) psgL += s;
            if ((SOUNDCNT_L & (1 << 13)) != 0) psgR += s;
        }

        // CH3 Wave
        if (ch3Running && ch3On) {
            int period = (2048 - ch3Freq) * 2;
            ch3CycAcc += CYCLES_PER_SAMPLE;
            while (ch3CycAcc >= period) { ch3CycAcc -= period; ch3Pos = (ch3Pos + 1) & 63; }
            int nibble = (waveRAM[ch3Pos >> 1] >> ((ch3Pos & 1) == 0 ? 4 : 0)) & 0xF;
            int vol = (SOUND3CNT_H >>> 13) & 0x3;
            int shift = vol == 0 ? 4 : vol - 1;
            int s = nibble >> shift;
            if ((SOUNDCNT_L & (1 << 10)) != 0) psgL += s;
            if ((SOUNDCNT_L & (1 << 14)) != 0) psgR += s;
        }

        // CH4 Noise
        if (ch4Running) {
            int freq = ch4Div == 0 ? 8 : (ch4Div * 16);
            freq <<= ch4Shift;
            ch4CycAcc += CYCLES_PER_SAMPLE;
            while (ch4CycAcc >= freq && freq > 0) {
                ch4CycAcc -= freq;
                int xor = (ch4LFSR ^ (ch4LFSR >> 1)) & 1;
                ch4LFSR = (ch4LFSR >> 1) | (xor << 14);
                if (ch4Width != 0) ch4LFSR = (ch4LFSR & ~(1 << 6)) | (xor << 6);
            }
            int s = (~ch4LFSR & 1) * ch4Volume;
            if ((SOUNDCNT_L & (1 << 11)) != 0) psgL += s;
            if ((SOUNDCNT_L & (1 << 15)) != 0) psgR += s;
        }

        // Direct Sound DMA channels A/B (signed 8-bit samples). Their volume is
        // independent of the PSG master volume (SOUNDCNT_H bit2 = A, bit3 = B;
        // 0 = 50%, 1 = 100%). Pokémon plays most of its music/SFX through these.
        int daA = (((SOUNDCNT_H >> 2) & 1) != 0) ? dmaASample : (dmaASample >> 1);
        int daB = (((SOUNDCNT_H >> 3) & 1) != 0) ? dmaBSample : (dmaBSample >> 1);
        if ((SOUNDCNT_H & (1 << 8))  != 0) dmaL += daA;  // A -> left
        if ((SOUNDCNT_H & (1 << 9))  != 0) dmaR += daA;  // A -> right
        if ((SOUNDCNT_H & (1 << 12)) != 0) dmaL += daB;  // B -> left
        if ((SOUNDCNT_H & (1 << 13)) != 0) dmaR += daB;  // B -> right

        // Master PSG volume (SOUNDCNT_L: bits 4-6 left, 0-2 right), 0..7 -> 1..8.
        psgL *= ((SOUNDCNT_L >>> 4) & 0x7) + 1;   // 0..480
        psgR *= (SOUNDCNT_L & 0x7) + 1;           // 0..480

        // Final mix. Direct Sound (DMA A/B) carries the music and most SFX, so it
        // gets the larger share of the 16-bit range; the gains keep typical
        // content well clear of clipping while making the output actually audible
        // (the previous gain of 64 left music around -17 dB — nearly silent).
        int left  = psgL * 20 + dmaL * 110;
        int right = psgR * 20 + dmaR * 110;
        left  = Math.max(-32768, Math.min(32767, left));
        right = Math.max(-32768, Math.min(32767, right));

        // Ring-buffer store. Wrap the write head WITHOUT dropping a sample (the
        // old code returned early on wrap, losing one stereo frame every 2048 —
        // a faint 16 Hz discontinuity train). drainInto() tracks drainPos so the
        // wrap is handled losslessly downstream.
        if (audioBufferPos >= BUFFER_SIZE * 2) { audioBufferPos = 0; bufferReady = true; }
        audioBuffer[audioBufferPos++] = (short) left;
        audioBuffer[audioBufferPos++] = (short) right;
    }

    public void pushFifoA(byte[] data) {
        for (byte b : data) {
            if (fifoASize < 32) { fifoA[fifoATail] = b; fifoATail = (fifoATail + 1) % 32; fifoASize++; }
        }
    }

    public void pushFifoB(byte[] data) {
        for (byte b : data) {
            if (fifoBSize < 32) { fifoB[fifoBTail] = b; fifoBTail = (fifoBTail + 1) % 32; fifoBSize++; }
        }
    }

    public void popFifoA() {
        if (fifoASize > 0) {
            dmaASample = (byte) fifoA[fifoAHead];
            fifoAHead = (fifoAHead + 1) % 32;
            fifoASize--;
        }
    }

    public void popFifoB() {
        if (fifoBSize > 0) {
            dmaBSample = (byte) fifoB[fifoBHead];
            fifoBHead = (fifoBHead + 1) % 32;
            fifoBSize--;
        }
    }

    public short[] getAudioBuffer()  { return audioBuffer; }
    public boolean isBufferReady()   { return bufferReady; }
    public void    clearBufferReady(){ bufferReady = false; }

    /** Current fill level (in bytes/samples) of each Direct Sound FIFO. The DMA
     *  controller uses these to top up a FIFO only when it has drained to half,
     *  matching the hardware. */
    public int fifoASize() { return fifoASize; }
    public int fifoBSize() { return fifoBSize; }

    // ── Per-frame audio drain (for real-time output) ──────────────────────
    // The audio output drains the samples produced since the previous call once
    // per video frame. We track how far it has consumed (drainPos) so we can
    // hand over only the new samples regardless of where audioBufferPos is.
    private int drainPos = 0;
    private final short[] drainTmp = new short[BUFFER_SIZE * 2];

    /**
     * Copies the stereo samples generated since the last call into {@code out}
     * and returns the number of individual shorts copied (sampleCount*2). The
     * audio output writes these straight to the sound line.
     */
    public synchronized int drainInto(short[] out) {
        int pos = audioBufferPos;          // current write head (may have wrapped)
        int count;
        if (pos >= drainPos) {
            count = pos - drainPos;
            if (count > out.length) count = out.length;
            System.arraycopy(audioBuffer, drainPos, out, 0, count);
        } else {
            // Wrapped: take [drainPos..end) then [0..pos)
            int tail = (BUFFER_SIZE * 2) - drainPos;
            int head = pos;
            count = tail + head;
            if (count > out.length) count = out.length;
            int n1 = Math.min(tail, count);
            System.arraycopy(audioBuffer, drainPos, out, 0, n1);
            int n2 = count - n1;
            if (n2 > 0) System.arraycopy(audioBuffer, 0, out, n1, n2);
        }
        drainPos = pos;
        return count;
    }

    public int readRegister(int offset) {
        switch (offset) {
            case 0x60: return SOUND1CNT_L & 0xFF;
            case 0x62: return SOUND1CNT_H & 0xFF;
            case 0x63: return (SOUND1CNT_H >>> 8) & 0xFF;
            case 0x68: return SOUND2CNT_L & 0xFF;
            case 0x69: return (SOUND2CNT_L >>> 8) & 0xFF;
            case 0x70: return SOUND3CNT_L & 0xFF;
            case 0x72: return SOUND3CNT_H & 0xFF;
            case 0x73: return (SOUND3CNT_H >>> 8) & 0xFF;
            case 0x78: return SOUND4CNT_L & 0xFF;
            case 0x7C: return SOUND4CNT_H & 0xFF;
            case 0x80: return SOUNDCNT_L & 0xFF;
            case 0x81: return (SOUNDCNT_L >>> 8) & 0xFF;
            case 0x82: return SOUNDCNT_H & 0xFF;
            case 0x83: return (SOUNDCNT_H >>> 8) & 0xFF;
            case 0x84: return (enabled ? (1 << 7) : 0) | (ch1Running ? 1 : 0) | (ch2Running ? 2 : 0) | (ch3Running ? 4 : 0) | (ch4Running ? 8 : 0);
            case 0x88: return SOUNDBIAS & 0xFF;
            case 0x89: return (SOUNDBIAS >>> 8) & 0xFF;
            default:
                if (offset >= 0x90 && offset < 0xA0) return waveRAM[offset - 0x90] & 0xFF;
                return 0;
        }
    }

    public void writeRegister(int offset, int val) {
        switch (offset) {
            case 0x60: SOUND1CNT_L = (SOUND1CNT_L & 0xFF00) | val;
                ch1SweepPeriod = (val >>> 4) & 0x7; ch1SweepDir = (val >>> 3) & 1; ch1SweepShift = val & 0x7; break;
            case 0x62: SOUND1CNT_H = (SOUND1CNT_H & 0xFF00) | val; break;
            case 0x63: SOUND1CNT_H = (SOUND1CNT_H & 0x00FF) | (val << 8); break;
            case 0x64: { // NR14
                ch1Freq = (ch1Freq & 0xFF) | ((val & 0x7) << 8);
                ch1LenEnabled = (val & (1 << 6)) != 0;
                if ((val & (1 << 7)) != 0) triggerCH1();
                break;
            }
            case 0x68: SOUND2CNT_L = (SOUND2CNT_L & 0xFF00) | val; break;
            case 0x69: SOUND2CNT_L = (SOUND2CNT_L & 0x00FF) | (val << 8); break;
            case 0x6C: {
                ch2Freq = (ch2Freq & 0xFF) | ((val & 0x7) << 8);
                ch2LenEnabled = (val & (1 << 6)) != 0;
                if ((val & (1 << 7)) != 0) triggerCH2();
                break;
            }
            case 0x70: ch3On = (val & (1 << 7)) != 0; SOUND3CNT_L = val; break;
            case 0x72: SOUND3CNT_H = (SOUND3CNT_H & 0xFF00) | val; break;
            case 0x73: SOUND3CNT_H = (SOUND3CNT_H & 0x00FF) | (val << 8);
                ch3Volume = (val >>> 5) & 0x3; break;
            case 0x74: {
                ch3Freq = (ch3Freq & 0xFF) | ((val & 0x7) << 8);
                ch3LenEnabled = (val & (1 << 6)) != 0;
                if ((val & (1 << 7)) != 0) triggerCH3();
                break;
            }
            case 0x78: SOUND4CNT_L = (SOUND4CNT_L & 0xFF00) | val; break;
            case 0x79: {
                SOUND4CNT_L = (SOUND4CNT_L & 0x00FF) | (val << 8);
                ch4Volume = (val >>> 4) & 0xF;
                ch4EnvDir = (val >>> 3) & 1;
                ch4EnvPeriod = val & 0x7;
                break;
            }
            case 0x7C: SOUND4CNT_H = (SOUND4CNT_H & 0xFF00) | val;
                ch4Shift = (val >>> 4) & 0xF; ch4Width = (val >>> 3) & 1; ch4Div = val & 0x7; break;
            case 0x7D: ch4LenEnabled = (val & (1 << 6)) != 0; if ((val & (1 << 7)) != 0) triggerCH4(); break;
            case 0x80: SOUNDCNT_L = (SOUNDCNT_L & 0xFF00) | val; break;
            case 0x81: SOUNDCNT_L = (SOUNDCNT_L & 0x00FF) | (val << 8); break;
            case 0x82: SOUNDCNT_H = (SOUNDCNT_H & 0xFF00) | val; break;
            case 0x83: {
                SOUNDCNT_H = (SOUNDCNT_H & 0x00FF) | (val << 8);
                // FBA 13g — Direct Sound FIFO reset, mirrors mGBA's
                // GBAAudioWriteSOUNDCNT_HI: writing 1 to the ChA reset (bit 11)
                // or ChB reset (bit 15) strobe clears that FIFO. In the high
                // byte written here, bit 11 = bit 3 and bit 15 = bit 7.
                // Games strobe these to flush queued PCM (e.g. when a dialog
                // box advances and the next sound starts); without honoring it
                // the leftover FIFO bytes replayed as a short "boop". Like mGBA
                // we clear ONLY the FIFO pointers — not the current output
                // sample (dmaXSample) and not the volume.
                if ((val & (1 << 3)) != 0) { fifoAHead = 0; fifoATail = 0; fifoASize = 0; }
                if ((val & (1 << 7)) != 0) { fifoBHead = 0; fifoBTail = 0; fifoBSize = 0; }
                break;
            }
            case 0x84: enabled = (val & (1 << 7)) != 0; if (!enabled) { ch1Running=ch2Running=ch3Running=ch4Running=false; } break;
            case 0x88: SOUNDBIAS = (SOUNDBIAS & 0xFF00) | val; break;
            case 0x89: SOUNDBIAS = (SOUNDBIAS & 0x00FF) | (val << 8); break;
            default:
                if (offset >= 0x90 && offset < 0xA0) waveRAM[offset - 0x90] = (byte) val;
                break;
        }
    }

    // ─ Channel triggers ───────────────────────────────────────────────────
    private void triggerCH1() {
        ch1Running = true;
        if (ch1LenTimer == 0) ch1LenTimer = 64;
        ch1Volume = (SOUND1CNT_H >>> 12) & 0xF;
        ch1EnvDir = (SOUND1CNT_H >>> 11) & 1;
        ch1EnvPeriod = (SOUND1CNT_H >>> 8) & 0x7;
        ch1EnvTimer = 0;
        ch1Freq = SOUND1CNT_H & 0x7FF;
        ch1SweepTimer = 0;
    }

    private void triggerCH2() {
        ch2Running = true;
        if (ch2LenTimer == 0) ch2LenTimer = 64;
        ch2Volume = (SOUND2CNT_L >>> 12) & 0xF;
        ch2EnvDir = (SOUND2CNT_L >>> 11) & 1;
        ch2EnvPeriod = (SOUND2CNT_L >>> 8) & 0x7;
        ch2EnvTimer = 0;
        ch2Freq = SOUND2CNT_H & 0x7FF;
    }

    private void triggerCH3() {
        ch3Running = ch3On;
        if (ch3LenTimer == 0) ch3LenTimer = 256;
        ch3Pos = 0;
        ch3Freq = SOUND3CNT_H & 0x7FF;
    }

    private void triggerCH4() {
        ch4Running = true;
        if (ch4LenTimer == 0) ch4LenTimer = 64;
        ch4LFSR = 0x7FFF;
        ch4EnvTimer = 0;
    }

    public void reset() {
        SOUND1CNT_L = SOUND1CNT_H = SOUND1CNT_X = 0;
        SOUND2CNT_L = SOUND2CNT_H = 0;
        SOUND3CNT_L = SOUND3CNT_H = SOUND3CNT_X = 0;
        SOUND4CNT_L = SOUND4CNT_H = 0;
        SOUNDCNT_L = SOUNDCNT_H = SOUNDBIAS = 0;
        ch1Running = ch2Running = ch3Running = ch4Running = false;
        java.util.Arrays.fill(waveRAM, (byte)0);
        audioBufferPos = 0;
        bufferReady = false;
    }
}
