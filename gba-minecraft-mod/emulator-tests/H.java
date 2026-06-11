import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;
import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.emulator.apu.APU;
import com.gbaminecraft.emulator.timer.TimerController;
import com.gbaminecraft.emulator.dma.DMAController;
import com.gbaminecraft.emulator.input.GBAInput;
import java.lang.reflect.*;
import java.nio.file.*;
public class H {
    public GBAEmulator emu; public MemoryBus bus; public ARM7TDMI cpu; public PPU ppu;
    public APU apu; public TimerController timers; public DMAController dma; public GBAInput input;
    Method ppuTick,apuTick,timTick,vblEdge,newFrame,keyIrq,dmaVbl,hblEdge,dmaHbl,getFb;
    public long maxRomPc=0;
    public H(String romPath) throws Exception {
        byte[] rom = Files.readAllBytes(Paths.get(romPath));
        emu = new GBAEmulator(); emu.loadROM(rom, "E");
        bus=(MemoryBus)fld("bus"); cpu=(ARM7TDMI)fld("cpu"); ppu=(PPU)fld("ppu");
        apu=(APU)fld("apu"); timers=(TimerController)fld("timers"); dma=(DMAController)fld("dma"); input=(GBAInput)fld("input");
        ppuTick=PPU.class.getMethod("tick",int.class); apuTick=APU.class.getMethod("tick",int.class);
        timTick=TimerController.class.getMethod("tick",int.class); vblEdge=PPU.class.getMethod("pollVBlankEdge");
        newFrame=PPU.class.getMethod("pollNewFrame"); keyIrq=GBAInput.class.getMethod("checkKeyInterrupt");
        dmaVbl=DMAController.class.getMethod("onVBlank"); getFb=PPU.class.getMethod("getFramebuffer");
        try{hblEdge=PPU.class.getMethod("pollHBlankEdge");dmaHbl=DMAController.class.getMethod("onHBlank");}catch(Exception e){}
    }
    Object fld(String n) throws Exception { Field f=GBAEmulator.class.getDeclaredField(n); f.setAccessible(true); return f.get(emu); }
    public void frame() throws Exception {
        int left=GBAEmulator.CYCLES_PER_FRAME;
        while(left>0){
            if(bus.isIRQPending())cpu.triggerIRQ();
            int cyc=cpu.halted?4:cpu.step()*4;
            ppuTick.invoke(ppu,cyc); apuTick.invoke(apu,cyc); timTick.invoke(timers,cyc); bus.tickSerial(cyc);
            if((Boolean)vblEdge.invoke(ppu)) dmaVbl.invoke(dma);
            if(hblEdge!=null && (Boolean)hblEdge.invoke(ppu)) dmaHbl.invoke(dma);
            if((Boolean)keyIrq.invoke(input)) bus.requestInterrupt(1<<12);
            left-=cyc;
        }
        newFrame.invoke(ppu);
    }
    public void frames(int n) throws Exception { for(int i=0;i<n;i++){ frame(); trackMax(); } }
    public int[] fb() throws Exception { return (int[])getFb.invoke(ppu); }
    public int r16(int a){ return bus.read16(a)&0xFFFF; }
    public int dispcnt(){ return r16(0x04000000); }
    public void press(int k){ input.press(k); }
    public void release(int k){ input.release(k); }
    public void tap(int k,int hold,int after) throws Exception { input.press(k); frames(hold); input.release(k); frames(after); }
    public void pressStart() throws Exception { tap(GBAInput.KEY_START,6,6); }
    public int fbColors() throws Exception { int[] f=fb(); java.util.HashSet<Integer> c=new java.util.HashSet<>(); for(int p:f){c.add(p&0xFFFFFF); if(c.size()>900)break;} return c.size(); }
    public void trackMax(){ int pc=cpu.getPC(); if(pc>=0x08000000&&pc<0x0A000000&&(pc&0xFFFFFFFFL)>maxRomPc)maxRomPc=pc&0xFFFFFFFFL; }
    public int fbHash() throws Exception { return java.util.Arrays.hashCode(fb()); }
    public void ascii() throws Exception {
        int[] f=fb();
        for(int yy=0; yy<160; yy+=4){ StringBuilder sb=new StringBuilder();
            for(int xx=0; xx<240; xx+=4){ int p=f[yy*240+xx]&0xFFFFFF; int lum=((p&0xFF)+((p>>8)&0xFF)+((p>>16)&0xFF))/3;
                sb.append(lum<32?' ':lum<96?'.':lum<160?'o':lum<224?'O':'#'); } System.out.println(sb); }
    }
}
