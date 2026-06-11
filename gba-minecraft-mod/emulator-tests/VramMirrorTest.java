import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * Regression test for VRAM address mirroring. VRAM is 96 KB; the 0x06000000
 * region mirrors every 128 KB, and inside each 128 KB block the upper 32 KB
 * (0x18000-0x1FFFF) mirror 0x10000-0x17FFF. A naive (addr & 0x17FFF) corrupted
 * tile/charBase placement and left Pokémon's menus/intro blank.
 */
public class VramMirrorTest {
    static int passed = 0, failed = 0;
    static void check(String n, boolean ok, String d) {
        if (ok) { passed++; System.out.printf("  OK  %-44s %s%n", n, d); }
        else    { failed++; System.out.printf("  XX  %-44s %s%n", n, d); }
    }
    public static void main(String[] a) {
        MemoryBus bus = new MemoryBus();
        // Distinct writes to bases that a buggy &0x17FFF mask would collide.
        bus.write16(0x06008000, (short)0x1111); // charBase 2 (tiles)
        bus.write16(0x0600F000, (short)0x2222); // screenBase 30 (map)
        bus.write16(0x06002000, (short)0x3333);
        check("charBase 0x6008000 persiste",  (bus.read16(0x06008000)&0xFFFF)==0x1111, String.format("0x%04X",bus.read16(0x06008000)&0xFFFF));
        check("screenBase 0x600F000 persiste", (bus.read16(0x0600F000)&0xFFFF)==0x2222, String.format("0x%04X",bus.read16(0x0600F000)&0xFFFF));
        check("0x6002000 no colisiona con 0x600A000",
              (bus.read16(0x06002000)&0xFFFF)==0x3333, String.format("0x%04X",bus.read16(0x06002000)&0xFFFF));

        // Upper-32KB mirror: 0x06018000 must alias 0x06010000.
        bus.write16(0x06010000, (short)0xABCD);
        check("0x06018000 espeja 0x06010000",
              (bus.read16(0x06018000)&0xFFFF)==0xABCD, String.format("0x%04X",bus.read16(0x06018000)&0xFFFF));
        // 128KB mirror: 0x06020000 aliases 0x06000000.
        bus.write16(0x06000000, (short)0x5678);
        check("0x06020000 espeja 0x06000000",
              (bus.read16(0x06020000)&0xFFFF)==0x5678, String.format("0x%04X",bus.read16(0x06020000)&0xFFFF));

        System.out.println("\n=========================================");
        System.out.println("  VRAM MIRROR: " + passed + " PASARON, " + failed + " FALLARON");
        System.out.println("=========================================");
        if (failed > 0) System.exit(1);
    }
}
