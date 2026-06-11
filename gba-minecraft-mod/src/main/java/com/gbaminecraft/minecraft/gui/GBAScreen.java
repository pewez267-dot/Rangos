package com.gbaminecraft.minecraft.gui;

import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.emulator.input.GBAInput;
import com.gbaminecraft.emulator.ppu.PPU;
import com.gbaminecraft.minecraft.item.GBACartridgeItem;
import com.gbaminecraft.minecraft.network.GBANetworkHandler;
import com.gbaminecraft.minecraft.tileentity.GBATileEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side GUI for the GBA Console.
 *
 * Renders the GBA screen (240×160) scaled up in a Minecraft GUI window.
 * Key bindings are mapped to GBA buttons and sent to the server.
 *
 * Layout:
 *  ┌──────────────────────────────────────────┐
 *  │  GBA: <ROM name>                 [■ Stop]│
 *  │  ┌──────────────────────────────────────┐ │
 *  │  │          GBA SCREEN 240×160          │ │
 *  │  │          (scaled 2x = 480×320)       │ │
 *  │  └──────────────────────────────────────┘ │
 *  │  [D-Pad]  [A] [B]  [Select] [Start]  [L][R] │
 *  │  FPS: 59.7   Speed: 1x  [Load ROM] [Reset] │
 *  └──────────────────────────────────────────┘
 */
public class GBAScreen extends AbstractContainerScreen<GBAMenu> {

    // Screen texture (dynamically updated each frame)
    private DynamicTexture gbaTexture;
    private NativeImage gbaImage;
    private ResourceLocation textureLocation;
    private boolean textureCreated = false;

    // Scale factor for the GBA display
    private static final int SCALE = 2;
    private static final int GBA_W = PPU.SCREEN_WIDTH;
    private static final int GBA_H = PPU.SCREEN_HEIGHT;
    private static final int DISPLAY_W = GBA_W * SCALE;
    private static final int DISPLAY_H = GBA_H * SCALE;

    // GUI dimensions
    private static final int GUI_W = DISPLAY_W + 20;
    private static final int GUI_H = DISPLAY_H + 80;

