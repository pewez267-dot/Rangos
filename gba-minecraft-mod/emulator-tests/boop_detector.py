#!/usr/bin/env python3
"""Detecta 'boops'/transitorios de estatica en una captura de audio del emulador.
Un boop = rafaga corta de energia en banda alta (5-14 kHz) que destaca sobre el
fondo. Imprime los tiempos y un conteo total para comparar antes/despues de un fix.
"""
import sys, wave, numpy as np

def load(path):
    w=wave.open(path,'rb'); nch=w.getnchannels(); fr=w.getframerate(); n=w.getnframes()
    a=np.frombuffer(w.readframes(n),dtype='<i2').astype(np.float64); w.close()
    if nch==2: a=a.reshape(-1,2).mean(axis=1)
    return fr,a

def detect(path):
    fr,x=load(path)
    # Energia de banda alta por ventanas cortas (~12 ms)
    N=512; hop=256
    win=np.hanning(N)
    nfr=(len(x)-N)//hop
    f=np.fft.rfftfreq(N,1.0/fr)
    hi=(f>=5000)&(f<=14000)
    band=np.zeros(nfr); t=np.zeros(nfr)
    for i in range(nfr):
        seg=x[i*hop:i*hop+N]*win
        sp=np.abs(np.fft.rfft(seg))**2
        band[i]=sp[hi].sum(); t[i]=(i*hop+N/2)/fr
    # Normaliza y busca picos que superen mucho la mediana local
    med=np.median(band)+1e-9
    ratio=band/med
    # Un boop: ratio supera umbral y es un maximo local separado
    thr=8.0
    peaks=[]
    i=1
    while i<nfr-1:
        if ratio[i]>thr and ratio[i]>=ratio[i-1] and ratio[i]>=ratio[i+1]:
            peaks.append((t[i],ratio[i]))
            i+=int(0.15*fr/hop)  # no recontar el mismo evento (150ms)
        else:
            i+=1
    print(f"\n=== {path} ===")
    print(f"  dur={len(x)/fr:.1f}s  energia_alta_mediana={med:.1f}  umbral_ratio={thr}")
    print(f"  BOOPS detectados: {len(peaks)}")
    for tt,r in peaks[:40]:
        print(f"    t={tt:6.2f}s  ratio={r:5.1f}x")
    return len(peaks)

total=0
for p in sys.argv[1:]:
    total+=detect(p)
print(f"\nTOTAL boops: {total}")
