import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;

/**
 * Headless PPU render + timing test. Verifies that:
 *  - Mode 3 (15-bit bitmap) renders pixels with correct color conversion
 *  - Mode 4 (8-bit paletted) resolves palette entries
 *  - VBlank edge fires exactly once per frame
 *  - A full frame completes in the expected number of scanlines
 */
public class PpuTest {

    static int passed = 0, failed = 0;

    static PPU makePpu(MemoryBus bus) {
        PPU ppu = new PPU(bus);
        com.gbaminecraft.emulator.apu.APU apu = new com.gbaminecraft.emulator.apu.APU(bus);
        com.gbaminecraft.emulator.input.GBAInput input = new com.gbaminecraft.emulator.input.GBAInput();
        com.gbaminecraft.emulator.timer.TimerController timers = new com.gbaminecraft.emulator.timer.TimerController(bus);
        com.gbaminecraft.emulator.dma.DMAController dma = new com.gbaminecraft.emulator.dma.DMAController(bus);
        com.gbaminecraft.emulator.cpu.ARM7TDMI cpu = new com.gbaminecraft.emulator.cpu.ARM7TDMI(bus);
        bus.connectSubsystems(ppu, apu, input, timers, dma, cpu);
        ppu.reset();
        return ppu;
    }

    public static void main(String[] args) {
        testMode3Bitmap();
        testMode4Paletted();
        testVBlankTiming();
        testFrameProduced();

        System.out.println("\n=========================================");
        System.out.println("  PPU: " + passed + " PASARON, " + failed + " FALLARON");
        System.out.println("=========================================");
        if (failed > 0) System.exit(1);
    }

    static void check(String name, boolean ok, String detail) {
        if (ok) { passed++; System.out.printf("  OK  %-30s %s%n", name, detail); }
        else    { failed++; System.out.printf("  XX  %-30s %s%n", name, detail); }
    }

    static int color15(int r5, int g5, int b5) { return (r5 & 0x1F) | ((g5 & 0x1F) << 5) | ((b5 & 0x1F) << 10); }

    // Run one full frame (228 scanlines worth of cycles)
    static void runFrame(PPU ppu) {
        for (int i = 0; i < PPU.LINES_PER_FRAME; i++) {
            ppu.tick(PPU.CYCLES_PER_LINE);
        }
    }

    static void testMode3Bitmap() {
        System.out.println("[Mode 3 bitmap 15-bit]");
        MemoryBus bus = new MemoryBus();
        PPU ppu = makePpu(bus);
        // DISPCNT = mode 3, BG2 on (bit 10)
        bus.write16(0x04000000, (short)(3 | (1 << 10)));
        // Pixel (0,0) = pure red, (1,0) = pure green
        int addr = 0x06000000;
        bus.write16(addr,     (short) color15(31, 0, 0));   // red
        bus.write16(addr + 2, (short) color15(0, 31, 0));   // green
        runFrame(ppu);
        int[] fb = ppu.getFramebuffer();
        int p0 = fb[0] & 0xFFFFFF, p1 = fb[1] & 0xFFFFFF;
        check("pixel(0,0) rojo", p0 == 0xF80000, String.format("0x%06X", p0));
        check("pixel(1,0) verde", p1 == 0x00F800, String.format("0x%06X", p1));
    }

    static void testMode4Paletted() {
        System.out.println("[Mode 4 paletizado 8bpp]");
        MemoryBus bus = new MemoryBus();
        PPU ppu = makePpu(bus);
        bus.write16(0x04000000, (short)(4 | (1 << 10))); // mode 4, BG2 on
        // palette entry 5 = blue
        bus.write16(0x05000000 + 5*2, (short) color15(0, 0, 31));
        // VRAM index at pixel (10,0) = 5
        bus.write8(0x06000000 + 10, (byte)5);
        runFrame(ppu);
        int[] fb = ppu.getFramebuffer();
        int p = fb[10] & 0xFFFFFF;
        check("pixel(10,0) azul (paleta 5)", p == 0x0000F8, String.format("0x%06X", p));
    }

    static void testVBlankTiming() {
        System.out.println("[VBlank dispara una sola vez por frame]");
        MemoryBus bus = new MemoryBus();
        PPU ppu = makePpu(bus);
        int edges = 0;
        for (int i = 0; i < PPU.LINES_PER_FRAME; i++) {
            ppu.tick(PPU.CYCLES_PER_LINE);
            if (ppu.pollVBlankEdge()) edges++;
        }
        check("exactamente 1 flanco VBlank", edges == 1, "edges=" + edges);
    }

    static void testFrameProduced() {
        System.out.println("[Frame completo señalizado]");
        MemoryBus bus = new MemoryBus();
        PPU ppu = makePpu(bus);
        boolean frame = false;
        for (int i = 0; i < PPU.LINES_PER_FRAME; i++) {
            ppu.tick(PPU.CYCLES_PER_LINE);
            if (ppu.pollNewFrame()) frame = true;
        }
        check("pollNewFrame() true", frame, "");
    }
}
