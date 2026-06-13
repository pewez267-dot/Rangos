#!/usr/bin/env python3
# Mide energia >6.7kHz (imagenes ZOH, ya que la fuente Direct Sound es 13379Hz)
# en regiones de boop vs regiones tranquilas del dma_only.wav.
import sys, wave, numpy as np
path=sys.argv[1]
w=wave.open(path,'rb'); fr=w.getframerate(); nch=w.getnchannels(); n=w.getnframes()
a=np.frombuffer(w.readframes(n),dtype='<i2').astype(np.float64); w.close()
if nch==2: a=a.reshape(-1,2).mean(axis=1)
SRC_NYQ=6689.0
def band_energy(seg):
    seg=seg-seg.mean(); W=np.hanning(len(seg))
    sp=np.abs(np.fft.rfft(seg*W))**2
    f=np.fft.rfftfreq(len(seg),1.0/fr)
    tot=sp[1:].sum()+1e-9
    above=sp[f>=SRC_NYQ].sum()
    b_mid=sp[(f>=2000)&(f<SRC_NYQ)].sum()
    return 100*above/tot, 100*b_mid/tot
def at(tc, label):
    c=int(tc*fr); half=int(0.025*fr)
    pa,pm=band_energy(a[c-half:c+half])
    print(f"  {label:18s} t={tc:6.2f}s  >6.7kHz(imagen)={pa:5.2f}%   2-6.7kHz={pm:5.2f}%")
print(f"=== {path} (fr={fr}) ===")
print("BOOPS:")
for t in [2.90,19.77,23.53,27.23,34.70,38.46,57.15]:
    at(t,"boop")
print("REGIONES TRANQUILAS:")
for t in [8.0,12.0,16.0,30.0,48.0]:
    at(t,"quieto")
# Energia global >6.7kHz
seg=a-a.mean(); 
N=8192; nseg=len(seg)//N; acc=np.zeros(N//2+1); win=np.hanning(N)
for i in range(nseg): acc+=np.abs(np.fft.rfft(seg[i*N:(i+1)*N]*win))**2
f=np.fft.rfftfreq(N,1.0/fr); tot=acc[1:].sum()
print(f"GLOBAL >6.7kHz = {100*acc[f>=SRC_NYQ].sum()/tot:.2f}%")
