import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import java.lang.reflect.Field;
/** Verifica LDR Rd,[PC,#imm] (Thumb format 6) = como los juegos cargan
 *  constantes de 32 bits (direcciones). Este faltaba y causaba la pantalla roja. */
public class PcRelTest {
    static int passed=0, failed=0; static MemoryBus bus; static ARM7TDMI cpu; static int[] regs; static Field cpsrF;
    static final int P=0x03000000;
    public static void main(String[] a) throws Exception {
        Field rf=ARM7TDMI.class.getField("regs"); cpsrF=ARM7TDMI.class.getField("cpsr");
        bus=new MemoryBus(); cpu=new ARM7TDMI(bus); regs=(int[])rf.get(cpu);
        // Thumb. LDR r1,[pc,#0] => 0x4900. PC=(P+4)&~3. Pon una constante alli.
        regs[15]=P; cpsrF.setInt(cpu, 0x1F|(1<<5));
        bus.write16(P, (short)0x4900);            // ldr r1,[pc,#0]
        int litAddr = ((P+4)&~3) + 0;             // donde lee la constante
        bus.write32(litAddr, 0x0203B600);         // direccion real (no 0xB600!)
        cpu.step();
        ck("LDR r1,[pc,#0] carga constante completa", regs[1]==0x0203B600, regs[1]);
        // LDR r3,[pc,#8] => 0x4B02
        regs[15]=P; cpsrF.setInt(cpu, 0x1F|(1<<5));
        bus.write16(P,(short)0x4B02);
        bus.write32(((P+4)&~3)+8, 0xDEADBEEF);
        cpu.step();
        ck("LDR r3,[pc,#8]", regs[3]==0xDEADBEEF, regs[3]);
        System.out.println("\n  PC-REL LDR: "+passed+" PASARON, "+failed+" FALLARON");
        if(failed>0) System.exit(1);
    }
    static void ck(String n, boolean ok, int v){ if(ok){passed++;System.out.printf("  OK  %-38s 0x%08X%n",n,v);} else {failed++;System.out.printf("  XX  %-38s 0x%08X%n",n,v);} }
}
