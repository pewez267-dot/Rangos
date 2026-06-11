import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.GBAStateIO;
import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import java.io.*;
import java.lang.reflect.Field;

/** Verifica save-state: snapshot de RAM+CPU(+chip de save), modificar, restaurar. */
public class StateTest {
    static int passed=0, failed=0;
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-32s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-32s %s%n",n,d);}
    }
    public static void main(String[] a) throws Exception {
        GBAEmulator emu = new GBAEmulator();
        // ROM minima valida de 4KB (header no se valida a fondo en loadROM? usamos bytes)
        byte[] rom = new byte[0x8000];
        // pon una instruccion b . al inicio
        rom[0]=(byte)0xFE; rom[1]=(byte)0xFF; rom[2]=(byte)0xFF; rom[3]=(byte)0xEA;
        boolean loaded = emu.loadROM(rom, "test.gba");
        check("ROM cargada", loaded, "");

        MemoryBus bus = emu.getBus();
        ARM7TDMI cpu = emu.getCPU();
        Field rf = ARM7TDMI.class.getField("regs");
        int[] regs = (int[]) rf.get(cpu);

        // Escribe estado conocido
        bus.write32(0x02000000, 0xCAFEBABE);
        bus.write32(0x03000000, 0x12345678);
        regs[5] = 0xAABBCCDD;
        cpu.cpsr = 0x6000001F;

        // Snapshot
        ByteArrayOutputStream snap = new ByteArrayOutputStream();
        GBAStateIO.saveState(emu, snap);

        // Corrompe todo
        bus.write32(0x02000000, 0);
        bus.write32(0x03000000, 0);
        regs[5] = 0;
        cpu.cpsr = 0x1F;

        // Restaura
        boolean ok = GBAStateIO.loadState(emu, new ByteArrayInputStream(snap.toByteArray()));
        check("loadState exitoso", ok, "");
        check("EWRAM restaurada", bus.read32(0x02000000)==0xCAFEBABE, String.format("0x%08X",bus.read32(0x02000000)));
        check("IWRAM restaurada", bus.read32(0x03000000)==0x12345678, String.format("0x%08X",bus.read32(0x03000000)));
        check("R5 restaurado", regs[5]==0xAABBCCDD, String.format("0x%08X",regs[5]));
        check("CPSR restaurado", cpu.cpsr==0x6000001F, String.format("0x%08X",cpu.cpsr));

        System.out.println("\n=========================================");
        System.out.println("  SAVE-STATE: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
}
