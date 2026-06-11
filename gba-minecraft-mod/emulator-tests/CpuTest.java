import com.gbaminecraft.emulator.cpu.ARM7TDMI;
import com.gbaminecraft.emulator.memory.MemoryBus;

import java.lang.reflect.Field;

/**
 * Headless ARM7TDMI test harness.
 *
 * We place hand-assembled ARM/Thumb machine code into IWRAM (0x03000000) and
 * point the CPU there, then single-step and assert on register results. This
 * verifies the real execution path (fetch/decode/execute/PC) without Minecraft.
 */
public class CpuTest {

    static int passed = 0, failed = 0;
    static MemoryBus bus;
    static ARM7TDMI cpu;
    static Field regsF, cpsrF;

    static final int BASE = 0x03000000;

    public static void main(String[] args) throws Exception {
        regsF = ARM7TDMI.class.getField("regs");
        cpsrF = ARM7TDMI.class.getField("cpsr");

        testMovImm();
        testAddSub();
        testDataChain();
        testBranch();
        testBL();
        testLdrStr();
        testStmLdm();
        testThumbBasic();
        testThumbBranch();
        testConditionFlags();
        testMultiply();
        testLoopCountdown();

        System.out.println("\n=========================================");
        System.out.println("  RESULTADO: " + passed + " PASARON, " + failed + " FALLARON");
        System.out.println("=========================================");
        if (failed > 0) System.exit(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────
    static void fresh() throws Exception {
        bus = new MemoryBus();
        cpu = new ARM7TDMI(bus);
        setReg(15, BASE);
        setReg(13, 0x03007F00);
        // ensure ARM state
        cpsrF.setInt(cpu, 0x1F);
    }

    static int[] regs() throws Exception { return (int[]) regsF.get(cpu); }
    static void setReg(int i, int v) throws Exception { regs()[i] = v; }
    static int getReg(int i) throws Exception { return regs()[i]; }

    static void writeArm(int addr, int... words) {
        for (int i = 0; i < words.length; i++) bus.write32(addr + i*4, words[i]);
    }
    static void writeThumb(int addr, int... halfs) {
        for (int i = 0; i < halfs.length; i++) bus.write16(addr + i*2, (short) halfs[i]);
    }

    static void run(int steps) { for (int i = 0; i < steps; i++) cpu.step(); }

    static void check(String name, int actual, int expected) {
        if (actual == expected) { passed++; System.out.printf("  OK  %-28s = 0x%08X%n", name, actual); }
        else { failed++; System.out.printf("  XX  %-28s = 0x%08X (esperado 0x%08X)%n", name, actual, expected); }
    }

    // ── tests ────────────────────────────────────────────────────────────
    // MOV R0, #0x12 ; MOV R1, #0xFF
    static void testMovImm() throws Exception {
        fresh();
        System.out.println("[MOV inmediato]");
        writeArm(BASE,
            0xE3A00012,   // mov r0, #0x12
            0xE3A010FF);  // mov r1, #0xFF
        run(2);
        check("R0", getReg(0), 0x12);
        check("R1", getReg(1), 0xFF);
    }

    // MOV R0,#10 ; MOV R1,#3 ; ADD R2,R0,R1 ; SUB R3,R0,R1
    static void testAddSub() throws Exception {
        fresh();
        System.out.println("[ADD / SUB] (el bug critico que no escribia resultado)");
        writeArm(BASE,
            0xE3A0000A,   // mov r0,#10
            0xE3A01003,   // mov r1,#3
            0xE0802001,   // add r2,r0,r1
            0xE0403001);  // sub r3,r0,r1
        run(4);
        check("R2 (10+3)", getReg(2), 13);
        check("R3 (10-3)", getReg(3), 7);
    }

    // chain: r0=5; r0=r0+r0 (=10); r0=r0<<1 via mov shifted
    static void testDataChain() throws Exception {
        fresh();
        System.out.println("[Cadena de data-processing]");
        writeArm(BASE,
            0xE3A00005,   // mov r0,#5
            0xE0800000,   // add r0,r0,r0  -> 10
            0xE1A00080);  // mov r0, r0, lsl #1 -> 20
        run(3);
        check("R0 ((5+5)<<1)", getReg(0), 20);
    }

    // B forward over a poison instruction
    static void testBranch() throws Exception {
        fresh();
        System.out.println("[B salto incondicional]");
        // b at BASE+0: target = (BASE+0+8) + (imm<<2). For target=BASE+12 (skip poison), imm=1.
        writeArm(BASE,
            0xEA000001,   // b +1 -> target = BASE+8+4 = BASE+0x0C
            0xE3A000FF,   // mov r0,#0xFF (debe saltarse) BASE+4
            0xE3A000FF,   // mov r0,#0xFF (debe saltarse) BASE+8
            0xE3A00002);  // mov r0,#2  <- target BASE+0x0C
        run(2); // b, then target mov
        check("R0 (salto correcto)", getReg(0), 2);
    }

    // BL then verify LR, then return via BX LR
    static void testBL() throws Exception {
        fresh();
        System.out.println("[BL + retorno BX LR]");
        // bl at BASE: target = (BASE+8) + (imm<<2). For target=BASE+0x10, imm=2.
        writeArm(BASE,
            0xEB000002,   // BASE+0:  bl -> BASE+0x10
            0xE3A00009,   // BASE+4:  mov r0,#9 (retorno)
            0xE1A00000,   // BASE+8:  nop
            0xE1A00000);  // BASE+0xC: nop
        writeArm(BASE + 0x10, 0xE3A0100C); // BASE+0x10: mov r1,#0x0C (subrutina)
        writeArm(BASE + 0x14, 0xE12FFF1E); // BASE+0x14: bx lr
        run(1); // bl
        check("LR tras BL", getReg(14), BASE + 4);
        check("PC tras BL (subrutina)", getReg(15), BASE + 0x10);
        run(1); // mov r1 (BASE+0x10)
        check("R1 en subrutina", getReg(1), 0x0C);
        run(1); // bx lr (BASE+0x14)
        check("PC retornó a LR", getReg(15) & ~3, BASE + 4);
    }

    // STR/LDR roundtrip through IWRAM
    static void testLdrStr() throws Exception {
        fresh();
        System.out.println("[STR / LDR]");
        setReg(4, 0x03001000);
        writeArm(BASE,
            0xE3A05ABC,   // mov r5, #0xABC00 ? -> actually #0xABC ror... use simpler
            0xE5845000,   // str r5,[r4]
            0xE5946000);  // ldr r6,[r4]
        // 0xE3A05ABC: mov r5, #0xABC000? rotate. Keep it simple: set r5 directly.
        setReg(5, 0x12345678);
        // overwrite first instr with a nop (mov r0,r0)
        writeArm(BASE, 0xE1A00000);
        run(3);
        check("R6 (LDR tras STR)", getReg(6), 0x12345678);
        check("mem[0x03001000]", bus.read32(0x03001000), 0x12345678);
    }

    // STMIA / LDMIA
    static void testStmLdm() throws Exception {
        fresh();
        System.out.println("[STMIA! / LDMIA]");
        setReg(0, 0x11111111);
        setReg(1, 0x22222222);
        setReg(2, 0x33333333);
        setReg(4, 0x03002000);
        // 0xE8A40007 = STMIA r4!,{r0,r1,r2}  (P=0,U=1,W=1,L=0)
        writeArm(BASE, 0xE8A40007);
        run(1); // stmia
        check("mem[0x2000]", bus.read32(0x03002000), 0x11111111);
        check("mem[0x2004]", bus.read32(0x03002004), 0x22222222);
        check("mem[0x2008]", bus.read32(0x03002008), 0x33333333);
        check("R4 writeback", getReg(4), 0x03002000 + 12);

        // Now LDMIA r4,{r5,r6,r7}: write data directly to the SAME bus first
        bus.write32(0x03003000, 0x11111111);
        bus.write32(0x03003004, 0x22222222);
        bus.write32(0x03003008, 0x33333333);
        setReg(4, 0x03003000);
        setReg(15, BASE + 0x40);
        // 0xE89400E0 = LDMIA r4,{r5,r6,r7}  (P=0,U=1,W=0,L=1)
        writeArm(BASE + 0x40, 0xE89400E0);
        run(1);
        check("R5 (LDMIA)", getReg(5), 0x11111111);
        check("R6 (LDMIA)", getReg(6), 0x22222222);
        check("R7 (LDMIA)", getReg(7), 0x33333333);
    }

    // Thumb: MOV/ADD immediate
    static void testThumbBasic() throws Exception {
        fresh();
        System.out.println("[Thumb MOV/ADD inmediato]");
        cpsrF.setInt(cpu, 0x1F | (1 << 5)); // set T bit
        writeThumb(BASE,
            0x2007,   // mov r0, #7
            0x3005,   // add r0, #5  -> 12
            0x2103);  // mov r1, #3
        run(3);
        check("R0 (7+5)", getReg(0), 12);
        check("R1", getReg(1), 3);
    }

    // Thumb unconditional branch
    static void testThumbBranch() throws Exception {
        fresh();
        System.out.println("[Thumb B incondicional]");
        cpsrF.setInt(cpu, 0x1F | (1 << 5));
        // b at BASE+2: target = (BASE+2+4) + (imm<<1). For target=BASE+8, imm=1.
        writeThumb(BASE,
            0x2001,   // mov r0,#1   BASE+0
            0xE001,   // b +1 -> BASE+6+2 = BASE+8   BASE+2
            0x20FF,   // mov r0,#0xFF (saltada) BASE+4
            0x20FF,   // mov r0,#0xFF (saltada) BASE+6
            0x2002);  // mov r0,#2 <- target BASE+8
        run(3); // mov, b, target mov
        check("R0 (Thumb salto)", getReg(0), 2);
    }

    // Condition flags: CMP then conditional move
    static void testConditionFlags() throws Exception {
        fresh();
        System.out.println("[Flags: CMP + MOVEQ/MOVNE]");
        writeArm(BASE,
            0xE3A00005,   // mov r0,#5
            0xE3500005,   // cmp r0,#5    -> Z=1
            0x03A0100A,   // moveq r1,#0x0A (debe ejecutar)
            0x13A0200B);  // movne r2,#0x0B (no debe)
        run(4);
        check("R1 (MOVEQ tras Z=1)", getReg(1), 0x0A);
        check("R2 (MOVNE no ejecuta)", getReg(2), 0x00);
    }

    static void testMultiply() throws Exception {
        fresh();
        System.out.println("[MUL]");
        setReg(0, 7);
        setReg(1, 6);
        writeArm(BASE, 0xE0020091); // mul r2,r1,r0  (rd=r2,rm=r1,rs=r0) enc: rd[19:16],rs[11:8],rm[3:0]
        // MUL encoding: cond 0000 00AS rd rn rs 1001 rm. rd=2,rs=0,rm=1 -> 0xE0020091
        run(1);
        check("R2 (7*6)", getReg(2), 42);
    }

    // Real loop: countdown r0 from 5 to 0 using SUBS + BNE
    static void testLoopCountdown() throws Exception {
        fresh();
        System.out.println("[Bucle real: SUBS + BNE countdown]");
        setReg(0, 5);
        setReg(1, 0);
        writeArm(BASE,
            0xE2811001,   // loop: add r1,r1,#1
            0xE2500001,   //       subs r0,r0,#1
            0x1AFFFFFC);  //       bne loop  (offset -4 -> back to loop)
        // bne offset: target=(cur+8)+(imm<<2). imm=0xFFFFFC sign-ext=-4 -> back 16? compute:
        // instr at BASE+8, cur+8=BASE+16, +(-4<<2=-16) => BASE+0 = loop. good.
        run(200); // plenty
        check("R0 (cuenta a 0)", getReg(0), 0);
        check("R1 (iteraciones)", getReg(1), 5);
    }
}
