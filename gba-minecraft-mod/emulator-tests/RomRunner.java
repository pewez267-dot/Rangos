import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.debug.BootTracer;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import java.lang.reflect.*;
import java.nio.file.*;

/**
 * Headless runner: carga la ROM real y ejecuta N frames SIN Minecraft, luego
 * vuelca el diagnostico. Asi puedo cazar bugs aqui mismo, sin recompilar el mod.
 */
public class RomRunner {
    public static void main(String[] a) throws Exception {
        byte[] rom = Files.readAllBytes(Paths.get(a[0]));
        int frames = a.length > 1 ? Integer.parseInt(a[1]) : 600;

        GBAEmulator emu = new GBAEmulator();
        emu.loadROM(rom, "Pokemon Esmeralda");

        // Acceso por reflexion a los internos para correr sin el hilo de Minecraft.
        Field busF = GBAEmulator.class.getDeclaredField("bus"); busF.setAccessible(true);
        Field cpuF = GBAEmulator.class.getDeclaredField("cpu"); cpuF.setAccessible(true);
        Field ppuF = GBAEmulator.class.getDeclaredField("ppu"); ppuF.setAccessible(true);
        Field traF = GBAEmulator.class.getDeclaredField("tracer"); traF.setAccessible(true);
        MemoryBus bus = (MemoryBus) busF.get(emu);
        ARM7TDMI cpu = (ARM7TDMI) cpuF.get(emu);
        PPU ppu = (PPU) ppuF.get(emu);
        BootTracer tracer = (BootTracer) traF.get(emu);
        tracer.setEnabled(true);

        // Replica el bucle de runFrame() aqui (sin hilo).
        Method isIRQ = MemoryBus.class.getMethod("isIRQPending");
        Field timF = GBAEmulator.class.getDeclaredField("timers"); timF.setAccessible(true);
        com.gbaminecraft.emulator.timer.TimerController timers =
                (com.gbaminecraft.emulator.timer.TimerController) timF.get(emu);
        final int CYCLES_PER_FRAME = 280896;
        long totalInstr = 0;
        for (int f = 0; f < frames; f++) {
            int left = CYCLES_PER_FRAME;
            while (left > 0) {
                if ((Boolean) isIRQ.invoke(bus)) { tracer.onIrq(bus.read16(0x04000202)); cpu.triggerIRQ(); }
                int cyc;
                if (cpu.halted) { cyc = 4; }
                else {
                    int pc = cpu.getPC();
                    int instr = cpu.isThumb() ? bus.read16(pc & ~1) : bus.read32(pc & ~3);
                    tracer.onStep(pc, instr, cpu);
                    cyc = cpu.step();
                    totalInstr++;
                }
                ppu.tick(cyc);
                timers.tick(cyc);      // timers drive delay loops the boot code waits on
                bus.tickSerial(cyc);   // serial transfers self-complete (link-cable init)
                if (ppu.pollVBlankEdge()) { /* dma vblank ya interno */ }
                left -= cyc;
            }
            if (ppu.pollNewFrame()) tracer.onFrame();
        }

        System.out.println("Frames simulados: " + frames + "  Instrucciones: " + totalInstr);
        System.out.println(tracer.report(cpu, bus));
    }
}
