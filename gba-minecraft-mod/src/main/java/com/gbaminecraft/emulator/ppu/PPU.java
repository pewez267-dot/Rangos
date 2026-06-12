package com.gbaminecraft.emulator.ppu;

import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * GBA Picture Processing Unit (PPU).
 * Renders backgrounds (modes 0-5) and sprites (OBJs) to a 240x160 framebuffer.
 * Each pixel is ARGB8888.
 */
public class PPU {

    public static final int SCREEN_WIDTH  = 240;
    public static final int SCREEN_HEIGHT = 160;

    // Timing constants (cycles at ~16.78 MHz)
    public static final int CYCLES_PER_DOT    = 4;
    public static final int DOTS_PER_LINE     = 308; // 240 visible + 68 HBlank
    public static final int LINES_PER_FRAME   = 228; // 160 visible + 68 VBlank
    public static final int CYCLES_PER_LINE   = DOTS_PER_LINE * CYCLES_PER_DOT;
    public static final int CYCLES_PER_HBLANK = 68 * CYCLES_PER_DOT;
    public static final int CYCLES_PER_FRAME  = DOTS_PER_LINE * LINES_PER_FRAME * CYCLES_PER_DOT;

    // Interrupt bits
    public static final int IRQ_VBLANK = 1 << 0;
    public static final int IRQ_HBLANK = 1 << 1;
    public static final int IRQ_VCOUNT = 1 << 2;

    // Framebuffer (ARGB)
    private final int[] framebuffer  = new int[SCREEN_WIDTH * SCREEN_HEIGHT];
    private final int[] scanlineBuf  = new int[SCREEN_WIDTH];

    // ── Per-pixel composition buffers (rebuilt each scanline) ──────────────
    // For correct priority + alpha blending we keep, for every pixel, the two
    // topmost layers (by priority): their 15-bit color and which layer drew them.
    // Layer ids: 0..3 = BG0..BG3, 4 = OBJ (sprite), 5 = backdrop.
    private final int[]  topColor   = new int[SCREEN_WIDTH];  // 15-bit color of top layer
    private final int[]  topPrio    = new int[SCREEN_WIDTH];  // priority (0=front)
    private final int[]  topLayer   = new int[SCREEN_WIDTH];
    private final int[]  sndColor   = new int[SCREEN_WIDTH];  // 15-bit color of 2nd layer
    private final int[]  sndLayer   = new int[SCREEN_WIDTH];
    private final boolean[] objSemi = new boolean[SCREEN_WIDTH]; // OBJ in semi-transparent mode
    private final int[]  objPrioBuf = new int[SCREEN_WIDTH];     // sprite priority per pixel
    private final boolean[] winMask = new boolean[SCREEN_WIDTH]; // per-pixel: layer enable set
    private final int[]  winLayers  = new int[SCREEN_WIDTH];     // enabled-layers bitmask per pixel (incl. bit5=effects)
    private boolean winActive  = false; // true when any window is active this line
    private boolean anySemiObj = false; // true when a semi-transparent OBJ was plotted this line

    // Memory
    private byte[] vram;
    private byte[] palette;
    private byte[] oam;
    private MemoryBus bus;

    // I/O Registers (mirrored from bus)
    private int DISPCNT  = 0;
    private int DISPSTAT = 0;
    private int VCOUNT   = 0;
    private int[] BG_CNT    = new int[4];
    private int[] BG_HOFS   = new int[4];
    private int[] BG_VOFS   = new int[4];
    private int[] BG_PA     = new int[2]; // BG2, BG3
    private int[] BG_PB     = new int[2];
    private int[] BG_PC     = new int[2];
    private int[] BG_PD     = new int[2];
    private int[] BG_RefX   = new int[2];
    private int[] BG_RefY   = new int[2];
    private int   WIN0H, WIN0V, WIN1H, WIN1V;
    private int   WININ, WINOUT;
    private int   BLDCNT, BLDALPHA, BLDY;
    private int   MOSAIC;

    // Internal affine reference points (latched at VBlank)
    private int[] affineRefX = new int[2];
    private int[] affineRefY = new int[2];

    // Cycle counter
    private int cycleCnt = 0;
    private int scanline  = 0;
    private boolean inHBlank = false;   // true once we've crossed into the HBlank portion of the line

    private boolean newFrame = false;
    private boolean vblankEdge = false; // set once when entering VBlank (for DMA/observers)
    private boolean hblankEdge = false; // set once per visible-line HBlank (for HBlank DMA)

    public PPU(MemoryBus bus) {
        this.bus = bus;
        this.vram    = bus.getVRAM();
        this.palette = bus.getPalette();
        this.oam     = bus.getOAM();
    }

    // ── Main tick ─────────────────────────────────────────────────────────
    // Drives the dot-clock: each line is CYCLES_PER_LINE; the visible portion
    // is the first 240 dots, HBlank is the remaining 68 dots. We fire HBlank
    // exactly once per line and advance the scanline at end-of-line.
    public void tick(int cycles) {
        cycleCnt += cycles;

        // Enter HBlank once we pass the visible dot region of the current line.
        if (!inHBlank && cycleCnt >= SCREEN_WIDTH * CYCLES_PER_DOT) {
            inHBlank = true;
            onEnterHBlank();
        }

        if (cycleCnt >= CYCLES_PER_LINE) {
            cycleCnt -= CYCLES_PER_LINE;
            inHBlank = false;
            advanceScanline();
        }
    }

    private void onEnterHBlank() {
        // HBlank flag + IRQ, only during visible lines (per hardware it also
        // occurs in VBlank lines, but the HBlank IRQ is what games rely on).
        DISPSTAT |= 2;
        if ((DISPSTAT & (1 << 4)) != 0) bus.requestInterrupt(IRQ_HBLANK);
        // HBlank DMA fires during visible lines (scanline 0..159).
        if (scanline < SCREEN_HEIGHT) hblankEdge = true;
    }

    /** True once after each visible-line HBlank; consumed by the main loop to
     *  trigger HBlank-timed DMA (used by Emerald for HUD/raster effects). */
    public boolean pollHBlankEdge() { boolean e = hblankEdge; hblankEdge = false; return e; }

