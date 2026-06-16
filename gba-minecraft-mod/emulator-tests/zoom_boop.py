#!/usr/bin/env python3
import sys, wave, numpy as np
path=sys.argv[1]; tc=float(sys.argv[2])  # tiempo central del boop
w=wave.open(path,'rb'); fr=w.getframerate(); nch=w.getnchannels(); n=w.getnframes()
a=np.frombuffer(w.readframes(n),dtype='<i2').astype(np.float64); w.close()
if nch==2: a=a.reshape(-1,2).mean(axis=1)
c=int(tc*fr); half=int(0.06*fr)  # +-60ms
seg=a[c-half:c+half]
print(f"Ventana +-60ms en t={tc}s ({len(seg)} muestras @ {fr}Hz)")
# envolvente de energia en ventanas de 2ms
W=int(0.002*fr)
env=[np.sqrt(np.mean(seg[i:i+W]**2)) for i in range(0,len(seg)-W,W)]
env=np.array(env)
print("Envolvente RMS (cada 2ms), '#'=nivel:")
mx=env.max()+1e-9
for i,e in enumerate(env):
    t=(c-half + i*W)/fr
    bars='#'*int(40*e/mx)
    print(f"  t={t:7.3f}s rms={e:7.0f} {bars}")
# espectro del nucleo del boop (20ms centrados)
core=a[c-int(0.01*fr):c+int(0.01*fr)]
core=core-core.mean()
sp=np.abs(np.fft.rfft(core*np.hanning(len(core))))
f=np.fft.rfftfreq(len(core),1.0/fr)
top=np.argsort(sp)[::-1][:8]
print("Picos espectrales del boop (Hz : magnitud):")
for k in sorted(top, key=lambda j:f[j]):
    print(f"   {f[k]:6.0f} Hz : {sp[k]:.0f}")
# ¿el segmento se repite? autocorrelacion corta
print(f"pico abs en ventana: {np.max(np.abs(seg)):.0f}  (vs mediana global {np.median(np.abs(a)):.0f})")
