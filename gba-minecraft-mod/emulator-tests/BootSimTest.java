import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import java.lang.reflect.Field;

/**
 * Reproduce la SECUENCIA DE ARRANQUE tipica de un juego GBA: cambiar de modo
 * via MSR y montar el SP de cada modo. Si algo falla, el SP se corrompe y el
 * boot descarrila (justo lo que vimos).
 */
public class BootSimTest {
    static int passed=0, failed=0;
    static MemoryBus bus; static ARM7TDMI cpu; static int[] regs; static Field cpsrF;
    static final int P=0x03000000;

    public static void main(String[] a) throws Exception {
        Field rf=ARM7TDMI.class.getField("regs"); cpsrF=ARM7TDMI.class.getField("cpsr");
        bus=new MemoryBus(); cpu=new ARM7TDMI(bus); regs=(int[])rf.get(cpu);

        testMsrModeSwitch();
        testPerModeStacks();
        testMrsReadback();

        System.out.println("\n=========================================");
        System.out.println("  BOOT-SIM: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-42s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-42s %s%n",n,d);}
    }
    static void put(int off,int instr){ bus.write32(P+off, instr); }
    static void run(int n){ for(int i=0;i<n;i++) cpu.step(); }
    static void fresh()throws Exception{ regs[15]=P; cpsrF.setInt(cpu,0x1F); }

    static void testPerModeStacks() throws Exception {
        fresh();
        put(0x00, 0xE321F0D2); run(1);  // msr cpsr_c,#0xD2 -> IRQ
        check("modo tras msr #0xD2 = IRQ(0x12)", (cpsrF.getInt(cpu)&0x1F)==0x12, String.format("0x%02X",cpsrF.getInt(cpu)&0x1F));
        regs[13]=0x03007FA0;
        regs[15]=P+4; put(0x04,0xE321F0D3); run(1);  // -> SVC
        check("modo tras msr #0xD3 = SVC(0x13)", (cpsrF.getInt(cpu)&0x1F)==0x13, String.format("0x%02X",cpsrF.getInt(cpu)&0x1F));
        check("SP de IRQ NO se ve en SVC", regs[13]!=0x03007FA0, String.format("0x%08X",regs[13]));
        regs[13]=0x03007FE0;
        regs[15]=P+8; put(0x08,0xE321F01F); run(1);  // -> SYS
        check("modo tras msr #0x1F = SYS(0x1F)", (cpsrF.getInt(cpu)&0x1F)==0x1F, String.format("0x%02X",cpsrF.getInt(cpu)&0x1F));
        regs[13]=0x03007F00;
        regs[15]=P+12; put(0x0C,0xE321F0D2); run(1); // back to IRQ
        check("volver a IRQ recupera su SP", regs[13]==0x03007FA0, String.format("0x%08X",regs[13]));
        regs[15]=P+16; put(0x10,0xE321F0D3); run(1); // back to SVC
        check("volver a SVC recupera su SP", regs[13]==0x03007FE0, String.format("0x%08X",regs[13]));
        regs[15]=P+20; put(0x14,0xE321F01F); run(1); // back to SYS
        check("volver a SYS recupera su SP", regs[13]==0x03007F00, String.format("0x%08X",regs[13]));
    }

    static void testMsrModeSwitch() throws Exception {
        fresh();
        put(0x00, 0xE321F0D1); run(1); // msr cpsr_c,#0xD1 -> FIQ, I+F off
        check("msr a FIQ", (cpsrF.getInt(cpu)&0x1F)==0x11, String.format("0x%02X",cpsrF.getInt(cpu)&0x1F));
        check("bit I activado por #0xD1", (cpsrF.getInt(cpu)&0x80)!=0, String.format("0x%08X",cpsrF.getInt(cpu)));
    }

    static void testMrsReadback() throws Exception {
        fresh();
        cpsrF.setInt(cpu, 0x6000001F);
        put(0x00, 0xE10F0000); run(1); // mrs r0, cpsr
        check("mrs r0,cpsr", regs[0]==0x6000001F, String.format("0x%08X",regs[0]));
    }
}
