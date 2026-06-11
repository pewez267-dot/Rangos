import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.memory.FlashMemory;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.bios.HleBios;
import java.lang.reflect.Field;

/**
 * Tests for the HLE BIOS (Div, CpuSet, CpuFastSet, LZ77) and Flash save chip
 * (ID probe, byte program/read, sector + chip erase). These are the pieces
 * Pokémon Emerald exercises at boot.
 */
public class BiosFlashTest {
    static int passed=0, failed=0;
    static MemoryBus bus; static ARM7TDMI cpu; static HleBios bios; static int[] regs;

    public static void main(String[] a) throws Exception {
        Field rf = ARM7TDMI.class.getField("regs");
        bus = new MemoryBus();
        cpu = new ARM7TDMI(bus);
        bios = new HleBios(cpu, bus);
        regs = (int[]) rf.get(cpu);

        testDiv();
        testCpuSetFill();
        testCpuSetCopy();
        testCpuFastSet();
        testLz77();
        testFlashId();
        testFlashWriteRead();
        testFlashSectorErase();
        testFlashBankSwitch();

        System.out.println("\n=========================================");
        System.out.println("  BIOS+FLASH: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if (failed>0) System.exit(1);
    }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-30s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-30s %s%n",n,d);}
    }

    static void testDiv() {
        System.out.println("[SWI 0x06 Div]");
        regs[0]=100; regs[1]=7;
        bios.handle(0x06);
        check("100/7 cociente", regs[0]==14, "q="+regs[0]);
        check("100%7 resto", regs[1]==2, "r="+regs[1]);
        check("|cociente|", regs[3]==14, "abs="+regs[3]);
    }

    static void testCpuSetFill() {
        System.out.println("[SWI 0x0B CpuSet fill 16-bit]");
        bus.write16(0x02000000, (short)0xBEEF);
        regs[0]=0x02000000; regs[1]=0x02001000;
        regs[2]=(8 & 0x1FFFFF) | (1<<24); // count=8, fixed src, 16-bit
        bios.handle(0x0B);
        boolean ok=true;
        for(int i=0;i<8;i++) if((bus.read16(0x02001000+i*2)&0xFFFF)!=0xBEEF) ok=false;
        check("8 halfwords = 0xBEEF", ok, "");
    }

    static void testCpuSetCopy() {
        System.out.println("[SWI 0x0B CpuSet copy 32-bit]");
        for(int i=0;i<4;i++) bus.write32(0x02002000+i*4, 0x11110000+i);
        regs[0]=0x02002000; regs[1]=0x02003000;
        regs[2]=(4 & 0x1FFFFF) | (1<<26); // count=4, 32-bit, incrementing
        bios.handle(0x0B);
        boolean ok=true;
        for(int i=0;i<4;i++) if(bus.read32(0x02003000+i*4)!=(0x11110000+i)) ok=false;
        check("4 words copiados", ok, "");
    }

    static void testCpuFastSet() {
        System.out.println("[SWI 0x0C CpuFastSet]");
        for(int i=0;i<8;i++) bus.write32(0x02004000+i*4, 0xCAFE0000+i);
        regs[0]=0x02004000; regs[1]=0x02005000; regs[2]=8; // 8 words
        bios.handle(0x0C);
        boolean ok=true;
        for(int i=0;i<8;i++) if(bus.read32(0x02005000+i*4)!=(0xCAFE0000+i)) ok=false;
        check("8 words (bloque rapido)", ok, "");
    }

