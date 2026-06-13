#!/usr/bin/env python3
"""Analisis medido del audio capturado del emulador.
Detecta: clipping, DC offset, nivel RMS/pico, y contenido de aliasing
(energia por encima de la Nyquist de la fuente Direct Sound ~6.69 kHz).
"""
import sys, wave, struct
import numpy as np

def load(path):
    w = wave.open(path, 'rb')
    nch = w.getnchannels(); sw = w.getsampwidth(); fr = w.getframerate(); n = w.getnframes()
    raw = w.readframes(n); w.close()
    assert sw == 2, "se esperaban 16-bit"
    a = np.frombuffer(raw, dtype='<i2').astype(np.float64)
    if nch == 2:
        a = a.reshape(-1, 2)
        L, R = a[:,0], a[:,1]
    else:
        L = R = a
    return fr, L, R, nch

def analyze(path):
    fr, L, R, nch = load(path)
    dur = len(L)/fr
    print(f"\n===== {path} =====")
    print(f"rate={fr} Hz  canales={nch}  duracion={dur:.2f}s  frames={len(L)}")
    for name, ch in (("L", L), ("R", R)):
        peak = np.max(np.abs(ch))
        rms = np.sqrt(np.mean(ch**2))
        dc = np.mean(ch)
        clip = np.sum((ch >= 32767) | (ch <= -32768))
        # contar transiciones cerca del techo (clipping suave)
        near = np.sum(np.abs(ch) >= 32000)
        peak_dbfs = 20*np.log10(peak/32768) if peak>0 else -999
        rms_dbfs = 20*np.log10(rms/32768) if rms>0 else -999
        print(f"  [{name}] pico={peak:.0f} ({peak_dbfs:+.1f} dBFS)  rms={rms:.0f} ({rms_dbfs:+.1f} dBFS)"
              f"  DC={dc:+.1f}  clip(==tope)={clip}  |x|>=32000={near}")
    # Espectro promedio (Welch simple) sobre el canal L
    x = L - np.mean(L)
    N = 8192
    if len(x) >= N:
        nseg = len(x)//N
        acc = np.zeros(N//2+1)
        win = np.hanning(N)
        for i in range(nseg):
            seg = x[i*N:(i+1)*N]*win
            acc += np.abs(np.fft.rfft(seg))**2
        acc /= nseg
        freqs = np.fft.rfftfreq(N, 1.0/fr)
        psd = acc / np.max(acc)
        # bandas
        def bandpow(f0, f1):
            m = (freqs>=f0)&(freqs<f1)
            return np.sum(acc[m])
        total = np.sum(acc[1:])
        src_nyq = 13379/2  # 6689 Hz: Nyquist de la fuente Direct Sound de Pokemon
        below = bandpow(0, src_nyq)
        above = bandpow(src_nyq, fr/2)
        print(f"  Energia < {src_nyq:.0f}Hz (musica real): {100*below/total:.1f}%")
        print(f"  Energia > {src_nyq:.0f}Hz (aliasing/imagenes ZOH): {100*above/total:.1f}%")
        print(f"  Bandas: 0-1k={100*bandpow(0,1000)/total:.1f}%  1-4k={100*bandpow(1000,4000)/total:.1f}%"
              f"  4-6.7k={100*bandpow(4000,6689)/total:.1f}%  6.7-10k={100*bandpow(6689,10000)/total:.1f}%"
              f"  10k+={100*bandpow(10000,fr/2)/total:.1f}%")
        # pico espectral dominante
        peakbin = np.argmax(acc[1:])+1
        print(f"  Pico espectral dominante: {freqs[peakbin]:.0f} Hz")

for p in sys.argv[1:]:
    analyze(p)
