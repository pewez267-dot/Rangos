import com.gbaminecraft.emulator.input.GBAInput;
/** Play: auto-player adaptativo. Avanza la intro pulsando A cuando la pantalla
 *  se estabiliza (texto terminado). Reporta progreso (maxROM, DISPCNT, escenas). */
public class Play {
  public static void main(String[] a) throws Exception {
    H h=new H(a[0]);
    int budget=a.length>1?Integer.parseInt(a[1]):40000;
    h.frames(5200); h.pressStart(); h.frames(60);
    System.out.printf("inicio: DISPCNT=0x%04X maxROM=0x%08X%n", h.dispcnt(), h.maxRomPc);
    int prevHash=h.fbHash(); int stable=0; int f=0; long lastReportMax=0; int prevDisp=h.dispcnt();
    java.util.HashSet<Long> scenes=new java.util.HashSet<>();
    int aPresses=0;
    while(f<budget){
      h.frame(); h.trackMax(); f++;
      if(f%10==0){
        int hsh=h.fbHash();
        if(hsh==prevHash) stable++; else { stable=0; prevHash=hsh; }
        // cuando la pantalla lleva ~30 frames estable, pulsar A (avanzar dialogo/confirmar)
        if(stable>=3){
          h.tap(GBAInput.KEY_A,3,2); aPresses++; f+=5; stable=0; prevHash=h.fbHash();
        }
      }
      int d=h.dispcnt();
      if(d!=prevDisp || h.maxRomPc>lastReportMax+0x2000){
        System.out.printf("f%6d DISPCNT=0x%04X col=%d maxROM=0x%08X (A=%d)%n", f, d, h.fbColors(), h.maxRomPc, aPresses);
        prevDisp=d; lastReportMax=h.maxRomPc;
      }
    }
    System.out.printf("FIN f%d: DISPCNT=0x%04X col=%d maxROM=0x%08X A_pulsados=%d%n", f, h.dispcnt(), h.fbColors(), h.maxRomPc, aPresses);
  }
}
