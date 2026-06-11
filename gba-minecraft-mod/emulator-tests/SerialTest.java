import com.gbaminecraft.emulator.memory.MemoryBus;

/** Verifica el SerialController: una transferencia iniciada (SIOCNT bit7=start)
 *  como maestro debe AUTO-completarse al avanzar el reloj, limpiando el bit de
 *  start y, si la IRQ de Serial está habilitada, dejando IF bit7 pendiente.
 *  Esto es lo que evita que Pokémon Emerald cuelgue al inicializar el cable link. */
public class SerialTest {
    static int passed=0, failed=0;
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-40s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-40s %s%n",n,d);}
    }
    public static void main(String[] a) {
        MemoryBus bus = new MemoryBus();

        // Normal 32-bit, maestro (clock interno bit0=1), start(bit7), IRQ(bit14).
        // SIOCNT = 0x5081 (igual que el arranque real de Emerald).
        bus.write16(0x04000208, (short)1);      // IME
        bus.write16(0x04000200, (short)0x0080); // IE: Serial (bit7)
        bus.write16(0x04000128, (short)0x5081); // SIOCNT: start + irq + internal + N32

        check("start activo justo tras escribir", (bus.read16(0x04000128)&0x80)!=0,
              String.format("SIOCNT=0x%04X", bus.read16(0x04000128)&0xFFFF));

        // Avanza el reloj de la SIO; debe completar la transferencia.
        for(int i=0;i<8;i++) bus.tickSerial(1000);

        int siocnt = bus.read16(0x04000128)&0xFFFF;
        check("start (bit7) limpio tras completar", (siocnt&0x80)==0,
              String.format("SIOCNT=0x%04X", siocnt));
        check("IRQ de Serial pendiente (IF bit7)", (bus.read16(0x04000202)&0x80)!=0,
              String.format("IF=0x%04X", bus.read16(0x04000202)&0xFFFF));
        check("isIRQPending (IE&IF&IME)", bus.isIRQPending(), "");

        // Sin compañero, los datos recibidos leen 0xFFFF.
        check("SIODATA32 = 0xFFFFFFFF (sin compañero)",
              (bus.read16(0x04000120)&0xFFFF)==0xFFFF,
              String.format("0x%04X", bus.read16(0x04000120)&0xFFFF));

        System.out.println("\n=========================================");
        System.out.println("  SERIAL (link-cable boot): "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
}
