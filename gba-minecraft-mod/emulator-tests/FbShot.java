import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import java.lang.reflect.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.*;

/** Headless screenshotter for the fantastic-boy emulator core. */
public class FbShot {
    public static void main(String[] a) throws Exception {
        byte[] rom = Files.readAllBytes(Paths.get(a[0]));
        int frames = Integer.parseInt(a[1]);
        String dir = a[2];
        Set<Integer> shots = new HashSet<>();
        for (int i = 3; i < a.length; i++) shots.add(Integer.parseInt(a[i]));
        new File(dir).mkdirs();

        GBAEmulator emu = new GBAEmulator();
        emu.loadROM(rom, "Pokemon Esmeralda");
        Field busF = GBAEmulator.class.getDeclaredField("bus"); busF.setAccessible(true);
        Field cpuF = GBAEmulator.class.getDeclaredField("cpu"); cpuF.setAccessible(true);
        Field ppuF = GBAEmulator.class.getDeclaredField("ppu"); ppuF.setAccessible(true);
        Field timF = GBAEmulator.class.getDeclaredField("timers"); timF.setAccessible(true);
        MemoryBus bus = (MemoryBus) busF.get(emu);
        ARM7TDMI cpu = (ARM7TDMI) cpuF.get(emu);
        PPU ppu = (PPU) ppuF.get(emu);
        com.gbaminecraft.emulator.timer.TimerController timers =
            (com.gbaminecraft.emulator.timer.TimerController) timF.get(emu);
        Method isIRQ = MemoryBus.class.getMethod("isIRQPending");
        final int CPF = 280896;
        // Simulate a player tapping A + START to advance intro/menus.
        Method press = GBAEmulator.class.getMethod("pressKey", int.class);
        Method release = GBAEmulator.class.getMethod("releaseKey", int.class);
        long lastCb2 = -1;

        for (int f = 0; f < frames; f++) {
            // Tap A (0) and START (3): pressed for frames 0-3 of each 24-frame cycle.
            int phase = f % 24;
            if (phase == 0) { press.invoke(emu, 0); press.invoke(emu, 3); }
            if (phase == 4) { release.invoke(emu, 0); release.invoke(emu, 3); }
            int left = CPF;
            while (left > 0) {
                if ((Boolean) isIRQ.invoke(bus)) cpu.triggerIRQ();
                int cyc;
                if (cpu.halted) cyc = 4;
                else cyc = cpu.step();
                ppu.tick(cyc);
                timers.tick(cyc);
                bus.tickSerial(cyc);
                left -= cyc;
            }
            ppu.pollNewFrame();
            long cb2now = bus.read32(0x030022C4) & 0xFFFFFFFFL;
            if (cb2now != lastCb2) {
                System.out.printf(">>> frame %5d CB2 changed -> %08X  DISPCNT=%04X%n",
                    f, (int)cb2now, bus.read16(0x04000000)&0xFFFF);
                lastCb2 = cb2now;
            }
            if (f % 200 == 0) {
                System.out.printf("frame %5d cb1@22C0=%08X cb2@22C4=%08X DISPCNT=%04X BG0CNT=%04X intrChk=%04X%n",
                    f, bus.read32(0x030022C0), bus.read32(0x030022C4),
                    bus.read16(0x04000000)&0xFFFF, bus.read16(0x04000008)&0xFFFF, bus.read16(0x030022DC)&0xFFFF);
            }
            if (shots.contains(f)) {
                int[] fb = ppu.getFramebuffer();
                BufferedImage img = new BufferedImage(240,160,BufferedImage.TYPE_INT_RGB);
                Set<Integer> cols = new HashSet<>();
                for (int y=0;y<160;y++) for (int x=0;x<240;x++){int p=fb[y*240+x]&0xFFFFFF;img.setRGB(x,y,p);cols.add(p);}
                File out = new File(dir, "fb_"+f+".png");
                ImageIO.write(img,"png",out);
                System.out.println("saved "+out.getPath()+" colors="+cols.size());
            }
        }
    }
}
