import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.input.GBAInput;
import com.gbaminecraft.emulator.timer.TimerController;
import com.gbaminecraft.emulator.dma.DMAController;
import java.lang.reflect.Field;

/**
 * End-to-end test: a hand-assembled ARM program runs on the CPU, sets video
 * Mode 3, and writes pixels to VRAM. We then tick the PPU and verify the
 * framebuffer shows what the PROGRAM drew. This exercises CPU+memory+PPU together.
 */
public class IntegrationTest {
    static int passed=0, failed=0;
    public static void main(String[] a) throws Exception {
        MemoryBus bus = new MemoryBus();
        PPU ppu = new PPU(bus);
        APU apu = new APU(bus);
        GBAInput input = new GBAInput();
        TimerController timers = new TimerController(bus);
        DMAController dma = new DMAController(bus);
        ARM7TDMI cpu = new ARM7TDMI(bus);
        bus.connectSubsystems(ppu, apu, input, timers, dma, cpu);
        ppu.reset();

        Field regsF = ARM7TDMI.class.getField("regs");
        Field cpsrF = ARM7TDMI.class.getField("cpsr");
        int[] regs = (int[]) regsF.get(cpu);
        regs[15] = 0x03000000;   // run from IWRAM
        cpsrF.setInt(cpu, 0x1F);

        // Program @ 0x03000000:
        //   mov r0, #0x04000000        ; DISPCNT addr
        //   mov r1, #0x0400 ; orr #3   ; mode3 + BG2  (0x0403)
        //   strh r1,[r0]
        //   mov r0, #0x06000000        ; VRAM
        //   mov r1, #0x001F            ; red
        //   strh r1,[r0]
        //   mov r1, #0x7C00            ; blue (15-bit b=31)
        //   strh r1,[r0,#2]
        //   b .                        ; halt loop
        int p = 0x03000000;
        int[] prog = {
            0xE3A00301, // mov r0,#0x04000000  (0x01 ror 6 = 0x04000000)
            0xE3A01B01, // mov r1,#0x0400      (0x01 ror 22 = 0x400)
            0xE3811003, // orr r1,r1,#3        -> 0x0403
            0xE1C010B0, // strh r1,[r0]
            0xE3A00406, // mov r0,#0x06000000  (0x06 ror 8 = 0x06000000)
            0xE3A0101F, // mov r1,#0x1F        (red)
            0xE1C010B0, // strh r1,[r0]
            0xE3A01B1F, // mov r1,#0x7C00      (0x1F ror 22 = 0x7C00) blue
            0xE1C010B2, // strh r1,[r0,#2]
            0xEAFFFFFE  // b . (infinite)
        };
        for (int i=0;i<prog.length;i++) bus.write32(p+i*4, prog[i]);

        // Execute the setup instructions (9 of them, then the loop)
        for (int i=0;i<9;i++) cpu.step();

        check("DISPCNT escrito por CPU", bus.read16(0x04000000)==0x0403,
              String.format("0x%04X", bus.read16(0x04000000)));
        check("VRAM[0] escrito por CPU", (bus.read16(0x06000000)&0xFFFF)==0x001F,
              String.format("0x%04X", bus.read16(0x06000000)));
        check("VRAM[2] escrito por CPU", (bus.read16(0x06000002)&0xFFFF)==0x7C00,
              String.format("0x%04X", bus.read16(0x06000002)));

        // Now render a frame and verify the framebuffer reflects what the program drew
        for (int i=0;i<PPU.LINES_PER_FRAME;i++) ppu.tick(PPU.CYCLES_PER_LINE);
        int[] fb = ppu.getFramebuffer();
        check("framebuffer(0,0) rojo", (fb[0]&0xFFFFFF)==0xF80000, String.format("0x%06X", fb[0]&0xFFFFFF));
        check("framebuffer(1,0) azul", (fb[1]&0xFFFFFF)==0x0000F8, String.format("0x%06X", fb[1]&0xFFFFFF));

        System.out.println("\n=========================================");
        System.out.println("  INTEGRACION CPU+MEM+PPU: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if (failed>0) System.exit(1);
    }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-32s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-32s %s%n",n,d);}
    }
}
