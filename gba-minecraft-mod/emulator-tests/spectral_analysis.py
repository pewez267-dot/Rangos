#!/usr/bin/env python3
# Análisis espectral para localizar el defecto que peak/rms/clip NO ven:
#  - tonos pegados (pitido)  -> picos de banda estrecha persistentes en el tiempo
#  - aliasing / aspereza      -> energía anómala en alta frecuencia
#  - distorsión armónica      -> armónicos por encima del fundamental
import sys, wave, struct
import numpy as np

def load(path):
    w = wave.open(path, 'rb')
    ch, sw, sr, n = w.getnchannels(), w.getsampwidth(), w.getframerate(), w.getnframes()
    raw = w.readframes(n); w.close()
    a = np.frombuffer(raw, dtype='<i2').astype(np.float64)
    if ch == 2:
        a = a.reshape(-1, 2)
        mono = a.mean(axis=1)
    else:
        mono = a
    return mono, sr, ch

def analyze(path):
    print("="*70)
    print(f"ARCHIVO: {path}")
    mono, sr, ch = load(path)
    dur = len(mono)/sr
    print(f"  sr={sr} Hz  ch={ch}  dur={dur:.1f}s  N={len(mono)}")
    # Normalizar para análisis
    peak = np.max(np.abs(mono)) or 1.0
    x = mono/peak

    # STFT manual (frames de 4096, hop 2048, Hann)
    win = 4096; hop = 2048
    w = np.hanning(win)
    frames = []
    for i in range(0, len(x)-win, hop):
        seg = x[i:i+win]*w
        F = np.abs(np.fft.rfft(seg))
        frames.append(F)
    S = np.array(frames)              # [t, f]
    freqs = np.fft.rfftfreq(win, 1/sr)
    Smean = S.mean(axis=0)            # espectro promedio
    Smean_db = 20*np.log10(Smean/ (Smean.max()+1e-9) + 1e-9)

    # 1) Reparto de energía por banda
    def band_energy(lo, hi):
        m = (freqs>=lo)&(freqs<hi)
        return (S[:,m]**2).sum()
    tot = (S**2).sum() + 1e-9
    print("  Reparto de energía por banda:")
    for lo,hi in [(0,500),(500,2000),(2000,5000),(5000,8000),(8000,12000),(12000,16000),(16000,sr/2)]:
        print(f"    {lo:5d}-{hi:5.0f} Hz : {100*band_energy(lo,hi)/tot:5.1f}%")

    # 2) Tonos pegados: bins cuya energía está presente en >70% de los frames
    #    (un tono musical va y viene; un "pitido" defectuoso está SIEMPRE)
    thr = np.percentile(S, 92, axis=0)            # umbral alto por bin
    active = S > (0.5*S.max(axis=0, keepdims=True))   # bin "encendido"
    persist = active.mean(axis=0)                  # fracción de tiempo encendido
    stuck = [(freqs[i], persist[i]) for i in range(len(freqs))
             if persist[i] > 0.85 and freqs[i] > 1500]
    stuck.sort(key=lambda t:-t[1])
    print(f"  Tonos PEGADOS (>85% del tiempo, >1.5kHz): {len(stuck)}")
    for f,p in stuck[:8]:
        print(f"    {f:7.1f} Hz  presente {100*p:.0f}% del tiempo")

    # 3) Energía sobre 14 kHz (aliasing/aspereza audible como pitido agudo)
    hf = 100*band_energy(14000, sr/2)/tot
    print(f"  Energía >14kHz (aspereza/alias): {hf:.2f}%")

    # 4) Discontinuidades grandes muestra-a-muestra (clicks)
    d = np.abs(np.diff(mono))
    jumps = int((d > 0.25*peak).sum())
    print(f"  Saltos muestra-a-muestra >25% FS (clicks): {jumps}")

    # 5) DC
    print(f"  DC offset medio: {mono.mean():.1f}")
    return freqs, Smean

for p in sys.argv[1:]:
    try:
        analyze(p)
    except Exception as e:
        print(f"  ERROR {p}: {e}")
