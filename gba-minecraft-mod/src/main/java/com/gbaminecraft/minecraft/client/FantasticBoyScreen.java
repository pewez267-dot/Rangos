package com.gbaminecraft.minecraft.client;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.GBAStateIO;
import com.gbaminecraft.emulator.ppu.PPU;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

/**
 * Fantastic Boy Advance — handheld launcher + emulator screen (client only).
 *
 * Flow:
 *   BROWSER   -> lists ROMs from the RomsGBA folder
 *   GAME_MENU -> New Game / Load Game / Edit Keys / Back
 *   KEYMAP    -> rebind GBA buttons
 *   PLAYING   -> emulation with a 3:2 screen and fully clickable controls
 */
public class FantasticBoyScreen extends Screen {

    private enum Mode { BROWSER, GAME_MENU, KEYMAP, PLAYING }

    private static final int GBA_W = PPU.SCREEN_WIDTH;   // 240
    private static final int GBA_H = PPU.SCREEN_HEIGHT;  // 160

    private final GBAEmulator emulator = new GBAEmulator();

    private Mode mode = Mode.BROWSER;
    private List<File> roms;
    private File selectedRom;
    private int romPage = 0;
    private static final int ROMS_PER_PAGE = 8;

    // Key remapping state
    private int rebindingIndex = -1;

    // Display geometry (computed per frame for PLAYING)
    private int dispX, dispY, dispW, dispH, scale;

    // Texture — FBA 13o: DOUBLE-BUFFERED. We ping-pong between two GL textures
    // so we never upload into the same texture the GPU is still drawing from the
    // previous frame. Updating a single live texture every frame let the GPU
    // sample it mid-upload -> a horizontal seam that scrolled up the screen
    // (visible even with VSync on, because it's a CPU/GPU upload-vs-draw race,
    // not monitor tearing). With two textures the just-uploaded one is shown and
    // the other is free to receive the next frame.
    private final DynamicTexture[]  gbaTexture = new DynamicTexture[2];
    private final NativeImage[]     gbaImage   = new NativeImage[2];
    private final ResourceLocation[] texLoc    = new ResourceLocation[2];
    private int texIndex = 0;
    private ResourceLocation currentTexLoc = null;
    private boolean textureCreated = false;

    // On-screen control hit boxes (set during render)
    private final int[][] controlBoxes = new int[10][4]; // [gbaKey] = {x,y,w,h}
    private String fpsStr = "";          // cached FPS label (rebuilt only when it changes)
    private double lastShownFps = -1;
    private int mouseHeldKey = -1;

    private String status = "";
    private int autosaveTicks = 0;

    public FantasticBoyScreen() {
        super(Component.literal("Fantastic Boy Advance"));
    }

    @Override
    protected void init() {
        GBAKeyConfig.load();
        if (roms == null) roms = RomLibrary.listRoms();
        buildWidgets();
    }

    private void refresh() {
        roms = RomLibrary.listRoms();
        rebuildWidgets();
    }

    private void buildWidgets() {
        clearWidgets();
        switch (mode) {
            case BROWSER:   buildBrowser();  break;
            case GAME_MENU: buildGameMenu(); break;
            case KEYMAP:    buildKeymap();   break;
            case PLAYING:   buildPlaying();  break;
        }
    }