    private void advanceScanline() {
        // Render the line we just finished clocking through (the current one),
        // before moving to the next, so line 0..159 are all drawn.
        if (scanline < SCREEN_HEIGHT) {
            renderScanline(scanline);
            // Advance affine backgrounds per visible line
            affineRefX[0] += (short) BG_PB[0];
            affineRefX[1] += (short) BG_PB[1];
            affineRefY[0] += (short) BG_PD[0];
            affineRefY[1] += (short) BG_PD[1];
        }

        scanline++;

        if (scanline >= LINES_PER_FRAME) {
            scanline = 0;
            // Latch affine reference points at the top of the frame
            affineRefX[0] = BG_RefX[0];
            affineRefX[1] = BG_RefX[1];
            affineRefY[0] = BG_RefY[0];
            affineRefY[1] = BG_RefY[1];
            // OAM is parsed once per frame for sprite rendering; force a refresh.
            spParsed = false;
        }

        VCOUNT = scanline;
        // HBlank flag clears at the start of each new line
        DISPSTAT &= ~2;
        updateDISPSTAT();

        // VBlank starts exactly at line 160
        if (scanline == SCREEN_HEIGHT) {
            newFrame = true;     // a complete visible frame is ready
            vblankEdge = true;   // signal DMA / observers once
        }
    }

    private void updateDISPSTAT() {
        // VBlank flag: set for lines 160..226 (not the last line 227)
        if (scanline >= SCREEN_HEIGHT && scanline < LINES_PER_FRAME - 1) {
            boolean wasSet = (DISPSTAT & 1) != 0;
            DISPSTAT |= 1;
            // Fire the VBlank IRQ only once, on the transition into line 160
            if (!wasSet && scanline == SCREEN_HEIGHT && (DISPSTAT & (1 << 3)) != 0) {
                bus.requestInterrupt(IRQ_VBLANK);
            }
        } else {
            DISPSTAT &= ~1;
        }
        // VCount match
        int lyc = (DISPSTAT >>> 8) & 0xFF;
        if (scanline == lyc) {
            DISPSTAT |= (1 << 2);
            if ((DISPSTAT & (1 << 5)) != 0) bus.requestInterrupt(IRQ_VCOUNT);
        } else {
            DISPSTAT &= ~(1 << 2);
        }
    }

    // Layer ids for the per-pixel compositor
    private static final int LAYER_BG0 = 0, LAYER_BG1 = 1, LAYER_BG2 = 2,
                             LAYER_BG3 = 3, LAYER_OBJ = 4, LAYER_BD = 5;

    /** When set, the PPU still ticks (so VBlank/HBlank IRQs and DMAs fire on
     *  schedule and the game logic + audio stay in sync), but renderScanline
     *  becomes a no-op for visible lines. Used by the frame-skip heuristic to
     *  recover frame rate on slow machines without breaking timing. */
    private boolean skipRender = false;
    public void setSkipRender(boolean v) { this.skipRender = v; }
    public boolean isSkipRender() { return skipRender; }

    // ── Scanline render ────────────────────────────────────────────────────
    private void renderScanline(int y) {
        if (skipRender) return;
        int bgMode = DISPCNT & 0x7;
        boolean display = (DISPCNT & (1 << 7)) == 0; // forced blank when bit7 set
        int fbBase = y * SCREEN_WIDTH;

        if (!display) {
            for (int x = 0; x < SCREEN_WIDTH; x++) framebuffer[fbBase + x] = 0xFFFFFFFF;
            return;
        }

        // Initialize composition buffers with the backdrop (palette entry 0).
        // Arrays.fill is a JIT intrinsic and is markedly faster than a manual
        // per-pixel loop — and this runs on every one of the 160 visible lines.
        int backdrop = readPalette15(0);
        java.util.Arrays.fill(topColor, backdrop);
        java.util.Arrays.fill(topPrio, 4);
        java.util.Arrays.fill(topLayer, LAYER_BD);
        java.util.Arrays.fill(sndColor, backdrop);
        java.util.Arrays.fill(sndLayer, LAYER_BD);
        java.util.Arrays.fill(objSemi, false);
        java.util.Arrays.fill(objPrioBuf, 4);
        anySemiObj = false;

        // Build per-pixel window/layer-enable mask.
        computeWindows(y);

        // Backgrounds feed the layer buffers (priority-ordered inside compositor).
        switch (bgMode) {
            case 0: renderMode0(y); break;
            case 1: renderMode1(y); break;
            case 2: renderMode2(y); break;
            case 3: renderMode3(y); break;
            case 4: renderMode4(y); break;
            case 5: renderMode5(y); break;
        }

        // Sprites (OBJ) plot into the layer buffers too.
        if ((DISPCNT & (1 << 12)) != 0) renderSprites(y, bgMode);

        // Composite with alpha blending / brightness effects + windows.
        composite(y, fbBase);
    }

    // ── Palette helpers ────────────────────────────────────────────────────
    /** Reads a palette entry as raw 15-bit BGR (for the compositor). */
    private int readPalette15(int idx) {
        int off = idx * 2;
        if (off + 1 >= palette.length) return 0;
        return ((palette[off] & 0xFF) | ((palette[off + 1] & 0xFF) << 8)) & 0x7FFF;
    }

