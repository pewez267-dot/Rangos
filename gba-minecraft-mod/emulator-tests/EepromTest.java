import com.gbaminecraft.emulator.memory.Eeprom;

/**
 * Tests the EEPROM serial protocol: write a 64-bit value to an address, then
 * read it back, exercising the write ("10"+addr+data+0) and read
 * ("11"+addr+0, then 4 dummy + 64 data) sequences.
 */
public class EepromTest {
    static int passed=0, failed=0;

    public static void main(String[] a){
        test512B();
        test8K();
        System.out.println("\n=========================================");
        System.out.println("  EEPROM: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-30s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-30s %s%n",n,d);}
    }

    static void writeBits(Eeprom e, long val, int n){
        for(int i=n-1;i>=0;i--) e.writeBit((int)((val>>i)&1));
    }

    static long doWrite(Eeprom e, int addr, int addrBits, long data){
        // "10" + addr + 64 data + "0"
        writeBits(e, 0b10, 2);
        writeBits(e, addr, addrBits);
        writeBits(e, data, 64);
        e.writeBit(0);
        return data;
    }

    static long doRead(Eeprom e, int addr, int addrBits){
        // "11" + addr + "0"
        writeBits(e, 0b11, 2);
        writeBits(e, addr, addrBits);
        e.writeBit(0);
        // read 4 dummy + 64 data bits
        long v=0;
        for(int i=0;i<4;i++) e.readBit();
        for(int i=0;i<64;i++) v=(v<<1)|(e.readBit()&1);
        return v;
    }

    static void test512B(){
        System.out.println("[EEPROM 512B (6-bit addr)]");
        Eeprom e=new Eeprom(Eeprom.Size.K4);
        long data=0x0123456789ABCDEFL;
        doWrite(e, 5, 6, data);
        long back=doRead(e, 5, 6);
        check("write/read addr 5", back==data, String.format("0x%016X",back));
        // otra direccion sigue en 0xFF...
        long other=doRead(e, 7, 6);
        check("addr 7 sin escribir = FFFF...", other==0xFFFFFFFFFFFFFFFFL, String.format("0x%016X",other));
    }

    static void test8K(){
        System.out.println("[EEPROM 8KB (14-bit addr)]");
        Eeprom e=new Eeprom(Eeprom.Size.K64);
        long d1=0xDEADBEEFCAFEBABEL, d2=0x1122334455667788L;
        doWrite(e, 100, 14, d1);
        doWrite(e, 1000, 14, d2);
        long b1=doRead(e, 100, 14);
        long b2=doRead(e, 1000, 14);
        check("addr 100", b1==d1, String.format("0x%016X",b1));
        check("addr 1000", b2==d2, String.format("0x%016X",b2));
    }
}
