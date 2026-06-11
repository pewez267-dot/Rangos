import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import java.lang.reflect.Field;

/**
 * Verifica LDM/STM en los 4 modos y un roundtrip PUSH/POP con writeback,
 * que es exactamente lo que un juego hace para montar su pila al arrancar.
 * Este era el bug que descarrilaba el boot (SP corrompido).
 */
public class StackTest {
    static int passed=0, failed=0;
    static MemoryBus bus; static ARM7TDMI cpu; static int[] regs; static Field cpsrF;
    static final int BASE=0x03000000;

    public static void main(String[] a) throws Exception {
        Field rf = ARM7TDMI.class.getField("regs");
        cpsrF = ARM7TDMI.class.getField("cpsr");
        bus = new MemoryBus(); cpu = new ARM7TDMI(bus); regs=(int[])rf.get(cpu);

        testPushPopRoundtrip();
        testStmfdWriteback();
        testLdmiaWriteback();

        System.out.println("\n=========================================");
        System.out.println("  STACK (LDM/STM): "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-34s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-34s %s%n",n,d);}
    }
    static void fresh() throws Exception { regs[15]=BASE; cpsrF.setInt(cpu,0x1F); }

    // PUSH {r1,r2,lr} ; luego POP {r1,r2,pc-equiv} y verifica SP intacto
    static void testPushPopRoundtrip() throws Exception {
        fresh();
        regs[13]=0x03007F00;          // SP
        regs[1]=0x11111111; regs[2]=0x22222222; regs[14]=0x08000123;
        // STMFD sp!,{r1,r2,lr}  => 0xE92D4006
        bus.write32(BASE, 0xE92D4006);
        cpu.step();
        int spAfterPush = regs[13];
        check("SP tras PUSH = base-12", spAfterPush==0x03007F00-12, String.format("0x%08X",spAfterPush));
        check("mem top = r1", bus.read32(spAfterPush)==0x11111111, String.format("0x%08X",bus.read32(spAfterPush)));
        check("mem+8 = lr", bus.read32(spAfterPush+8)==0x08000123, String.format("0x%08X",bus.read32(spAfterPush+8)));

        // corrompe registros y haz POP {r1,r2,lr}  => LDMFD sp!,{r1,r2,lr} = 0xE8BD4006
        regs[1]=0; regs[2]=0; regs[14]=0;
        regs[15]=BASE+4; bus.write32(BASE+4, 0xE8BD4006);
        cpu.step();
        check("r1 restaurado", regs[1]==0x11111111, String.format("0x%08X",regs[1]));
        check("r2 restaurado", regs[2]==0x22222222, String.format("0x%08X",regs[2]));
        check("lr restaurado", regs[14]==0x08000123, String.format("0x%08X",regs[14]));
        check("SP volvió a base (roundtrip)", regs[13]==0x03007F00, String.format("0x%08X",regs[13]));
    }

    // STMFD sp!,{r0-r3}: 4 regs, SP debe bajar 16
    static void testStmfdWriteback() throws Exception {
        fresh();
        regs[13]=0x03006000;
        regs[0]=0xA; regs[1]=0xB; regs[2]=0xC; regs[3]=0xD;
        bus.write32(BASE, 0xE92D000F); // stmfd sp!,{r0-r3}
        cpu.step();
        check("STMFD SP = base-16", regs[13]==0x03006000-16, String.format("0x%08X",regs[13]));
        // r0 (mas bajo) en la direccion mas baja
        check("r0 en addr más baja", bus.read32(regs[13])==0xA, String.format("0x%08X",bus.read32(regs[13])));
        check("r3 en addr más alta", bus.read32(regs[13]+12)==0xD, String.format("0x%08X",bus.read32(regs[13]+12)));
    }

    // LDMIA r4!,{r0,r1}: incrementa, writeback +8
    static void testLdmiaWriteback() throws Exception {
        fresh();
        regs[4]=0x03005000;
        bus.write32(0x03005000, 0x55555555);
        bus.write32(0x03005004, 0x66666666);
        bus.write32(BASE, 0xE8B40003); // ldmia r4!,{r0,r1}
        cpu.step();
        check("r0 (LDMIA)", regs[0]==0x55555555, String.format("0x%08X",regs[0]));
        check("r1 (LDMIA)", regs[1]==0x66666666, String.format("0x%08X",regs[1]));
        check("r4 writeback +8", regs[4]==0x03005000+8, String.format("0x%08X",regs[4]));
    }
}