    // ── Windows ────────────────────────────────────────────────────────────
    /**
     * For each pixel of the line, compute which layers (BG0..3, OBJ, effects)
     * are enabled, taking WIN0/WIN1/OBJ-window/outside into account. When no
     * window is active, all layers enabled by DISPCNT are allowed everywhere.
     */
    private void computeWindows(int y) {
        boolean w0 = (DISPCNT & (1 << 13)) != 0;
        boolean w1 = (DISPCNT & (1 << 14)) != 0;
        boolean wObj = (DISPCNT & (1 << 15)) != 0;
        int dispLayers = ((DISPCNT >> 8) & 0x1F); // BG0..3 + OBJ enable bits

        if (!w0 && !w1 && !wObj) {
            int all = dispLayers | (1 << 5); // all displayed layers + effects
            java.util.Arrays.fill(winLayers, all);
            winActive = false;
            return;
        }
        winActive = true;

        int win0L = (WIN0H >> 8) & 0xFF, win0R = WIN0H & 0xFF;
        int win1L = (WIN1H >> 8) & 0xFF, win1R = WIN1H & 0xFF;
        int win0T = (WIN0V >> 8) & 0xFF, win0B = WIN0V & 0xFF;
        int win1T = (WIN1V >> 8) & 0xFF, win1B = WIN1V & 0xFF;

        boolean in0Row = w0 && inV(y, win0T, win0B);
        boolean in1Row = w1 && inV(y, win1T, win1B);

        int inMask  = (WININ & 0x3F);            // WIN0 layers
        int in1Mask = (WININ >> 8) & 0x3F;       // WIN1 layers
        int outMask = (WINOUT & 0x3F);           // outside layers
        int objMask = (WINOUT >> 8) & 0x3F;      // OBJ-window layers

        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int m;
            if (in0Row && inH(x, win0L, win0R))       m = inMask;
            else if (in1Row && inH(x, win1L, win1R))  m = in1Mask;
            else                                       m = outMask;
            // (OBJ window handled approximately via outside/obj mask in composite)
            winLayers[x] = m & (dispLayers | (1 << 5));
        }
    }

    private static boolean inV(int y, int top, int bottom) {
        if (top <= bottom) return y >= top && y < bottom;
        return y >= top || y < bottom; // wrapped
    }
    private static boolean inH(int x, int left, int right) {
        if (left <= right) return x >= left && x < right;
        return x >= left || x < right;
    }

    // ── Compositor (alpha blending + brightness) ───────────────────────────
    private void composite(int y, int fbBase) {
        int effect = (BLDCNT >> 6) & 0x3;        // 0=none,1=alpha,2=brighten,3=darken
        int t1 = BLDCNT & 0x3F;                  // 1st target layers (incl OBJ bit4, BD bit5)
        int t2 = (BLDCNT >> 8) & 0x3F;           // 2nd target layers
        int eva = Math.min(16, BLDALPHA & 0x1F);
        int evb = Math.min(16, (BLDALPHA >> 8) & 0x1F);
        int evy = Math.min(16, BLDY & 0x1F);

        // Fast path: no colour-special-effect, no semi-transparent sprite and no
        // active window on this line — just convert the top layer straight to
        // ARGB. This is the overwhelmingly common case and skips the per-pixel
        // blend/window branching for all 240 pixels.
        if (effect == 0 && !anySemiObj && !winActive) {
            for (int x = 0; x < SCREEN_WIDTH; x++) {
                framebuffer[fbBase + x] = color15toARGB(topColor[x]);
            }
            return;
        }

        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int topL = topLayer[x];
            int col  = topColor[x];
            boolean effectsAllowed = (winLayers[x] & (1 << 5)) != 0;

            // Semi-transparent OBJ forces alpha blending with the layer below.
            boolean semiObj = (topL == LAYER_OBJ) && objSemi[x];

            if (effectsAllowed && (semiObj || (effect == 1 && isTarget(topL, t1)))) {
                int below = sndColor[x];
                int belowL = sndLayer[x];
                if (semiObj || isTarget(belowL, t2)) {
                    col = blendAlpha(col, below, eva, evb);
                } else if (effect == 1 && !semiObj) {
                    // top is 1st target but nothing valid below -> no blend
                }
            } else if (effectsAllowed && effect == 2 && isTarget(topL, t1)) {
                col = brighten(col, evy);
            } else if (effectsAllowed && effect == 3 && isTarget(topL, t1)) {
                col = darken(col, evy);
            }

            framebuffer[fbBase + x] = color15toARGB(col);
        }
    }

    private static boolean isTarget(int layer, int mask) { return (mask & (1 << layer)) != 0; }

    private static int blendAlpha(int a, int b, int eva, int evb) {
        int ar=a&0x1F, ag=(a>>5)&0x1F, ab=(a>>10)&0x1F;
        int br=b&0x1F, bg=(b>>5)&0x1F, bb=(b>>10)&0x1F;
        int r=Math.min(31,(ar*eva+br*evb)>>4);
        int g=Math.min(31,(ag*eva+bg*evb)>>4);
        int bl=Math.min(31,(ab*eva+bb*evb)>>4);
        return r | (g<<5) | (bl<<10);
    }
    private static int brighten(int c, int evy) {
        int r=c&0x1F, g=(c>>5)&0x1F, b=(c>>10)&0x1F;
        r += ((31-r)*evy)>>4; g += ((31-g)*evy)>>4; b += ((31-b)*evy)>>4;
        return r | (g<<5) | (b<<10);
    }
    private static int darken(int c, int evy) {
        int r=c&0x1F, g=(c>>5)&0x1F, b=(c>>10)&0x1F;
        r -= (r*evy)>>4; g -= (g*evy)>>4; b -= (b*evy)>>4;
        return r | (g<<5) | (b<<10);
    }



    /**
     * Submit a candidate pixel for a layer. Keeps the two front-most layers per
     * pixel (by priority) so the blend stage can mix target1 over target2.
     */
    private void plot(int x, int color15, int prio, int layer) {        // Window: is this layer allowed at this pixel?
        if ((winLayers[x] & (1 << layer)) == 0 && layer != LAYER_BD) return;
        if (prio < topPrio[x] || (prio == topPrio[x] && layer < topLayer[x])) {
            // pushes current top down to second
            sndColor[x] = topColor[x]; sndLayer[x] = topLayer[x];
            topColor[x] = color15; topPrio[x] = prio; topLayer[x] = layer;
        } else if (color15 != -1) {
            // candidate for the second slot (only if it beats current second by priority)
            if (layer != topLayer[x]) { sndColor[x] = color15; sndLayer[x] = layer; }
        }
    }

    /** OBJ plot: sprites carry their own priority and may be semi-transparent. */
    private void plotObj(int x, int color15, int prio, boolean semi) {
        if ((winLayers[x] & (1 << LAYER_OBJ)) == 0) return;
        if (semi) anySemiObj = true;
        // OBJ wins ties against BG of equal priority.
        if (prio < topPrio[x] || (prio == topPrio[x])) {
            if (topLayer[x] != LAYER_OBJ) { sndColor[x] = topColor[x]; sndLayer[x] = topLayer[x]; }
            topColor[x] = color15; topPrio[x] = prio; topLayer[x] = LAYER_OBJ;
            objSemi[x] = semi;
        } else {
            sndColor[x] = color15; sndLayer[x] = LAYER_OBJ;
        }
    }

    // ── Mode 0: four regular BGs ───────────────────────────────────────────
    private void renderMode0(int y) {
        // Render BGs in priority order (lowest priority first)
        for (int priority = 3; priority >= 0; priority--) {
            for (int bg = 3; bg >= 0; bg--) {
                if ((DISPCNT & (1 << (8 + bg))) == 0) continue;
                if ((BG_CNT[bg] & 3) != priority) continue;
                renderRegularBG(bg, y);
            }
        }
    }

    // ── Mode 1: BG0+BG1 regular, BG2 affine ──────────────────────────────
    private void renderMode1(int y) {
        for (int priority = 3; priority >= 0; priority--) {
            for (int bg = 2; bg >= 0; bg--) {
                if ((DISPCNT & (1 << (8 + bg))) == 0) continue;
                if ((BG_CNT[bg] & 3) != priority) continue;
                if (bg == 2) renderAffineBG(0, y);
                else renderRegularBG(bg, y);
            }
        }
    }

    // ── Mode 2: BG2+BG3 affine ────────────────────────────────────────────
    private void renderMode2(int y) {
        for (int priority = 3; priority >= 0; priority--) {
            for (int bg = 3; bg >= 2; bg--) {
                if ((DISPCNT & (1 << (8 + bg))) == 0) continue;
                if ((BG_CNT[bg] & 3) != priority) continue;
                renderAffineBG(bg - 2, y);
            }
        }
    }

    // ── Mode 3: 240x160 15-bit bitmap ─────────────────────────────────────
    private void renderMode3(int y) {
        if ((DISPCNT & (1 << 10)) == 0) return;
        int prio = BG_CNT[2] & 3;
        int base = y * SCREEN_WIDTH * 2;
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int color15 = ((vram[base + x*2] & 0xFF)) | ((vram[base + x*2+1] & 0xFF) << 8);
            plot(x, color15 & 0x7FFF, prio, LAYER_BG2);
        }
    }

    // ── Mode 4: 240x160 8bpp paletted, 2 pages ────────────────────────────
    private void renderMode4(int y) {
        if ((DISPCNT & (1 << 10)) == 0) return;
        int prio = BG_CNT[2] & 3;
        int page = ((DISPCNT & (1 << 4)) != 0) ? 0xA000 : 0;
        int base = page + y * SCREEN_WIDTH;
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int idx = vram[base + x] & 0xFF;
            if (idx != 0) plot(x, readPalette15(idx), prio, LAYER_BG2);
        }
    }

    // ── Mode 5: 160x128 15-bit bitmap, 2 pages ────────────────────────────
    private void renderMode5(int y) {
        if ((DISPCNT & (1 << 10)) == 0) return;
        if (y >= 128) return;
        int page = ((DISPCNT & (1 << 4)) != 0) ? 0xA000 : 0;
        int base = page + y * 160 * 2;
        int prio = BG_CNT[2] & 3;
        for (int x = 0; x < 160; x++) {
            int color15 = (vram[base + x*2] & 0xFF) | ((vram[base + x*2+1] & 0xFF) << 8);
            plot(x, color15 & 0x7FFF, prio, LAYER_BG2);
        }
    }

    // ── Regular BG render ─────────────────────────────────────────────────
    // Reusable buffer holding the 8 decoded texels (palette-resolved 15-bit
    // colour, or -1 for transparent) of the BG tile row currently being drawn,
    // already in screen order (horizontal flip applied). Decoding once per tile
    // instead of once per pixel removes ~8x of the tile-map reads, char reads and
    // palette lookups — and BG rendering was 58% of the PPU's frame cost.
    private final int[] bgRowColor = new int[8];

    private void renderRegularBG(int bg, int y) {
        int cnt      = BG_CNT[bg];
        int screenBase = ((cnt >>> 8) & 0x1F) * 0x800;
        int charBase   = ((cnt >>> 2) & 0x3)  * 0x4000;
        boolean color256 = (cnt & (1 << 7)) != 0;
        int sizeMode   = (cnt >>> 14) & 0x3;
        int prio       = cnt & 3;
        int layer      = bg; // LAYER_BG0..3 == 0..3

        int scrollX = BG_HOFS[bg] & 0x1FF;
        int scrollY = BG_VOFS[bg] & 0x1FF;

        int mapWidth  = (sizeMode & 1) != 0 ? 512 : 256;
        int mapHeight = (sizeMode & 2) != 0 ? 512 : 256;

        boolean mosaicEnabled = (cnt & (1 << 6)) != 0;
        int mosaicW = ((MOSAIC & 0xF)) + 1;
        int mosaicH = ((MOSAIC >> 4) & 0xF) + 1;

        int srcY = y;
        if (mosaicEnabled) srcY -= (y % mosaicH);
        int bgY = (srcY + scrollY) % mapHeight;
        int tileY    = bgY / 8;
        int pixY     = bgY % 8;
        int tileYmod = tileY % 32;
        int tileYblk = tileY / 32;

        int curTileX = -1, curBlock = -1;
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int srcX = mosaicEnabled ? x - (x % mosaicW) : x;
            int bgX  = (srcX + scrollX) % mapWidth;
            int tileX = bgX >> 3;
            int pixX  = bgX & 7;

            int screenBlock;
            if (sizeMode == 1)      screenBlock = tileX / 32;
            else if (sizeMode == 2) screenBlock = tileYblk;
            else if (sizeMode == 3) screenBlock = tileYblk * 2 + (tileX / 32);
            else                    screenBlock = 0;

            if (tileX != curTileX || screenBlock != curBlock) {
                curTileX = tileX; curBlock = screenBlock;
                decodeBgTileRow(screenBase, charBase, color256, screenBlock, tileX, tileYmod, pixY);
            }
            int c = bgRowColor[pixX];
            if (c >= 0) plot(x, c, prio, layer);
        }
    }

    /** Decode the 8 texels of one BG tile row into {@link #bgRowColor} (screen
     *  order, transparent = -1). Mirrors the original per-pixel logic exactly. */
    private void decodeBgTileRow(int screenBase, int charBase, boolean color256,
                                 int screenBlock, int tileX, int tileYmod, int pixY) {
        int mapOff = screenBase + screenBlock * 0x800 + tileYmod * 64 + (tileX % 32) * 2;
        if (mapOff + 1 >= vram.length) { java.util.Arrays.fill(bgRowColor, -1); return; }
        int tileAttr = (vram[mapOff] & 0xFF) | ((vram[mapOff + 1] & 0xFF) << 8);
        int tileNum  = tileAttr & 0x3FF;
        boolean flipH = (tileAttr & (1 << 10)) != 0;
        boolean flipV = (tileAttr & (1 << 11)) != 0;
        int palBank   = (tileAttr >>> 12) & 0xF;
        int pyOff = flipV ? (7 - pixY) : pixY;

        if (color256) {
            int base = charBase + tileNum * 64 + pyOff * 8;
            for (int t = 0; t < 8; t++) {
                int off = base + t;
                int idx = off < vram.length ? (vram[off] & 0xFF) : 0;
                bgRowColor[flipH ? 7 - t : t] = idx != 0 ? readPalette15(idx) : -1;
            }
        } else {
            int base = charBase + tileNum * 32 + pyOff * 4;
            for (int t = 0; t < 8; t++) {
                int off = base + (t >> 1);
                int nib = off < vram.length ? (vram[off] & 0xFF) : 0;
                int idx = (t & 1) != 0 ? (nib >>> 4) : (nib & 0xF);
                bgRowColor[flipH ? 7 - t : t] = idx != 0 ? readPalette15(palBank * 16 + idx) : -1;
            }
        }
    }

    // ── Affine BG render ──────────────────────────────────────────────────
    private void renderAffineBG(int idx, int y) {
        int bg = idx + 2;
        int cnt = BG_CNT[bg];
        int screenBase = ((cnt >>> 8) & 0x1F) * 0x800;
        int charBase   = ((cnt >>> 2) & 0x3)  * 0x4000;
        boolean wrap   = (cnt & (1 << 13)) != 0;
        int sizeMode   = (cnt >>> 14) & 0x3;
        int mapSize    = 128 << sizeMode; // 128, 256, 512, 1024
        int prio       = cnt & 3;
        int layer      = bg;

        int pa = (short) BG_PA[idx];
        int pc = (short) BG_PC[idx];
        int refX = affineRefX[idx];
        int refY = affineRefY[idx];

        for (int x = 0; x < SCREEN_WIDTH; x++) {
            int bgX = (refX + pa * x) >> 8;
            int bgY = (refY + pc * x) >> 8;

            if (wrap) {
                bgX = ((bgX % mapSize) + mapSize) % mapSize;
                bgY = ((bgY % mapSize) + mapSize) % mapSize;
            } else if (bgX < 0 || bgX >= mapSize || bgY < 0 || bgY >= mapSize) {
                continue;
            }

            int tileX = bgX / 8, tileY = bgY / 8;
            int pixX  = bgX % 8, pixY  = bgY % 8;
            int mapOff = screenBase + tileY * (mapSize / 8) + tileX;
            if (mapOff >= vram.length) continue;
            int tileNum = vram[mapOff] & 0xFF;
            int off = charBase + tileNum * 64 + pixY * 8 + pixX;
            int colorIdx = off < vram.length ? (vram[off] & 0xFF) : 0;
            // Affine BGs are 256-color, using the standard BG palette (0..255).
            if (colorIdx != 0) plot(x, readPalette15(colorIdx), prio, layer);
        }
    }

    // ── Sprite render ─────────────────────────────────────────────────────
    private static final int[][] OBJ_SIZE = {
        {8,8},  {16,16}, {32,32}, {64,64},  // Square
        {16,8}, {32,8},  {32,16}, {64,32},  // Horizontal
        {8,16}, {8,32},  {16,32}, {32,64}   // Vertical
    };

    // Pre-parsed OAM cache, rebuilt once at the top of each frame instead of
    // for every scanline. The old code re-decoded all 128 OAM entries 160
    // times per frame (≈20,480 outer iterations) which was the bulk of the
    // sprite cost. After this the per-scanline pass only walks visible sprites.
    private final int[]   spAttr0   = new int[128];
    private final int[]   spAttr1   = new int[128];
    private final int[]   spAttr2   = new int[128];
    private final int[]   spSprY    = new int[128];
    private final int[]   spSprX    = new int[128];
    private final int[]   spW       = new int[128];
    private final int[]   spH       = new int[128];
    private final int[]   spBoxW    = new int[128];
    private final int[]   spBoxH    = new int[128];
    private final boolean[] spLive  = new boolean[128];
    private final boolean[] spAffine= new boolean[128];
    private boolean spParsed = false;

    /** Decode every OAM entry once per frame. Drops hidden, malformed, or
     *  off-screen-Y sprites so the per-scanline loop only walks live ones. */
    private void parseOam() {
        for (int sp = 0; sp < 128; sp++) {
            int oamOff = sp * 8;
            int attr0  = (oam[oamOff]     & 0xFF) | ((oam[oamOff + 1] & 0xFF) << 8);
            int attr1  = (oam[oamOff + 2] & 0xFF) | ((oam[oamOff + 3] & 0xFF) << 8);
            int attr2  = (oam[oamOff + 4] & 0xFF) | ((oam[oamOff + 5] & 0xFF) << 8);
            boolean affine = (attr0 & (1 << 8)) != 0;
            // Non-affine sprites with bit9 set are "hidden".
            if (!affine && (attr0 & (1 << 9)) != 0) { spLive[sp] = false; continue; }

            int shape   = (attr0 >>> 14) & 0x3;
            int size    = (attr1 >>> 14) & 0x3;
            int sizeIdx = shape * 4 + size;
            if (sizeIdx >= OBJ_SIZE.length) { spLive[sp] = false; continue; }

            int w = OBJ_SIZE[sizeIdx][0];
            int h = OBJ_SIZE[sizeIdx][1];
            boolean doubleSize = (attr0 & (1 << 9)) != 0;
            int boxW = (affine && doubleSize) ? w * 2 : w;
            int boxH = (affine && doubleSize) ? h * 2 : h;

            int sprY = attr0 & 0xFF;
            if (sprY >= 160) sprY -= 256;
            // Cull entirely off-screen-Y sprites.
            if (sprY + boxH <= 0 || sprY >= SCREEN_HEIGHT) { spLive[sp] = false; continue; }

            int sprX = attr1 & 0x1FF;
            if (sprX >= SCREEN_WIDTH) sprX -= 512;
            // Cull entirely off-screen-X sprites.
            if (sprX + boxW <= 0 || sprX >= SCREEN_WIDTH) { spLive[sp] = false; continue; }

            spAttr0[sp] = attr0;
            spAttr1[sp] = attr1;
            spAttr2[sp] = attr2;
            spSprY[sp]  = sprY;
            spSprX[sp]  = sprX;
            spW[sp]     = w;
            spH[sp]     = h;
            spBoxW[sp]  = boxW;
            spBoxH[sp]  = boxH;
            spAffine[sp]= affine;
            spLive[sp]  = true;
        }
        spParsed = true;
    }

    private void renderSprites(int y, int bgMode) {
        if (!spParsed) parseOam();
        boolean use1D = (DISPCNT & (1 << 6)) != 0;
        int spriteCharBase = (bgMode >= 3) ? 0x14000 : 0x10000;
        int mosaicW = ((MOSAIC >> 8) & 0xF) + 1;   // OBJ mosaic H size
        int mosaicH = ((MOSAIC >> 12) & 0xF) + 1;  // OBJ mosaic V size

        // Walk OAM back-to-front so lower indices win ties (per hardware).
        for (int sp = 127; sp >= 0; sp--) {
            if (!spLive[sp]) continue;
            int sprY = spSprY[sp];
            int boxH = spBoxH[sp];
            // Sprite has to overlap this scanline.
            if (y < sprY || y >= sprY + boxH) continue;

            int attr0 = spAttr0[sp];
            int attr1 = spAttr1[sp];
            int attr2 = spAttr2[sp];
            int objMode = (attr0 >>> 10) & 0x3;          // 0=normal,1=semi,2=window
            boolean mosaic   = (attr0 & (1 << 12)) != 0;
            boolean affine   = spAffine[sp];
            boolean color256 = (attr0 & (1 << 13)) != 0;
            int palBank   = color256 ? 0 : ((attr2 >>> 12) & 0xF);
            int tileNum   = attr2 & 0x3FF;
            int priority  = (attr2 >>> 10) & 0x3;

            int sprX = spSprX[sp];
            int boxW = spBoxW[sp];
            int w = spW[sp];
            int h = spH[sp];

            int rowInBox = y - sprY;
            if (mosaic) rowInBox -= (rowInBox % mosaicH);

            int pa, pb, pc, pd;
            boolean flipH = false, flipV = false;
            if (affine) {
                int aIdx = (attr1 >>> 9) & 0x1F;
                int b = aIdx * 32;
                pa = (short)((oam[b+6]&0xFF)  | ((oam[b+7]&0xFF)<<8));
                pb = (short)((oam[b+14]&0xFF) | ((oam[b+15]&0xFF)<<8));
                pc = (short)((oam[b+22]&0xFF) | ((oam[b+23]&0xFF)<<8));
                pd = (short)((oam[b+30]&0xFF) | ((oam[b+31]&0xFF)<<8));
            } else {
                pa = 0x100; pb = 0; pc = 0; pd = 0x100; // identity
                flipH = (attr1 & (1 << 12)) != 0;
                flipV = (attr1 & (1 << 13)) != 0;
            }

            // Clip the on-screen X range against the framebuffer once per sprite
            // so the inner loop doesn't re-check (screenX < 0 || screenX >= W)
            // for every column.
            int bxStart = Math.max(0, -sprX);
            int bxEnd   = Math.min(boxW, SCREEN_WIDTH - sprX);

            int halfW = boxW >> 1, halfH = boxH >> 1;
            int cx = w >> 1, cy = h >> 1;

            // Non-affine fast path: no per-pixel matrix, same row in texture
            // throughout, contiguous scan line. Far cheaper than the affine path
            // and used for the great majority of sprites.
            if (!affine) {
                int sampleY = flipV ? (h - 1 - rowInBox) : rowInBox;
                if (sampleY < 0 || sampleY >= h) continue;
                int tileRow = sampleY >> 3;
                int pixRow  = sampleY & 7;

                for (int bx = bxStart; bx < bxEnd; bx++) {
                    int screenX = sprX + bx;
                    int sampleX = flipH ? (w - 1 - bx) : bx;
                    int mx = mosaic ? sampleX - (sampleX % mosaicW) : sampleX;
                    int tileCol = mx >> 3, pixCol = mx & 7;

                    int tileIdx;
                    if (use1D) {
                        tileIdx = color256 ? tileNum + tileRow * (w >> 3) * 2 + tileCol * 2
                                           : tileNum + tileRow * (w >> 3) + tileCol;
                    } else {
                        tileIdx = color256 ? (tileNum & ~1) + tileRow * 32 + tileCol * 2
                                           : tileNum + tileRow * 32 + tileCol;
                    }

                    if (color256) {
                        int off = spriteCharBase + tileIdx * 32 + pixRow * 8 + pixCol;
                        int idx = off < vram.length ? (vram[off] & 0xFF) : 0;
                        if (idx != 0 && objMode != 2) {
                            plotObj(screenX, readPalette15(256 + idx), priority, objMode == 1);
                        }
                    } else {
                        int off = spriteCharBase + tileIdx * 32 + pixRow * 4 + (pixCol >> 1);
                        int nib = off < vram.length ? (vram[off] & 0xFF) : 0;
                        int idx = (pixCol & 1) != 0 ? (nib >>> 4) : (nib & 0xF);
                        if (idx != 0 && objMode != 2) {
                            plotObj(screenX, readPalette15(256 + palBank * 16 + idx), priority, objMode == 1);
                        }
                    }
                }
                continue;
            }

            // Affine path (preserved for rotation/scaling sprites).
            int dy0 = rowInBox - halfH;
            int paDxBase = pa * (bxStart - halfW);
            int pcDxBase = pc * (bxStart - halfW);
            int sxAcc = paDxBase + pb * dy0;
            int syAcc = pcDxBase + pd * dy0;

            for (int bx = bxStart; bx < bxEnd; bx++) {
                int screenX = sprX + bx;
                int sampleX = (sxAcc >> 8) + cx;
                int sampleY = (syAcc >> 8) + cy;
                sxAcc += pa;
                syAcc += pc;
                if (sampleX < 0 || sampleX >= w || sampleY < 0 || sampleY >= h) continue;

                int mx = mosaic ? sampleX - (sampleX % mosaicW) : sampleX;
                int tileCol = mx >> 3, pixCol = mx & 7;
                int tileRow = sampleY >> 3, pixRow = sampleY & 7;

                int tileIdx;
                if (use1D) {
                    tileIdx = color256 ? tileNum + tileRow * (w >> 3) * 2 + tileCol * 2
                                       : tileNum + tileRow * (w >> 3) + tileCol;
                } else {
                    tileIdx = color256 ? (tileNum & ~1) + tileRow * 32 + tileCol * 2
                                       : tileNum + tileRow * 32 + tileCol;
                }

                if (color256) {
                    int off = spriteCharBase + tileIdx * 32 + pixRow * 8 + pixCol;
                    int idx = off < vram.length ? (vram[off] & 0xFF) : 0;
                    if (idx != 0 && objMode != 2) {
                        plotObj(screenX, readPalette15(256 + idx), priority, objMode == 1);
                    }
                } else {
                    int off = spriteCharBase + tileIdx * 32 + pixRow * 4 + (pixCol >> 1);
                    int nib = off < vram.length ? (vram[off] & 0xFF) : 0;
                    int idx = (pixCol & 1) != 0 ? (nib >>> 4) : (nib & 0xF);
                    if (idx != 0 && objMode != 2) {
                        plotObj(screenX, readPalette15(256 + palBank * 16 + idx), priority, objMode == 1);
                    }
                }
            }
        }
    }

    // ── Palette helpers ────────────────────────────────────────────────────
    private int readPalette(int idx) {
        int off = idx * 2;
        if (off + 1 >= palette.length) return 0xFF000000;
        int color15 = (palette[off] & 0xFF) | ((palette[off + 1] & 0xFF) << 8);
        return color15toARGB(color15);
    }

    private static int color15toARGB(int c) {
        int r = (c & 0x1F) << 3;
        int g = ((c >>> 5) & 0x1F) << 3;
        int b = ((c >>> 10) & 0x1F) << 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ── I/O register access ───────────────────────────────────────────────
    public int readRegister(int offset) {
        switch (offset) {
            case 0x00: return DISPCNT & 0xFF;
            case 0x01: return (DISPCNT >>> 8) & 0xFF;
            case 0x04: return DISPSTAT & 0xFF;
            case 0x05: return (DISPSTAT >>> 8) & 0xFF;
            case 0x06: return VCOUNT & 0xFF;
            case 0x07: return 0;
            case 0x08: return BG_CNT[0] & 0xFF;
            case 0x09: return (BG_CNT[0] >>> 8) & 0xFF;
            case 0x0A: return BG_CNT[1] & 0xFF;
            case 0x0B: return (BG_CNT[1] >>> 8) & 0xFF;
            case 0x0C: return BG_CNT[2] & 0xFF;
            case 0x0D: return (BG_CNT[2] >>> 8) & 0xFF;
            case 0x0E: return BG_CNT[3] & 0xFF;
            case 0x0F: return (BG_CNT[3] >>> 8) & 0xFF;
            default:   return 0;
        }
    }

    public void writeRegister(int offset, int val) {
        switch (offset) {
            case 0x00: DISPCNT  = (DISPCNT  & 0xFF00) | val; break;
            case 0x01: DISPCNT  = (DISPCNT  & 0x00FF) | (val << 8); break;
            case 0x04: DISPSTAT = (DISPSTAT & 0xFF00) | (val & 0xF8); break; // lower bits read-only
            case 0x05: DISPSTAT = (DISPSTAT & 0x00FF) | (val << 8); break;
            case 0x08: BG_CNT[0] = (BG_CNT[0] & 0xFF00) | val; break;
            case 0x09: BG_CNT[0] = (BG_CNT[0] & 0x00FF) | (val << 8); break;
            case 0x0A: BG_CNT[1] = (BG_CNT[1] & 0xFF00) | val; break;
            case 0x0B: BG_CNT[1] = (BG_CNT[1] & 0x00FF) | (val << 8); break;
            case 0x0C: BG_CNT[2] = (BG_CNT[2] & 0xFF00) | val; break;
            case 0x0D: BG_CNT[2] = (BG_CNT[2] & 0x00FF) | (val << 8); break;
            case 0x0E: BG_CNT[3] = (BG_CNT[3] & 0xFF00) | val; break;
            case 0x0F: BG_CNT[3] = (BG_CNT[3] & 0x00FF) | (val << 8); break;
            case 0x10: BG_HOFS[0] = (BG_HOFS[0] & 0xFF00) | val; break;
            case 0x11: BG_HOFS[0] = (BG_HOFS[0] & 0x00FF) | (val << 8); break;
            case 0x12: BG_VOFS[0] = (BG_VOFS[0] & 0xFF00) | val; break;
            case 0x13: BG_VOFS[0] = (BG_VOFS[0] & 0x00FF) | (val << 8); break;
            case 0x14: BG_HOFS[1] = (BG_HOFS[1] & 0xFF00) | val; break;
            case 0x15: BG_HOFS[1] = (BG_HOFS[1] & 0x00FF) | (val << 8); break;
            case 0x16: BG_VOFS[1] = (BG_VOFS[1] & 0xFF00) | val; break;
            case 0x17: BG_VOFS[1] = (BG_VOFS[1] & 0x00FF) | (val << 8); break;
            case 0x18: BG_HOFS[2] = (BG_HOFS[2] & 0xFF00) | val; break;
            case 0x19: BG_HOFS[2] = (BG_HOFS[2] & 0x00FF) | (val << 8); break;
            case 0x1A: BG_VOFS[2] = (BG_VOFS[2] & 0xFF00) | val; break;
            case 0x1B: BG_VOFS[2] = (BG_VOFS[2] & 0x00FF) | (val << 8); break;
            case 0x1C: BG_HOFS[3] = (BG_HOFS[3] & 0xFF00) | val; break;
            case 0x1D: BG_HOFS[3] = (BG_HOFS[3] & 0x00FF) | (val << 8); break;
            case 0x1E: BG_VOFS[3] = (BG_VOFS[3] & 0xFF00) | val; break;
            case 0x1F: BG_VOFS[3] = (BG_VOFS[3] & 0x00FF) | (val << 8); break;
            case 0x20: BG_PA[0] = (BG_PA[0] & 0xFF00) | val; break;
            case 0x21: BG_PA[0] = (BG_PA[0] & 0x00FF) | (val << 8); break;
            case 0x22: BG_PB[0] = (BG_PB[0] & 0xFF00) | val; break;
            case 0x23: BG_PB[0] = (BG_PB[0] & 0x00FF) | (val << 8); break;
            case 0x24: BG_PC[0] = (BG_PC[0] & 0xFF00) | val; break;
            case 0x25: BG_PC[0] = (BG_PC[0] & 0x00FF) | (val << 8); break;
            case 0x26: BG_PD[0] = (BG_PD[0] & 0xFF00) | val; break;
            case 0x27: BG_PD[0] = (BG_PD[0] & 0x00FF) | (val << 8); break;
            case 0x28: BG_RefX[0] = (BG_RefX[0] & 0xFFFFFF00) | val; break;
            case 0x29: BG_RefX[0] = (BG_RefX[0] & 0xFFFF00FF) | (val << 8); break;
            case 0x2A: BG_RefX[0] = (BG_RefX[0] & 0xFF00FFFF) | (val << 16); break;
            case 0x2B: BG_RefX[0] = (BG_RefX[0] & 0x00FFFFFF) | (val << 24); break;
            case 0x2C: BG_RefY[0] = (BG_RefY[0] & 0xFFFFFF00) | val; break;
            case 0x2D: BG_RefY[0] = (BG_RefY[0] & 0xFFFF00FF) | (val << 8); break;
            case 0x2E: BG_RefY[0] = (BG_RefY[0] & 0xFF00FFFF) | (val << 16); break;
            case 0x2F: BG_RefY[0] = (BG_RefY[0] & 0x00FFFFFF) | (val << 24); break;
            // BG3 affine params (PA-PD) and reference points
            case 0x30: BG_PA[1] = (BG_PA[1] & 0xFF00) | val; break;
            case 0x31: BG_PA[1] = (BG_PA[1] & 0x00FF) | (val << 8); break;
            case 0x32: BG_PB[1] = (BG_PB[1] & 0xFF00) | val; break;
            case 0x33: BG_PB[1] = (BG_PB[1] & 0x00FF) | (val << 8); break;
            case 0x34: BG_PC[1] = (BG_PC[1] & 0xFF00) | val; break;
            case 0x35: BG_PC[1] = (BG_PC[1] & 0x00FF) | (val << 8); break;
            case 0x36: BG_PD[1] = (BG_PD[1] & 0xFF00) | val; break;
            case 0x37: BG_PD[1] = (BG_PD[1] & 0x00FF) | (val << 8); break;
            case 0x38: BG_RefX[1] = (BG_RefX[1] & 0xFFFFFF00) | val; break;
            case 0x39: BG_RefX[1] = (BG_RefX[1] & 0xFFFF00FF) | (val << 8); break;
            case 0x3A: BG_RefX[1] = (BG_RefX[1] & 0xFF00FFFF) | (val << 16); break;
            case 0x3B: BG_RefX[1] = (BG_RefX[1] & 0x00FFFFFF) | (val << 24); break;
            case 0x3C: BG_RefY[1] = (BG_RefY[1] & 0xFFFFFF00) | val; break;
            case 0x3D: BG_RefY[1] = (BG_RefY[1] & 0xFFFF00FF) | (val << 8); break;
            case 0x3E: BG_RefY[1] = (BG_RefY[1] & 0xFF00FFFF) | (val << 16); break;
            case 0x3F: BG_RefY[1] = (BG_RefY[1] & 0x00FFFFFF) | (val << 24); break;
            case 0x40: WIN0H = (WIN0H & 0xFF00) | val; break;
            case 0x41: WIN0H = (WIN0H & 0x00FF) | (val << 8); break;
            case 0x42: WIN1H = (WIN1H & 0xFF00) | val; break;
            case 0x43: WIN1H = (WIN1H & 0x00FF) | (val << 8); break;
            case 0x44: WIN0V = (WIN0V & 0xFF00) | val; break;
            case 0x45: WIN0V = (WIN0V & 0x00FF) | (val << 8); break;
            case 0x46: WIN1V = (WIN1V & 0xFF00) | val; break;
            case 0x47: WIN1V = (WIN1V & 0x00FF) | (val << 8); break;
            case 0x48: WININ  = (WININ  & 0xFF00) | val; break;
            case 0x49: WININ  = (WININ  & 0x00FF) | (val << 8); break;
            case 0x4A: WINOUT = (WINOUT & 0xFF00) | val; break;
            case 0x4B: WINOUT = (WINOUT & 0x00FF) | (val << 8); break;
            case 0x4C: MOSAIC = (MOSAIC & 0xFF00) | val; break;
            case 0x4D: MOSAIC = (MOSAIC & 0x00FF) | (val << 8); break;
            case 0x50: BLDCNT  = (BLDCNT  & 0xFF00) | val; break;
            case 0x51: BLDCNT  = (BLDCNT  & 0x00FF) | (val << 8); break;
            case 0x52: BLDALPHA= (BLDALPHA& 0xFF00) | val; break;
            case 0x53: BLDALPHA= (BLDALPHA& 0x00FF) | (val << 8); break;
            case 0x54: BLDY   = val & 0x1F; break;
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public int[] getFramebuffer() { return framebuffer; }
    public int   getScanline()    { return scanline; }
    public boolean isVBlank()     { return scanline >= SCREEN_HEIGHT; }
    public boolean isHBlank()     { return inHBlank; }

    /** Returns true exactly once per frame, when VBlank begins (line 160). */
    public boolean pollVBlankEdge() {
        if (vblankEdge) { vblankEdge = false; return true; }
        return false;
    }

    public boolean pollNewFrame() {
        if (newFrame) { newFrame = false; return true; }
        return false;
    }

    public void reset() {
        DISPCNT = 0; DISPSTAT = 0; VCOUNT = 0;
        java.util.Arrays.fill(BG_CNT, 0);
        java.util.Arrays.fill(framebuffer, 0xFF000000);
        cycleCnt = 0;
        scanline = 0;
        inHBlank = false;
        newFrame = false;
        vblankEdge = false;
    }
}
