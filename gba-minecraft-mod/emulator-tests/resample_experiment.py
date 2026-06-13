#!/usr/bin/env python3
"""Mide la calidad de distintos caminos de resampleo 32768 -> 48000 Hz.
Objetivo: comprobar si el resampleo (el que hace el SO sobre nuestro stream de
32768, o nuestro resampler lineal) introduce distorsion audible, y cual es el
mejor metodo.

1) Test sintetico: barrido + tonos a 32768 Hz, resampleado por:
   - ZOH (nearest)  -> simula un resampler barato del SO
   - lineal         -> nuestro resampler actual
   - polyphase sinc -> resampler de calidad (lo que hace mGBA)
   Mide energia de aliasing/imagenes (basura no presente en el original).

2) Aplica los 3 metodos a la musica real capturada y guarda WAVs A/B.
"""
import numpy as np, wave, sys

SRC=32768; DST=48000

def write_wav(path, x, rate):
    x=np.clip(x,-32768,32767).astype('<i2')
    w=wave.open(path,'wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate)
    w.writeframes(x.tobytes()); w.close()

def read_wav(path):
    w=wave.open(path,'rb'); nch=w.getnchannels(); fr=w.getframerate(); n=w.getnframes()
    a=np.frombuffer(w.readframes(n),dtype='<i2').astype(np.float64); w.close()
    if nch==2: a=a.reshape(-1,2)[:,0]
    return fr,a

def zoh(x,src,dst):
    n=int(len(x)*dst/src); idx=np.floor(np.arange(n)*src/dst).astype(int); idx=np.clip(idx,0,len(x)-1)
    return x[idx]

def linear(x,src,dst):
    n=int(len(x)*dst/src); pos=np.arange(n)*src/dst; i=np.floor(pos).astype(int); f=pos-i
    i=np.clip(i,0,len(x)-2); return x[i]*(1-f)+x[i+1]*f

def sinc_poly(x,src,dst):
    # resampleo band-limited via FFT (alta calidad, referencia)
    n=int(round(len(x)*dst/src))
    return np.real(np.fft.irfft(np.fft.rfft(x), n=n)) * (n/len(x))

def aliasing_metric(orig_rate, orig, res_rate, res, label):
    # Compara el espectro: cuanta energia aparece en bandas que el original
    # (band-limited a SRC/2=16384) no tenia. Para senales con contenido solo
    # < ~6 kHz, cualquier energia nueva en altas es aliasing/imagen del resampler.
    def spec(x,rate):
        N=16384
        if len(x)<N: N=1<<int(np.log2(len(x)))
        nseg=len(x)//N; w=np.hanning(N); acc=np.zeros(N//2+1)
        for k in range(nseg): acc+=np.abs(np.fft.rfft((x[k*N:(k+1)*N]-x[k*N:(k+1)*N].mean())*w))**2
        acc/=max(nseg,1); fr=np.fft.rfftfreq(N,1.0/rate); return fr,acc
    f,a=spec(res,res_rate); tot=a[1:].sum()
    # banda de imagenes: por encima de 6.7 kHz (Nyquist real de la fuente DS)
    hi=a[f>=6689].sum()
    print(f"  {label:14s}: energia >6.7kHz (basura)= {100*hi/tot:5.2f}%   pico={f[1:][np.argmax(a[1:])]:.0f}Hz")

print("=== TEST SINTETICO (tonos limpios a 32768, contenido solo < 6 kHz) ===")
t=np.arange(SRC*4)/SRC
# tonos representativos de musica GBA, todos < 6 kHz (banda valida del Direct Sound)
sig = 8000*np.sin(2*np.pi*440*t) + 4000*np.sin(2*np.pi*1200*t) + 3000*np.sin(2*np.pi*3500*t)
sig += 2000*np.sin(2*np.pi*(300+1500*t)*t)  # barrido suave 300->~6k
sig=sig.astype(np.float64)
f0,a0=None,None
aliasing_metric(SRC,sig,SRC,sig,"original@32768")
aliasing_metric(SRC,sig,DST,zoh(sig,SRC,DST),"ZOH(SO barato)")
aliasing_metric(SRC,sig,DST,linear(sig,SRC,DST),"lineal(actual)")
aliasing_metric(SRC,sig,DST,sinc_poly(sig,SRC,DST),"sinc(calidad)")

print("\n=== MUSICA REAL: " + (sys.argv[1] if len(sys.argv)>1 else "samples/gameplay_capture_60s.wav") + " ===")
path=sys.argv[1] if len(sys.argv)>1 else "samples/gameplay_capture_60s.wav"
fr,music=read_wav(path)
aliasing_metric(fr,music,fr,music,"original@32768")
aliasing_metric(fr,music,DST,zoh(music,fr,DST),"ZOH(SO barato)")
aliasing_metric(fr,music,DST,linear(music,fr,DST),"lineal(actual)")
aliasing_metric(fr,music,DST,sinc_poly(music,fr,DST),"sinc(calidad)")
print("\n(ZOH simula lo que hace un resampler barato del SO sobre nuestro 32768.)")
