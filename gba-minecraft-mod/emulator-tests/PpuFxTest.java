import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.input.GBAInput;
import com.gbaminecraft.emulator.timer.TimerController;
import com.gbaminecraft.emulator.dma.DMAController;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;

/**
 * Tests for the new PPU features: alpha blending, brightness (fade), and
 * window clipping. These are what games use for menus, text boxes and fades.
 */
public class PpuFxTest {
    static int passed=0, failed=0;

    static PPU makePpu(MemoryBus bus) {
        PPU ppu=new PPU(bus);
        bus.connectSubsystems(ppu, new APU(bus), new GBAInput(),
                new TimerController(bus), new DMAController(bus), new ARM7TDMI(bus));
        ppu.reset();
        return ppu;
    }
    static int c15(int r,int g,int b){ return (r&0x1F)|((g&0x1F)<<5)|((b&0x1F)<<10); }
    static void runFrame(PPU p){ for(int i=0;i<PPU.LINES_PER_FRAME;i++) p.tick(PPU.CYCLES_PER_LINE); }
    static void check(String n, boolean ok, String d){
        if(ok){passed++;System.out.printf("  OK  %-34s %s%n",n,d);}
        else{failed++;System.out.printf("  XX  %-34s %s%n",n,d);}
    }

    public static void main(String[] a){
        testBrightnessFadeWhite();
        testBrightnessFadeBlack();
        testWindowClip();
        System.out.println("\n=========================================");
        System.out.println("  PPU FX: "+passed+" PASARON, "+failed+" FALLARON");
        System.out.println("=========================================");
        if(failed>0) System.exit(1);
    }

    // Mode 0, BG0 con un tile rojo, brillo hacia blanco al maximo -> blanco
    static void testBrightnessFadeWhite(){
        System.out.println("[Brillo hacia blanco (BLDY) sobre BG0]");
        MemoryBus bus=new MemoryBus(); PPU p=makePpu(bus);
        setupSolidBG0(bus, c15(31,0,0));        // rojo
        // BLDCNT: effect=2 (brighten), target1 includes BG0 (bit0)
        bus.write16(0x04000050, (short)((2<<6) | 0x01));
        bus.write16(0x04000054, (short)(16));            // BLDY=16 (max) -> blanco
        runFrame(p);
        int px=p.getFramebuffer()[0]&0xFFFFFF;
        check("rojo + fade-white(max) = blanco", px==0xF8F8F8, String.format("0x%06X",px));
    }

    static void testBrightnessFadeBlack(){
        System.out.println("[Brillo hacia negro (BLDY) sobre BG0]");
        MemoryBus bus=new MemoryBus(); PPU p=makePpu(bus);
        setupSolidBG0(bus, c15(0,31,0));        // verde
        bus.write16(0x04000050, (short)((3<<6) | 0x01)); // effect=3 darken, target1 BG0
        bus.write16(0x04000054, (short)(16));            // BLDY=16 -> negro
        runFrame(p);
        int px=p.getFramebuffer()[0]&0xFFFFFF;
        check("verde + fade-black(max) = negro", px==0x000000, String.format("0x%06X",px));
    }

    // Ventana WIN0 que solo deja ver BG0 dentro de x[0,120); fuera, backdrop.
    static void testWindowClip(){
        System.out.println("[Ventana WIN0 recorta BG0]");
        MemoryBus bus=new MemoryBus(); PPU p=makePpu(bus);
        setupSolidBG0(bus, c15(0,0,31));        // azul
        // backdrop (palette 0) = rojo, para distinguir el "fuera de ventana"
        bus.write16(0x05000000, (short)c15(31,0,0));
        // Habilita WIN0 (DISPCNT bit13) ademas de BG0 (bit8)
        int dispcnt = bus.read16(0x04000000);
        bus.write16(0x04000000, (short)(dispcnt | (1<<13)));
        // WIN0H: derecha=120, izquierda=0  (formato: [15:8]=left,[7:0]=right)
        bus.write16(0x04000040, (short)((0<<8) | 120));
        // WIN0V: top=0, bottom=160
        bus.write16(0x04000044, (short)((0<<8) | 160));
        // WININ: dentro de WIN0 habilita BG0 (bit0). WINOUT: fuera no habilita nada.
        bus.write16(0x04000048, (short)(0x0001));
        bus.write16(0x0400004A, (short)(0x0000));
        runFrame(p);
        int inside  = p.getFramebuffer()[10]&0xFFFFFF;   // x=10 dentro -> azul
        int outside = p.getFramebuffer()[200]&0xFFFFFF;  // x=200 fuera -> backdrop rojo
        check("dentro de ventana = azul BG0", inside==0x0000F8, String.format("0x%06X",inside));
        check("fuera de ventana = backdrop", outside==0xF80000, String.format("0x%06X",outside));
    }

    // Configura Mode 0 con BG0 lleno de un color (tile 0, 256-color, mapa en 0)
    static void setupSolidBG0(MemoryBus bus, int color15){
        // DISPCNT: mode 0, BG0 on (bit8)
        bus.write16(0x04000000, (short)(0 | (1<<8)));
        // BG0CNT: charBase=0, screenBase=block 8 (0x4000), 256-color (bit7), prio 0
        int screenBlock=8;
        bus.write16(0x04000008, (short)((screenBlock<<8) | (1<<7)));
        // paleta: indice 1 = color
        bus.write16(0x05000000 + 1*2, (short)color15);
        // tile 0 en charBase 0: 64 bytes, todos = indice 1
        for(int i=0;i<64;i++) bus.write8(0x06000000 + i, (byte)1);
        // mapa en screenBlock 8 (0x06000000 + 8*0x800 = 0x06004000): todas las entradas = tile 0
        int mapBase=0x06000000 + screenBlock*0x800;
        for(int i=0;i<32*32;i++) bus.write16(mapBase + i*2, (short)0x0000); // tile 0, pal bank 0
    }
}