    // ── BROWSER ──────────────────────────────────────────────────────────
    private void buildBrowser() {
        int cx = width / 2;
        int top = 50;

        int start = romPage * ROMS_PER_PAGE;
        for (int i = 0; i < ROMS_PER_PAGE && start + i < roms.size(); i++) {
            final File rom = roms.get(start + i);
            addRenderableWidget(Button.builder(Component.literal(rom.getName()), b -> {
                selectedRom = rom;
                mode = Mode.GAME_MENU;
                rebuildWidgets();
            }).bounds(cx - 140, top + i * 22, 280, 20).build());
        }

        if (romPage > 0) {
            addRenderableWidget(Button.builder(Component.literal("< Anterior"), b -> {
                romPage--; rebuildWidgets();
            }).bounds(cx - 140, top + ROMS_PER_PAGE * 22 + 6, 90, 20).build());
        }
        if (start + ROMS_PER_PAGE < roms.size()) {
            addRenderableWidget(Button.builder(Component.literal("Siguiente >"), b -> {
                romPage++; rebuildWidgets();
            }).bounds(cx + 50, top + ROMS_PER_PAGE * 22 + 6, 90, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Refrescar"), b -> refresh())
                .bounds(cx - 45, top + ROMS_PER_PAGE * 22 + 6, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx - 45, height - 28, 90, 20).build());
    }

    // ── GAME MENU ────────────────────────────────────────────────────────
    private void buildGameMenu() {
        int cx = width / 2;
        int top = height / 2 - 60;

        addRenderableWidget(Button.builder(Component.literal("Iniciar partida nueva"), b -> startGame(false))
                .bounds(cx - 100, top, 200, 22).build());

        boolean hasState = selectedRom != null && RomLibrary.stateFile(selectedRom).exists();
        Button load = Button.builder(Component.literal("Cargar partida"), b -> startGame(true))
                .bounds(cx - 100, top + 28, 200, 22).build();
        load.active = hasState;
        addRenderableWidget(load);

        addRenderableWidget(Button.builder(Component.literal("Editar teclas / mapeo"), b -> {
            mode = Mode.KEYMAP; rebuildWidgets();
        }).bounds(cx - 100, top + 56, 200, 22).build());

        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> {
            mode = Mode.BROWSER; rebuildWidgets();
        }).bounds(cx - 100, top + 90, 200, 20).build());
    }

