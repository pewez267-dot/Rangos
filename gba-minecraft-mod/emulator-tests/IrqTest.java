import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.bios.HleBios;
import java.lang.reflect.Field;

/** Verifica el despacho HLE de IRQ: instala un handler de juego en [0x03007FFC],
 *  dispara una IRQ y comprueba que el handler corre, retorna y el programa sigue. */
public class IrqTest {
    static int passed=0, failed=0;
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-34s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-34s %s%n",n,d);}
    }
    public static void main(String[] a) throws Exception {
        MemoryBus bus = new MemoryBus();
        ARM7TDMI cpu = new ARM7TDMI(bus);
        cpu.setHleBios(new HleBios(cpu, bus));
        Field rf = ARM7TDMI.class.getField("regs");
        Field cf = ARM7TDMI.class.getField("cpsr");
        int[] regs = (int[]) rf.get(cpu);

        int BASE=0x03000000;
        // Programa principal: bucle infinito (b .) en BASE
        bus.write32(BASE, 0xEAFFFFFE); // b .
        regs[15]=BASE; cf.setInt(cpu, 0x1F); // System mode, ARM, IRQ enabled (I=0)
        regs[13]=0x03007F00; // SP

        // Handler de IRQ del "juego" en 0x03001000:
        //   ldr r0,[pc,#0]  ; r0 = 0x03002000 (contador)
        //   ... pero mas simple: incrementa palabra en 0x03002000
        int H=0x03001000;
        // mov r0,#0x03002000? usamos: mov r1,#1 ; str r1,[r0] con r0 precargado por nosotros.
        // Programa del handler:
        //   ldr r0, =0x03002000 (via pc-rel)
        //   mov r1, #0x55
        //   str r1, [r0]
        //   bx  lr
        bus.write32(H+0,  0xE59F0008); // ldr r0,[pc,#8] -> carga palabra en H+0x10
        bus.write32(H+4,  0xE3A01055); // mov r1,#0x55
        bus.write32(H+8,  0xE5801000); // str r1,[r0]
        bus.write32(H+12, 0xE12FFF1E); // bx lr
        bus.write32(H+16, 0x03002000); // dato: direccion del contador
        // instala puntero del handler
        bus.write32(0x03007FFC, H);

        // Habilita IRQ: IME=1, IE=VBlank(bit0), e IF con bit0 pendiente
        bus.write16(0x04000208, (short)1);      // IME
        bus.write16(0x04000200, (short)0x0001); // IE bit0
        bus.requestInterrupt(1<<0);      // IF bit0 pendiente

        // Corre unas instrucciones del bucle; el runFrame real dispararia la IRQ,
        // aqui la disparamos manualmente como hace el main loop:
        cpu.step(); // ejecuta b . una vez
        check("IRQ pendiente detectada", bus.isIRQPending(), "");
        cpu.triggerIRQ();                // entra al handler HLE
        // ejecuta el handler: ldr, mov, str, bx lr, y el unwind del sentinel
        for(int i=0;i<6;i++) cpu.step();

        int val = bus.read32(0x03002000);
        check("handler escribió 0x55", (val&0xFF)==0x55, String.format("0x%08X",val));
        // Tras el unwind, PC debe volver al programa principal (BASE)
        check("PC volvió al programa", (regs[15]&~3)==BASE, String.format("0x%08X",regs[15]));

        // ── Regresión: el BIOS preserva r0-r3 y r12 del código interrumpido ──
        // Un handler que CLOBBEA r0-r3/r12 (como hace el dispatcher de Pokémon,
        // que pisa r1-r3 con IE/IF antes de su propio push) NO debe corromper
        // los registros del programa interrumpido. Sin esto, Emerald cuelga en
        // sus delay loops porque R1 (un puntero) vuelve como IE&IF.
        int H2=0x03001100;
        bus.write32(H2+0,  0xE3A000AA); // mov r0,#0xAA
        bus.write32(H2+4,  0xE3A010BB); // mov r1,#0xBB
        bus.write32(H2+8,  0xE3A020CC); // mov r2,#0xCC
        bus.write32(H2+12, 0xE3A030DD); // mov r3,#0xDD
        bus.write32(H2+16, 0xE3A0C0EE); // mov r12,#0xEE
        bus.write32(H2+20, 0xE12FFF1E); // bx lr
        bus.write32(0x03007FFC, H2);

        // Estado del programa interrumpido con valores conocidos en r0-r3,r12.
        regs[15]=BASE; cf.setInt(cpu, 0x1F); regs[13]=0x03007F00;
        regs[0]=0x10000000; regs[1]=0x10000001; regs[2]=0x10000002;
        regs[3]=0x10000003; regs[12]=0x1000000C;
        bus.write16(0x04000208,(short)1); bus.write16(0x04000200,(short)1);
        bus.requestInterrupt(1<<0);
        cpu.step();                 // bucle principal
        cpu.triggerIRQ();           // entra al handler (clobbea r0-r3,r12)
        for(int i=0;i<8;i++) cpu.step();
        check("r0 preservado tras IRQ",  regs[0]==0x10000000,  String.format("0x%08X",regs[0]));
        check("r1 preservado tras IRQ",  regs[1]==0x10000001,  String.format("0x%08X",regs[1]));
        check("r2 preservado tras IRQ",  regs[2]==0x10000002,  String.format("0x%08X",regs[2]));
        check("r3 preservado tras IRQ",  regs[3]==0x10000003,  String.format("0x%08X",regs[3]));
        check("r12 preservado tras IRQ", regs[12]==0x1000000C, String.format("0x%08X",regs[12]));

        System.out.println("\n=========================================");
        System.out.println("  IRQ HLE: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
}
