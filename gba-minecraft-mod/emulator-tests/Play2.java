import com.gbaminecraft.emulator.input.GBAInput;
/** Play2: pulsa A a ritmo constante (humano) y registra progreso de maxROM y
 *  cambios de DISPCNT, para ver hasta donde llega la intro. */
public class Play2 {
  public static void main(String[] a) throws Exception {
    H h=new H(a[0]);
    int budget=a.length>1?Integer.parseInt(a[1]):30000;
    h.frames(5200); h.pressStart(); h.frames(60);
    int f=0; int prevDisp=h.dispcnt(); long maxSeen=h.maxRomPc;
    System.out.printf("inicio: DISPCNT=0x%04X maxROM=0x%08X%n", h.dispcnt(), h.maxRomPc);
    while(f<budget){
      h.tap(GBAInput.KEY_A, 3, 22); f+=25; h.trackMax();
      int d=h.dispcnt();
      if(d!=prevDisp){ System.out.printf("f%6d ESCENA DISPCNT=0x%04X col=%d maxROM=0x%08X%n", f, d, h.fbColors(), h.maxRomPc); prevDisp=d; }
      if(h.maxRomPc>maxSeen){ maxSeen=h.maxRomPc; }
    }
    System.out.printf("FIN f%d: DISPCNT=0x%04X col=%d maxROM=0x%08X%n", f, h.dispcnt(), h.fbColors(), h.maxRomPc);
  }
}