    // ── KEYMAP ───────────────────────────────────────────────────────────
    private void buildKeymap() {
        int cx = width / 2;
        int top = 44;
        for (int i = 0; i < GBAKeyConfig.LABELS.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(
                    Component.literal(GBAKeyConfig.LABELS[i] + " : " + GBAKeyConfig.keyName(GBAKeyConfig.getKey(i))),
                    b -> { rebindingIndex = idx; rebuildWidgets(); })
                .bounds(cx - 120, top + i * 22, 240, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Restaurar por defecto"), b -> {
            GBAKeyConfig.resetDefaults(); rebindingIndex = -1; rebuildWidgets();
        }).bounds(cx - 120, top + 10 * 22 + 6, 115, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Volver"), b -> {
            rebindingIndex = -1; mode = Mode.GAME_MENU; rebuildWidgets();
        }).bounds(cx + 5, top + 10 * 22 + 6, 115, 20).build());
    }

    // ── PLAYING ──────────────────────────────────────────────────────────
    private void buildPlaying() {
        // Action buttons live in a top bar; positions depend on width.
        int y = 6;
        addRenderableWidget(Button.builder(Component.literal("Salir"), b -> exitGame())
                .bounds(6, y, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Guardar estado"), b -> doSaveState())
                .bounds(70, y, 100, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cargar estado"), b -> doLoadState())
                .bounds(174, y, 95, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Forzar guardado"), b -> doForceSave())
                .bounds(273, y, 105, 18).build());
        addRenderableWidget(Button.builder(Component.literal(emulator.isPaused() ? "Reanudar" : "Pausa"), b -> {
            if (emulator.isPaused()) emulator.resume(); else emulator.pause();
            rebuildWidgets();
        }).bounds(382, y, 70, 18).build());
        // Second row: diagnostics (dumps a boot trace to RomsGBA for debugging).
        addRenderableWidget(Button.builder(Component.literal(emulator.isTracing() ? "Trace ON" : "Trace OFF"), b -> {
            emulator.setTracing(!emulator.isTracing());
            rebuildWidgets();
        }).bounds(6, y + 20, 80, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Diagnóstico"), b -> dumpDiagnostics())
                .bounds(90, y + 20, 100, 18).build());
    }

    private void dumpDiagnostics() {
        try {
            String report = emulator.getDiagnostics();
            java.nio.file.Path out = RomLibrary.romsDir().resolve("boot-trace.txt");
            java.nio.file.Files.writeString(out, report);
            status = "Diagnóstico guardado en RomsGBA/boot-trace.txt";
        } catch (Exception e) {
            status = "Error diagnóstico: " + e.getMessage();
        }
    }

    // ── Game control ─────────────────────────────────────────────────────
    private void startGame(boolean loadState) {
        if (selectedRom == null) return;
        try {
            byte[] data = Files.readAllBytes(selectedRom.toPath());
            if (!emulator.loadROM(data, selectedRom.getName())) {
                status = "ROM invalida: " + selectedRom.getName();
                return;
            }
            // Battery save (if present) goes into SRAM
            File bat = RomLibrary.batteryFile(selectedRom);
            if (bat.exists()) {
                try (InputStream in = new FileInputStream(bat)) { GBAStateIO.loadBattery(emulator, in); }
            }
            if (loadState) {
                File st = RomLibrary.stateFile(selectedRom);
                if (st.exists()) {
                    try (InputStream in = new FileInputStream(st)) { GBAStateIO.loadState(emulator, in); }
                }
            }
            emulator.start();
            mode = Mode.PLAYING;
            status = "";
            rebuildWidgets();
        } catch (Exception e) {
            GBAMod.LOGGER.error("Error iniciando ROM", e);
            status = "Error: " + e.getMessage();
        }
    }

    private void doSaveState() {
        if (selectedRom == null) return;
        boolean wasPaused = emulator.isPaused();
        emulator.pause();
        try (OutputStream out = new FileOutputStream(RomLibrary.stateFile(selectedRom))) {
            GBAStateIO.saveState(emulator, out);
            status = "Estado guardado.";
        } catch (Exception e) {
            status = "Error al guardar estado: " + e.getMessage();
        }
        if (!wasPaused) emulator.resume();
    }

    private void doLoadState() {
        if (selectedRom == null) return;
        File st = RomLibrary.stateFile(selectedRom);
        if (!st.exists()) { status = "No hay estado guardado."; return; }
        boolean wasPaused = emulator.isPaused();
        emulator.pause();
        try (InputStream in = new FileInputStream(st)) {
            GBAStateIO.loadState(emulator, in);
            status = "Estado cargado.";
        } catch (Exception e) {
            status = "Error al cargar estado: " + e.getMessage();
        }
        if (!wasPaused) emulator.resume();
    }

    private void doForceSave() {
        if (selectedRom == null) return;
        try (OutputStream out = new FileOutputStream(RomLibrary.batteryFile(selectedRom))) {
            GBAStateIO.saveBattery(emulator, out);
            status = "Partida (bateria) guardada.";
        } catch (Exception e) {
            status = "Error al forzar guardado: " + e.getMessage();
        }
    }

    private void exitGame() {
        // Auto-persist battery on exit
        try {
            if (selectedRom != null) {
                try (OutputStream out = new FileOutputStream(RomLibrary.batteryFile(selectedRom))) {
                    GBAStateIO.saveBattery(emulator, out);
                }
            }
        } catch (Exception ignored) {}
        emulator.stop();
        emulator.releaseAllKeys();
        mouseHeldKey = -1;
        mode = Mode.BROWSER;
        rebuildWidgets();
    }

    // ── Rendering ────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        switch (mode) {
            case BROWSER:   renderBrowser(g);  break;
            case GAME_MENU: renderGameMenu(g); break;
            case KEYMAP:    renderKeymap(g);   break;
            case PLAYING:   renderPlaying(g, mouseX, mouseY); break;
        }
        // Autosave de batería cada ~10s mientras se juega, para no perder partida.
        if (mode == Mode.PLAYING && selectedRom != null && emulator.isRunning() && !emulator.isPaused()) {
            if (++autosaveTicks >= 600) { // ~10s a 60 fps
                autosaveTicks = 0;
                try (OutputStream out = new FileOutputStream(RomLibrary.batteryFile(selectedRom))) {
                    GBAStateIO.saveBattery(emulator, out);
                } catch (Exception ignored) {}
            }
        }
        super.render(g, mouseX, mouseY, partial);
        if (!status.isEmpty()) {
            g.drawCenteredString(font, status, width / 2, height - 42, 0xFFFF55);
        }
    }

    private void renderBrowser(GuiGraphics g) {
        g.drawCenteredString(font, "FANTASTIC BOY ADVANCE", width / 2, 18, 0xFFCC44);
        if (roms.isEmpty()) {
            g.drawCenteredString(font, "No hay ROMs. Coloca archivos .gba en:", width / 2, 70, 0xFFFFFF);
            g.drawCenteredString(font, RomLibrary.romsDir().toString(), width / 2, 84, 0xAAAAAA);
        } else {
            g.drawCenteredString(font, "Selecciona una ROM (" + roms.size() + ")", width / 2, 34, 0xAAAAAA);
        }
    }

    private void renderGameMenu(GuiGraphics g) {
        g.drawCenteredString(font, "FANTASTIC BOY ADVANCE", width / 2, 18, 0xFFCC44);
        if (selectedRom != null) {
            g.drawCenteredString(font, selectedRom.getName(), width / 2, height / 2 - 84, 0xFFFFFF);
        }
    }

    private void renderKeymap(GuiGraphics g) {
        g.drawCenteredString(font, "MAPEO DE TECLAS", width / 2, 18, 0xFFCC44);
        if (rebindingIndex >= 0) {
            g.drawCenteredString(font, "Pulsa una tecla para asignar a [" +
                    GBAKeyConfig.LABELS[rebindingIndex] + "]  (ESC cancela)", width / 2, 30, 0x55FF55);
        }
    }

    private void renderPlaying(GuiGraphics g, int mouseX, int mouseY) {
        computeGeometry();

        // Screen border
        g.fill(dispX - 3, dispY - 3, dispX + dispW + 3, dispY + dispH + 3, 0xFF000000);
        g.fill(dispX - 1, dispY - 1, dispX + dispW + 1, dispY + dispH + 1, 0xFF202020);

        updateTexture();
        if (textureCreated && currentTexLoc != null) {
            g.blit(currentTexLoc, dispX, dispY, dispW, dispH, 0f, 0f, GBA_W, GBA_H, GBA_W, GBA_H);
        }

        // ROM name + FPS
        g.drawString(font, emulator.getRomName(), dispX, dispY - 14, 0xFFCC44, false);
        if (emulator.isRunning() && !emulator.isPaused()) {
            double fps = emulator.getCurrentFps();
            if (fps != lastShownFps) { lastShownFps = fps; fpsStr = String.format("%.1f FPS", fps); }
            g.drawString(font, fpsStr, dispX + dispW - 50, dispY - 14, 0xAAAAAA, false);
        } else if (emulator.isPaused()) {
            g.drawCenteredString(font, "PAUSA", dispX + dispW / 2, dispY + dispH / 2 - 4, 0xFFFFFF);
        }

        renderControls(g);
    }

    /** Draws the on-screen clickable controls below the display, and fills hit boxes. */
    private void renderControls(GuiGraphics g) {
        int baseY = dispY + dispH + 14;
        int leftX = dispX + 4;

        // D-pad (cross) on the left
        int u = 26;                  // unit size
        int dpx = leftX + u;         // center column
        int dpy = baseY + u;         // center row
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_UP,    dpx,       dpy - u,   u, u);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_DOWN,  dpx,       dpy + u,   u, u);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_LEFT,  dpx - u,   dpy,       u, u);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_RIGHT, dpx + u,   dpy,       u, u);

        // A / B on the right
        int rightX = dispX + dispW - 4 - 2 * u - 10;
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_B, rightX,        baseY + u, u, u);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_A, rightX + u+10, baseY,     u, u);

        // Start / Select in the middle
        int midX = dispX + dispW / 2 - 50;
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_SELECT, midX,      baseY + 2*u + 4, 44, 16);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_START,  midX + 56, baseY + 2*u + 4, 44, 16);

        // L / R shoulder
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_L, dispX,            baseY - 2, 40, 16);
        setBox(com.gbaminecraft.emulator.input.GBAInput.KEY_R, dispX + dispW-40, baseY - 2, 40, 16);

        String[] labels = {"A","B","Sel","Start","→","←","↑","↓","R","L"};
        for (int k = 0; k < 10; k++) {
            int[] r = controlBoxes[k];
            boolean pressed = emulator.getInput().isPressed(k);
            int col = pressed ? 0xFF33DD55 : 0xCC555555;
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], col);
            g.renderOutline(r[0], r[1], r[2], r[3], 0xFF000000);
            g.drawCenteredString(font, labels[k], r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, 0xFFFFFF);
        }
    }

    private void setBox(int key, int x, int y, int w, int h) {
        controlBoxes[key][0] = x; controlBoxes[key][1] = y;
        controlBoxes[key][2] = w; controlBoxes[key][3] = h;
    }

    private void computeGeometry() {
        int maxW = width - 24;
        int maxH = height - 150; // leave room for controls + top bar
        scale = Math.max(1, Math.min(maxW / GBA_W, maxH / GBA_H));
        dispW = GBA_W * scale;
        dispH = GBA_H * scale;
        dispX = (width - dispW) / 2;
        dispY = 34;
    }

    private void createTexture() {
        if (textureCreated) return;
        for (int i = 0; i < 2; i++) {
            gbaImage[i]  = new NativeImage(NativeImage.Format.RGBA, GBA_W, GBA_H, false);
            gbaTexture[i] = new DynamicTexture(gbaImage[i]);
            texLoc[i]    = minecraft.getTextureManager()
                    .register("fantastic_boy_screen_" + i, gbaTexture[i]);
        }
        textureCreated = true;
    }

    private void updateTexture() {
        if (!textureCreated) createTexture();
        // Only re-upload when the emulator actually produced a NEW frame.
        // pollFrame() returns non-null exactly once per new emulator frame; on
        // the other host render frames (e.g. a 144 Hz monitor showing a 60 fps
        // source) we skip the 38,400-pixel copy and the GPU texture upload
        // entirely and just keep showing the current texture. This removes a big
        // chunk of redundant render-thread work that could cause display hitches.
        int[] frame = emulator.pollFrame();
        if (frame == null) return;
        // FBA 13o: write into the buffer that is NOT currently being displayed,
        // so the GPU is never reading the texture we're uploading.
        texIndex ^= 1;
        NativeImage img = gbaImage[texIndex];
        // The emulator framebuffer is ARGB8888 (0xAARRGGBB). Minecraft's
        // NativeImage with Format.RGBA expects each pixel as 0xAABBGGRR (ABGR),
        // i.e. red and blue swapped while alpha and green stay put.
        for (int py = 0; py < GBA_H; py++) {
            int row = py * GBA_W;
            for (int px = 0; px < GBA_W; px++) {
                int argb = frame[row + px];
                int abgr = (argb & 0xFF00FF00)         // alpha + green keep place
                         | ((argb & 0x00FF0000) >> 16)  // red  -> low byte
                         | ((argb & 0x000000FF) << 16); // blue -> high colour byte
                img.setPixelRGBA(px, py, abgr);
            }
        }
        gbaTexture[texIndex].upload();
        currentTexLoc = texLoc[texIndex];
    }

    // ── Input ────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (mode == Mode.PLAYING && btn == 0) {
            for (int k = 0; k < 10; k++) {
                int[] r = controlBoxes[k];
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    mouseHeldKey = k;
                    emulator.pressKey(k);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (mouseHeldKey >= 0) {
            emulator.releaseKey(mouseHeldKey);
            mouseHeldKey = -1;
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Rebinding capture
        if (mode == Mode.KEYMAP && rebindingIndex >= 0) {
            if (keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                GBAKeyConfig.setKey(rebindingIndex, keyCode);
            }
            rebindingIndex = -1;
            rebuildWidgets();
            return true;
        }

        if (mode == Mode.PLAYING) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { exitGame(); return true; }
            int gba = GBAKeyConfig.gbaKeyForGlfw(keyCode);
            if (gba >= 0) { emulator.pressKey(gba); return true; }
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (mode == Mode.PLAYING) {
            int gba = GBAKeyConfig.gbaKeyForGlfw(keyCode);
            if (gba >= 0) { emulator.releaseKey(gba); return true; }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        emulator.stop();
        if (textureCreated) {
            for (int i = 0; i < 2; i++) {
                if (texLoc[i] != null) minecraft.getTextureManager().release(texLoc[i]);
                if (gbaTexture[i] != null) gbaTexture[i].close();
                if (gbaImage[i] != null) gbaImage[i].close();
            }
            currentTexLoc = null;
            textureCreated = false;
        }
        super.onClose();
    }
}
