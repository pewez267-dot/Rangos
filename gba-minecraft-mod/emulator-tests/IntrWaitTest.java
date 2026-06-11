import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.bios.HleBios;
import com.gbaminecraft.emulator.cartridge.GpioRtc;
import java.lang.reflect.Field;

/**
 * Regression tests for the BIOS IntrWait/VBlankIntrWait semantics and the
 * cartridge RTC. These two fixes were needed for Pokémon Emerald's boot:
 *  - VBlankIntrWait must actually wait for a fresh VBlank IRQ (it used to just
 *    set halted=true and fall through, spinning thousands of times per frame).
 *  - The GPIO/RTC must answer the boot-time probes (game stalls otherwise).
 */
public class IntrWaitTest {
    static int passed = 0, failed = 0;
    static void check(String n, boolean ok, String d) {
        if (ok) { passed++; System.out.printf("  OK  %-40s %s%n", n, d); }
        else    { failed++; System.out.printf("  XX  %-40s %s%n", n, d); }
    }

    public static void main(String[] a) throws Exception {
        testVBlankIntrWait();
        testRtc();
        System.out.println("\n=========================================");
        System.out.println("  INTRWAIT + RTC: " + passed + " PASARON, " + failed + " FALLARON");
        System.out.println("=========================================");
        if (failed > 0) System.exit(1);
    }

    static void testVBlankIntrWait() throws Exception {
        MemoryBus bus = new MemoryBus();
        ARM7TDMI cpu = new ARM7TDMI(bus);
        HleBios hle = new HleBios(cpu, bus);
        cpu.setHleBios(hle);
        Field rf = ARM7TDMI.class.getField("regs");
        Field cf = ARM7TDMI.class.getField("cpsr");
        int[] regs = (int[]) rf.get(cpu);

        // Program at 0x03000000: SWI 5 (VBlankIntrWait), then b .
        int BASE = 0x03000000;
        bus.write16(BASE,     (short)0xDF05); // Thumb SWI 5
        bus.write16(BASE + 2, (short)0xE7FE); // b . (self)
        regs[15] = BASE;
        cf.setInt(cpu, 0x3F);                 // System, Thumb=false? set Thumb:
        // set Thumb mode (bit5)
        cf.setInt(cpu, cf.getInt(cpu) | (1 << 5));
        regs[13] = 0x03007F00;
        bus.write16(0x04000208, (short)1);          // IME
        bus.write16(0x04000200, (short)0x0001);     // IE = VBlank
        // Pre-set the BIOS INTRCHECK mirror with a STALE VBlank bit; a correct
        // VBlankIntrWait must NOT return on the stale bit — it must wait for a
        // fresh one.
        bus.write16(0x03007FF8, (short)0x0001);

        // Execute the SWI. It should clear the awaited bit and halt.
        cpu.step();
        check("VBlankIntrWait limpia bit viejo y espera",
              cpu.halted && (bus.read16(0x03007FF8) & 1) == 0,
              "halted=" + cpu.halted + " INTRCHECK=0x" + Integer.toHexString(bus.read16(0x03007FF8) & 0xFFFF));

        // Still halted while no VBlank arrives: stepping does nothing.
        cpu.step();
        check("sigue esperando sin VBlank", cpu.halted, "halted=" + cpu.halted);

        // Simulate the game's IRQ handler recording a VBlank in the mirror, and
        // wake the CPU (as triggerIRQ would). Re-running the SWI must now finish.
        bus.write16(0x03007FF8, (short)(bus.read16(0x03007FF8) | 0x0001));
        cpu.halted = false;                   // IRQ wakes the CPU
        cpu.step();                            // re-executes the SWI, sees the bit
        check("VBlankIntrWait termina al llegar VBlank",
              !cpu.halted && (bus.read16(0x03007FF8) & 1) == 0,
              "halted=" + cpu.halted + " INTRCHECK=0x" + Integer.toHexString(bus.read16(0x03007FF8) & 0xFFFF));
    }

    static void testRtc() {
        GpioRtc rtc = new GpioRtc();
        // GPIO is not readable until control bit0 is set.
        rtc.write16(0x080000C8, 1);            // enable register read
        check("RTC dir addr es GPIO", GpioRtc.isGpioAddr(0x080000C4), "");
        check("0x080000C8 fuera de rango GPIO? no", GpioRtc.isGpioAddr(0x080000C8), "");
        // Control register reads back the readable flag.
        int ctrl = rtc.read16(0x080000C8);
        check("RTC control readable=1", (ctrl & 1) == 1, "ctrl=0x" + Integer.toHexString(ctrl));
        // A non-GPIO ROM address returns -1 (caller falls back to ROM).
        check("addr no-GPIO devuelve -1", rtc.read16(0x08000000) == -1, "");
    }
}