    // Key mappings: Minecraft keyCode -> GBA key constant
    private static final Map<Integer, Integer> KEY_MAP = new HashMap<>();
    static {
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_X,      GBAInput.KEY_A);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_Z,      GBAInput.KEY_B);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE, GBAInput.KEY_SELECT);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,  GBAInput.KEY_START);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT,  GBAInput.KEY_RIGHT);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT,   GBAInput.KEY_LEFT);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_UP,     GBAInput.KEY_UP);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,   GBAInput.KEY_DOWN);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_S,      GBAInput.KEY_R);
        KEY_MAP.put(org.lwjgl.glfw.GLFW.GLFW_KEY_A,      GBAInput.KEY_L);
    }

    private GBATileEntity tileEntity;
    private GBAEmulator emulator;

    // Button state (for on-screen D-pad)
    private final boolean[] buttonStates = new boolean[10];

    // Speed selector
    private double speedMult = 1.0;

    public GBAScreen(GBAMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.tileEntity = menu.getTileEntity();
        this.emulator   = tileEntity.getEmulator();
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Stop button
        addRenderableWidget(Button.builder(Component.literal("Stop"), btn -> {
            GBANetworkHandler.sendStopEmulator(tileEntity.getBlockPos());
        }).bounds(x + imageWidth - 50, y + 4, 45, 14).build());

        // Reset button
        addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> {
            GBANetworkHandler.sendResetEmulator(tileEntity.getBlockPos());
        }).bounds(x + imageWidth - 100, y + GUI_H - 20, 45, 14).build());

        // Speed buttons
        addRenderableWidget(Button.builder(Component.literal("1x"), btn -> {
            speedMult = 1.0; GBANetworkHandler.sendSetSpeed(tileEntity.getBlockPos(), 1.0);
        }).bounds(x + 5, y + GUI_H - 20, 28, 14).build());

        addRenderableWidget(Button.builder(Component.literal("2x"), btn -> {
            speedMult = 2.0; GBANetworkHandler.sendSetSpeed(tileEntity.getBlockPos(), 2.0);
        }).bounds(x + 35, y + GUI_H - 20, 28, 14).build());

        addRenderableWidget(Button.builder(Component.literal("4x"), btn -> {
            speedMult = 4.0; GBANetworkHandler.sendSetSpeed(tileEntity.getBlockPos(), 4.0);
        }).bounds(x + 65, y + GUI_H - 20, 28, 14).build());

        // Create GBA texture
        createTexture();
    }

    private void createTexture() {
        if (textureCreated) return;
        gbaImage   = new NativeImage(NativeImage.Format.RGBA, GBA_W, GBA_H, false);
        gbaTexture = new DynamicTexture(gbaImage);
        textureLocation = minecraft.getTextureManager()
                .register("gba_screen_" + tileEntity.getBlockPos().hashCode(), gbaTexture);
        textureCreated = true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Background panel (dark purple GBA-colored)
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A0A2E);

        // Screen border
        int sx = x + 10;
        int sy = y + 20;
        graphics.fill(sx - 2, sy - 2, sx + DISPLAY_W + 2, sy + DISPLAY_H + 2, 0xFF000000);

        // Update and render the GBA screen texture
        updateScreenTexture();
        if (textureCreated) {
            RenderSystem.setShaderTexture(0, textureLocation);
            graphics.blit(textureLocation, sx, sy, 0, 0, DISPLAY_W, DISPLAY_H,
                    DISPLAY_W, DISPLAY_H);
        }

        // ROM name
        String romName = tileEntity.isCartridgeInserted()
                ? tileEntity.getLoadedRomName()
                : "No ROM Loaded — Insert a GBA Cartridge";
        graphics.drawString(font, romName, x + 5, y + 6, 0xFFCC44, true);

        // FPS display
        if (emulator.isRunning()) {
            String fps = String.format("%.1f FPS  Speed: %.1fx", emulator.getCurrentFps(), speedMult);
            graphics.drawString(font, fps, x + 5, y + DISPLAY_H + 26, 0xAAAAAA, false);
        }

        // On-screen button labels
        int btnY = y + DISPLAY_H + 26;
        graphics.drawString(font, "[←↑↓→] [Z=B] [X=A] [Enter=Start] [Bksp=Select] [A=L] [S=R]",
                x + 5, y + imageHeight - 36, 0x888888, false);

        // Button press indicators
        int indX = x + 5;
        int indY = y + DISPLAY_H + 14;
        String[] labels = {"A","B","Sel","Sta","→","←","↑","↓","R","L"};
        for (int i = 0; i < 10; i++) {
            int color = buttonStates[i] ? 0xFF00FF44 : 0xFF444444;
            graphics.fill(indX + i*22, indY, indX + i*22 + 20, indY + 10, color);
            graphics.drawCenteredString(font, labels[i], indX + i*22 + 10, indY + 1, 0xFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No labels rendered by super (title/inventory)
    }

    private void updateScreenTexture() {
        if (!textureCreated || emulator == null) return;

        int[] frame = emulator.pollFrame();
        if (frame == null) return;

        // Write ARGB pixels to the NativeImage (RGBA format)
        for (int py = 0; py < GBA_H; py++) {
            for (int px = 0; px < GBA_W; px++) {
                int argb = frame[py * GBA_W + px];
                // Convert ARGB -> ABGR (NativeImage RGBA is stored as ABGR internally)
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8)  & 0xFF;
                int b =  argb        & 0xFF;
                gbaImage.setPixelRGBA(px, py, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        gbaTexture.upload();
    }

    // ── Key input ──────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Integer gbaKey = KEY_MAP.get(keyCode);
        if (gbaKey != null) {
            buttonStates[gbaKey] = true;
            GBANetworkHandler.sendKeyPress(tileEntity.getBlockPos(), gbaKey, true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        Integer gbaKey = KEY_MAP.get(keyCode);
        if (gbaKey != null) {
            buttonStates[gbaKey] = false;
            GBANetworkHandler.sendKeyPress(tileEntity.getBlockPos(), gbaKey, false);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        // Release all keys when closing
        for (int key = 0; key < 10; key++) {
            if (buttonStates[key]) {
                GBANetworkHandler.sendKeyPress(tileEntity.getBlockPos(), key, false);
                buttonStates[key] = false;
            }
        }
        // Clean up texture
        if (textureCreated && gbaTexture != null) {
            minecraft.getTextureManager().release(textureLocation);
            gbaTexture.close();
            gbaImage.close();
            textureCreated = false;
        }
        super.onClose();
    }
}
