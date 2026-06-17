#!/usr/bin/env python3
# Mide el aliasing/imaging (la "estatica") que mete cada metodo de resampleo
# 32768 -> 48000 Hz. El audio nativo del GBA no tiene contenido > 16384 Hz, asi
# que TODA la energia en 16.5..24 kHz despues de resamplear es artefacto.
import wave, struct, numpy as np

SRC_RATE = 32768
DST_RATE = 48000

def load_wav_mono(path):
    w = wave.open(path, 'rb')
    n = w.getnframes(); ch = w.getnchannels(); sr = w.getframerate()
    raw = w.readframes(n); w.close()
    a = np.frombuffer(raw, dtype='<i2').astype(np.float64)
    if ch == 2:
        a = a.reshape(-1, 2).mean(axis=1)
    return a, sr

def resample_linear(x, src, dst):
    step = src / dst
    n_out = int(len(x) / step) - 2
    pos = np.arange(n_out) * step
    idx = pos.astype(np.int64)
    frac = pos - idx
    return x[idx] + (x[idx+1] - x[idx]) * frac          # == el codigo Java actual

def resample_cosine(x, src, dst):
    step = src / dst
    n_out = int(len(x) / step) - 2
    pos = np.arange(n_out) * step
    idx = pos.astype(np.int64)
    frac = pos - idx
    mu = (1 - np.cos(frac * np.pi)) * 0.5               # interpolacion coseno
    return x[idx] * (1 - mu) + x[idx+1] * mu

def resample_sinc(x, src, dst, half=16):
    # windowed-sinc vectorizado (referencia de alta calidad, ~ lo que hace mGBA)
    step = src / dst
    n_out = int(len(x) / step) - half - 2
    pos = np.arange(n_out) * step
    c = np.floor(pos).astype(np.int64)
    taps = np.arange(-half + 1, half + 1)               # (2*half,)
    k = c[:, None] + taps[None, :]                       # (n_out, 2*half)
    t = pos[:, None] - k
    win = np.where(np.abs(t) < half, 0.5 + 0.5*np.cos(np.pi*t/half), 0.0)
    h = np.sinc(t) * win
    k = np.clip(k, 0, len(x) - 1)
    return np.sum(x[k] * h, axis=1)

def hf_ratio(sig, rate, lo=16500, hi=23900):
    sig = sig - np.mean(sig)
    N = 1 << int(np.floor(np.log2(len(sig))))
    sig = sig[:N] * np.hanning(N)
    spec = np.abs(np.fft.rfind(sig)) if False else np.abs(np.fft.rfft(sig))
    freqs = np.fft.rfftfreq(N, 1.0/rate)
    p = spec**2
    total = p.sum() + 1e-9
    band = p[(freqs >= lo) & (freqs <= hi)].sum()
    return band / total * 100.0

def make_polyphase(half, phases):
    # tabla [phases][2*half] de sinc*Hann; fase = posicion fraccional cuantizada
    taps = np.arange(-half + 1, half + 1)
    tbl = np.zeros((phases, 2*half))
    for p in range(phases):
        frac = p / phases
        t = frac - taps
        win = np.where(np.abs(t) < half, 0.5 + 0.5*np.cos(np.pi*t/half), 0.0)
        h = np.sinc(t) * win
        s = h.sum()
        tbl[p] = h / (s if s != 0 else 1.0)   # normaliza ganancia DC = 1
    return taps, tbl

def resample_polyphase(x, src, dst, half, phases):
    taps, tbl = make_polyphase(half, phases)
    step = src / dst
    n_out = int(len(x) / step) - half - 2
    pos = np.arange(n_out) * step
    c = np.floor(pos).astype(np.int64)
    ph = ((pos - c) * phases).astype(np.int64) % phases
    k = c[:, None] + taps[None, :]
    k = np.clip(k, 0, len(x) - 1)
    return np.sum(x[k] * tbl[ph], axis=1)

x, sr = load_wav_mono('.audio/rom_capture.wav')
x = x[:98304]  # ~3 s a 32768 Hz, suficiente para el espectro
print(f"fuente: {len(x)} muestras @ {sr} Hz  (rms={np.sqrt(np.mean(x**2)):.1f})")
print(f"energia fuente en 16.5-24kHz (debe ser ~0): {hf_ratio(x, SRC_RATE):.4f}%")
print("-- resampleado a 48000 Hz, energia-fantasma (imaging) en 16.5-24 kHz --")
print(f"  {'LINEAL (actual)':32s}: {hf_ratio(resample_linear(x,SRC_RATE,DST_RATE), DST_RATE):.4f}%")
for half in (4, 6, 8, 12, 16):
    for phases in (256,):
        y = resample_polyphase(x, SRC_RATE, DST_RATE, half, phases)
        print(f"  polyphase sinc half={half:2d} phases={phases}: {hf_ratio(y, DST_RATE):.4f}%   rms={np.sqrt(np.mean(y**2)):.1f}")
import sys; sys.exit(0)

