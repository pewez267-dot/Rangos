import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import java.lang.reflect.Field;
/** Verifica las ops Thumb de registro alto (MOV/ADD/CMP r8-r15), que el boot de
 *  Pokemon usa para punteros de frame. Antes 0x46xx se enrutaba mal a thumbALU. */
public class HiRegTest {
    static int passed=0, failed=0; static MemoryBus bus; static ARM7TDMI cpu; static int[] regs; static Field cpsrF;
    static final int P=0x03000000;
    public static void main(String[] a) throws Exception {
        Field rf=ARM7TDMI.class.getField("regs"); cpsrF=ARM7TDMI.class.getField("cpsr");
        bus=new MemoryBus(); cpu=new ARM7TDMI(bus); regs=(int[])rf.get(cpu);
        // Thumb mode
        // MOV r8, r0  => 0x4680 (op=2 MOV, h1=1 -> rd=8, rs=r0)
        t(); regs[0]=0xDEAD; put(0,0x4680); cpu.step(); ck("MOV r8,r0", regs[8]==0xDEAD, regs[8]);
        // MOV r7, r8  => 0x4647 (op=2, h2=1 -> rs=8, rd=r7)
        t(); regs[8]=0xBEEF; put(0,0x4647); cpu.step(); ck("MOV r7,r8", regs[7]==0xBEEF, regs[7]);
        // ADD r8, r1  => 0x4488 (op=0 ADD, h1=1 -> rd=8, rs=r1)
        t(); regs[8]=0x10; regs[1]=0x05; put(0,0x4488); cpu.step(); ck("ADD r8,r1", regs[8]==0x15, regs[8]);
        // ADD r10, r1 (frame ptr) => 0x448A
        t(); regs[10]=0x100; regs[1]=0x20; put(0,0x448A); cpu.step(); ck("ADD r10,r1", regs[10]==0x120, regs[10]);
        // CMP r8, r0 con iguales -> Z=1 => 0x4580
        t(); regs[8]=7; regs[0]=7; put(0,0x4580); cpu.step(); ck("CMP r8,r0 (Z)", (cpsrFi()&0x40000000)!=0, cpsrFi());
        System.out.println("\n  HI-REG: "+passed+" PASARON, "+failed+" FALLARON");
        if(failed>0) System.exit(1);
    }
    static int cpsrFi() throws Exception { return cpsrF.getInt(cpu); }
    static void t() throws Exception { regs[15]=P; cpsrF.setInt(cpu, 0x1F|(1<<5)); } // Thumb
    static void put(int o,int v){ bus.write16(P+o,(short)v); }
    static void ck(String n, boolean ok, int v){ if(ok){passed++;System.out.printf("  OK  %-14s 0x%08X%n",n,v);} else {failed++;System.out.printf("  XX  %-14s 0x%08X%n",n,v);} }
}