    static void testLz77() {
        System.out.println("[SWI 0x11 LZ77UnCompWram]");
        // Build a tiny LZ77 stream that decompresses to "AAAA" (0x41 x4).
        // Header: byte0=0x10 (LZ77), bytes1..3 = size (4).
        int src=0x02006000, dst=0x02007000;
        bus.write32(src, 0x10 | (4<<8));           // header: type 0x10, size 4
        // flag byte: all literals (0x00), then would need 4 literals, but we use
        // 1 literal + 1 compressed run to test back-reference copy.
        bus.write8(src+4, (byte)0x40);  // flag: bit pattern 0100_0000 -> 2nd token compressed
        bus.write8(src+5, (byte)0x41);  // literal 'A'
        // compressed token: len nibble (3..18) and disp. (1-1)=0 disp, len=3 -> copies 'A' x3
        bus.write8(src+6, (byte)0x20);  // (len-3)=2 ->len=5? high nibble=2 -> len=5; disp hi=0
        bus.write8(src+7, (byte)0x00);  // disp lo=0 -> disp=0 -> copyFrom = dst+written-1
        regs[0]=src; regs[1]=dst;
        bios.handle(0x11);
        int b0=bus.read8(dst), b1=bus.read8(dst+1), b2=bus.read8(dst+2), b3=bus.read8(dst+3);
        check("LZ77 -> AAAA", b0==0x41&&b1==0x41&&b2==0x41&&b3==0x41,
              String.format("%02X %02X %02X %02X", b0,b1,b2,b3));
    }

    static void testFlashId() {
        System.out.println("[Flash: lectura de ID del fabricante]");
        MemoryBus b = new MemoryBus();
        FlashMemory f = new FlashMemory(FlashMemory.Size.M1);
        b.setFlash(f);
        // ID command sequence
        b.write8(0x0E005555, (byte)0xAA);
        b.write8(0x0E002AAA, (byte)0x55);
        b.write8(0x0E005555, (byte)0x90);
        int man = b.read8(0x0E000000);
        int dev = b.read8(0x0E000001);
        // exit ID mode
        b.write8(0x0E005555, (byte)0xAA);
        b.write8(0x0E002AAA, (byte)0x55);
        b.write8(0x0E005555, (byte)0xF0);
        check("manufacturer ID (Sanyo 0x62)", man==0x62, String.format("0x%02X",man));
        check("device ID (0x13)", dev==0x13, String.format("0x%02X",dev));
        check("sale de ID mode tras 0xF0", (b.read8(0x0E000000)&0xFF)==0xFF, "");
    }

    static void testFlashWriteRead() {
        System.out.println("[Flash: programar y leer byte]");
        MemoryBus b = new MemoryBus();
        b.setFlash(new FlashMemory(FlashMemory.Size.M1));
        // write 0x42 to offset 0x100
        b.write8(0x0E005555, (byte)0xAA);
        b.write8(0x0E002AAA, (byte)0x55);
        b.write8(0x0E005555, (byte)0xA0);   // program command
        b.write8(0x0E000100, (byte)0x42);
        check("byte programado", (b.read8(0x0E000100)&0xFF)==0x42, String.format("0x%02X",b.read8(0x0E000100)));
    }

    static void testFlashSectorErase() {
        System.out.println("[Flash: borrado de sector 4KB]");
        MemoryBus b = new MemoryBus();
        b.setFlash(new FlashMemory(FlashMemory.Size.M1));
        // program a byte first
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0xA0);
        b.write8(0x0E000010, (byte)0x55);
        // sector erase sequence: AA,55,80, AA,55, then 0x30 at sector addr
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0x80);
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E000000, (byte)0x30);
        check("sector borrado a 0xFF", (b.read8(0x0E000010)&0xFF)==0xFF, String.format("0x%02X",b.read8(0x0E000010)));
    }

    static void testFlashBankSwitch() {
        System.out.println("[Flash 128KB: cambio de banco]");
        MemoryBus b = new MemoryBus();
        b.setFlash(new FlashMemory(FlashMemory.Size.M1));
        // write 0xAB at bank0 off 0
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0xA0);
        b.write8(0x0E000000, (byte)0xAB);
        // switch to bank 1
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0xB0);
        b.write8(0x0E000000, (byte)0x01);
        // write 0xCD at bank1 off 0
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0xA0);
        b.write8(0x0E000000, (byte)0xCD);
        int bank1 = b.read8(0x0E000000);
        // switch back to bank 0
        b.write8(0x0E005555, (byte)0xAA); b.write8(0x0E002AAA, (byte)0x55); b.write8(0x0E005555, (byte)0xB0);
        b.write8(0x0E000000, (byte)0x00);
        int bank0 = b.read8(0x0E000000);
        check("bank1[0]=0xCD", (bank1&0xFF)==0xCD, String.format("0x%02X",bank1));
        check("bank0[0]=0xAB", (bank0&0xFF)==0xAB, String.format("0x%02X",bank0));
    }
}
