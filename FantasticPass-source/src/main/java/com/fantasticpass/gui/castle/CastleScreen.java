/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.castle;

import com.fantasticpass.client.PassBackgroundManager;
import com.fantasticpass.client.PassPlaylistManager;
import com.fantasticpass.gui.widgets.MusicButton;
import com.fantasticpass.gui.widgets.PeekButton;
import com.fantasticpass.quest.Quest;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public abstract class CastleScreen
extends Screen {
    public static final int SLOT = 16;
    protected static final int CELL = 18;
    protected static final int COLS = 9;
    private static final ResourceLocation PASS_BG = new ResourceLocation("fantasticpass", "textures/gui/castle/pass_bg.png");
    private static final int BG_W = 1536;
    private static final int BG_H = 1024;
    @Nullable
    protected final Screen parent;
    private final ResourceLocation background;
    private final int ascent;
    private final int cx0;
    private final int cy0;
    private final int cx1;
    private final int cy1;
    protected int scale = 3;
    protected int left;
    protected int top;
    private long openTime;
    protected float anim;
    private static boolean peekBackground;
    protected static final int QUEST_ICON = 1;

    protected static ResourceLocation castle(String name) {
        return new ResourceLocation("fantasticpass", "textures/gui/castle/" + name + ".png");
    }

    protected static ResourceLocation icon(int index) {
        String n = index == 0 ? "bp_icons_empty" : String.format("bp_icons_%02d", index);
        return new ResourceLocation("fantasticpass", "textures/gui/castle/icons/" + n + ".png");
    }

    public static boolean isPeek() {
        return peekBackground;
    }

    public static void togglePeek() {
        peekBackground = !peekBackground;
    }

    protected CastleScreen(Component title, @Nullable Screen parent, ResourceLocation background, int ascent, int cx0, int cy0, int cx1, int cy1) {
        super(title);
        this.parent = parent;
        this.background = background;
        this.ascent = ascent;
        this.cx0 = cx0;
        this.cy0 = cy0;
        this.cx1 = cx1;
        this.cy1 = cy1;
    }

    protected void init() {
        peekBackground = false;
        int contentW = this.cx1 - this.cx0;
        int contentH = this.cy1 - this.cy0;
        int availW = (int)((float)this.width * 0.7f);
        int availH = (int)((float)this.height * 0.7f);
        int fit = Math.min(availW / Math.max(1, contentW), availH / Math.max(1, contentH));
        this.scale = Mth.clamp((int)fit, (int)2, (int)4);
        int drawnW = contentW * this.scale;
        int drawnH = contentH * this.scale;
        int screenContentLeft = (this.width - drawnW) / 2;
        int screenContentTop = (this.height - drawnH) / 2;
        this.left = screenContentLeft - this.cx0 * this.scale;
        this.top = screenContentTop - this.cy0 * this.scale;
        this.openTime = System.currentTimeMillis();
        this.anim = 0.0f;
        PassPlaylistManager.ensurePlaying();
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f);
        int btnSize = Mth.clamp((int)(this.scale * 5), (int)14, (int)18);
        int bx = Math.min(this.sx(this.cx1) - btnSize - 2, this.width - btnSize - 2);
        bx = Math.max(bx, btnSize + 5);
        int by = Math.max(2, this.sy(this.cy0) + 2);
        this.addRenderableWidget(new MusicButton(bx, by, btnSize));
        this.addRenderableWidget(new PeekButton(bx - btnSize - 3, by, btnSize));
        this.initControls();
    }

    protected abstract void initControls();

    protected int sx(int tx) {
        return this.left + tx * this.scale;
    }

    protected int sy(int ty) {
        return this.top + ty * this.scale;
    }

    protected int slotX(int col) {
        return this.sx(48 + col * 18);
    }

    protected int slotY(int row) {
        return this.sy(5 + this.ascent + row * 18);
    }

    protected int slotPx() {
        return 16 * this.scale;
    }

    protected static int pageCount(int perPage, int total) {
        return Math.max(1, (Math.max(0, total) + perPage - 1) / perPage);
    }

    protected static int pageBase(int page, int perPage, int total) {
        int base = page * perPage;
        if (base + perPage > total) {
            base = Math.max(0, total - perPage);
        }
        return Math.max(0, base);
    }

    protected boolean overSlot(double mx, double my, int col, int row) {
        int x = this.slotX(col);
        int y = this.slotY(row);
        int s = this.slotPx();
        return mx >= (double)x && mx < (double)(x + s) && my >= (double)y && my < (double)(y + s);
    }

    protected float updateAnim() {
        float t = Mth.clamp((float)((float)(System.currentTimeMillis() - this.openTime) / 220.0f), (float)0.0f, (float)1.0f);
        this.anim = 1.0f - (1.0f - t) * (1.0f - t);
        return this.anim;
    }

    protected void drawCastleBackground(GuiGraphics g) {
        this.drawParallaxBackground(g);
        if (peekBackground) {
            return;
        }
        float a = this.updateAnim();
        g.setColor(1.0f, 1.0f, 1.0f, Math.max(0.0f, a));
        g.blit(this.background, this.left, this.top, 256 * this.scale, 256 * this.scale, 0.0f, 0.0f, 256, 256, 256, 256);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawParallaxBackground(GuiGraphics g) {
        if (!PassBackgroundManager.render(g, this.width, this.height)) {
            float cover = Math.max((float)this.width / 1536.0f, (float)this.height / 1024.0f);
            int drawW = Math.round(1536.0f * cover);
            int drawH = Math.round(1024.0f * cover);
            int x = (this.width - drawW) / 2;
            int y = (this.height - drawH) / 2;
            g.blit(PASS_BG, x, y, drawW, drawH, 0.0f, 0.0f, 1536, 1024, 1536, 1024);
        }
        if (!peekBackground) {
            g.fill(0, 0, this.width, this.height, 0x44000000);
        }
    }

    protected void drawIcon(GuiGraphics g, ResourceLocation tex, int col, int row) {
        int s = this.slotPx();
        g.blit(tex, this.slotX(col), this.slotY(row), s, s, 0.0f, 0.0f, 16, 16, 16, 16);
    }

    protected void drawIconAt(GuiGraphics g, ResourceLocation tex, int x, int y) {
        int s = this.slotPx();
        g.blit(tex, x, y, s, s, 0.0f, 0.0f, 16, 16, 16, 16);
    }

    protected void drawProgressBar(GuiGraphics g, int txStart, int txEnd, int ty, int th, float frac) {
        int x0 = this.sx(txStart);
        int x1 = this.sx(txEnd);
        int y0 = this.sy(ty);
        int y1 = this.sy(ty + th);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, -15069432);
        g.fill(x0, y0, x1, y1, -15988475);
        int span = x1 - x0;
        int fill = Math.round((float)span * Mth.clamp((float)frac, (float)0.0f, (float)1.0f));
        if (fill > 0) {
            g.fillGradient(x0, y0, x0 + fill, y1, -15541, -2521057);
            g.fill(x0, y0, x0 + fill, y0 + Math.max(1, this.scale / 2), 0x66FFFFFF);
        }
        g.renderOutline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, -10864104);
    }

    protected void drawItem(GuiGraphics g, ItemStack stack, int col, int row, boolean decorations) {
        if (stack.isEmpty()) {
            return;
        }
        int x = this.slotX(col);
        int y = this.slotY(row);
        g.pose().pushPose();
        g.pose().translate((double)x, (double)y, 0.0);
        g.pose().scale((float)this.scale, (float)this.scale, 1.0f);
        g.renderItem(stack, 0, 0);
        if (decorations) {
            g.renderItemDecorations(this.font, stack, 0, 0);
        }
        g.pose().popPose();
    }

    protected void drawQuestSlot(GuiGraphics g, Quest q, int col, int row, int progress, boolean claimed) {
        boolean complete;
        this.drawIcon(g, CastleScreen.icon(1), col, row);
        int x = this.slotX(col);
        int y = this.slotY(row);
        int s = this.slotPx();
        boolean bl = complete = claimed || progress >= q.getTarget();
        if (complete) {
            g.fill(x, y, x + s, y + s, 0x3355FF55);
            g.drawString(this.font, "\u2714", x + s - 9, y + s - 9, -9445265, true);
        } else {
            float frac = Mth.clamp((float)((float)progress / (float)q.getTarget()), (float)0.0f, (float)1.0f);
            int bx = x + 2;
            int by = y + s - 5;
            int bw = s - 4;
            g.fill(bx, by, bx + bw, by + 3, -15853024);
            g.fill(bx, by, bx + Math.round((float)bw * frac), by + 3, -16718337);
        }
    }

    protected void drawQuestSlotLocked(GuiGraphics g, int col, int row) {
        int x = this.slotX(col);
        int y = this.slotY(row);
        int s = this.slotPx();
        g.setColor(1.0f, 1.0f, 1.0f, 0.4f);
        this.drawIcon(g, CastleScreen.icon(1), col, row);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        g.fill(x, y, x + s, y + s, 1427116056);
        int q = Math.max(8, s / 2);
        g.blit(CastleScreen.icon(5), x + (s - q) / 2, y + (s - q) / 2, q, q, 0.0f, 0.0f, 16, 16, 16, 16);
    }

    protected List<Component> questLockedTooltip(Quest q) {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)q.getDescription().copy().withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD}));
        l.add((Component)Component.translatable((String)"fantasticpass.quest.premium_only").withStyle(ChatFormatting.LIGHT_PURPLE));
        l.add((Component)Component.translatable((String)"fantasticpass.quest.points", (Object[])new Object[]{q.getPoints()}).withStyle(ChatFormatting.AQUA));
        l.add((Component)Component.translatable((String)"fantasticpass.gui.premium_hint").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
        return l;
    }

    protected List<Component> questTooltip(Quest q, int progress, boolean claimed) {
        ArrayList<Component> l = new ArrayList<Component>();
        l.add((Component)q.getDescription().copy().withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
        boolean complete = claimed || progress >= q.getTarget();
        l.add((Component)Component.translatable((String)"fantasticpass.quest.progress", (Object[])new Object[]{Math.min(progress, q.getTarget()), q.getTarget()}).withStyle(complete ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        l.add((Component)Component.translatable((String)"fantasticpass.quest.points", (Object[])new Object[]{q.getPoints()}).withStyle(ChatFormatting.AQUA));
        l.add((Component)(complete ? Component.translatable((String)"fantasticpass.quest.completed").withStyle(ChatFormatting.GREEN) : Component.translatable((String)"fantasticpass.quest.in_progress").withStyle(ChatFormatting.YELLOW)));
        return l;
    }

    protected void playClick(float pitch) {
        // Silenciado: el usuario solo quiere sonido al ENTRAR a menus, no en cada interaccion.
    }

    protected void playChime(float pitch) {
        // Silenciado: el sonido de apertura de menu ahora se dispara directamente en init().
    }

    protected void playClaimFx() {
        // Recompensa gratis: fanfarria epica de logro a volumen maximo.
        this.playSoundVol(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        this.playSoundVol(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    protected void playClaimFx(boolean premium) {
        if (premium) {
            // Recompensa premium: fanfarria de logro + levelup + activacion de faro, epico y a tope (sin totem).
            this.playSoundVol(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            this.playSoundVol(SoundEvents.PLAYER_LEVELUP, 1.15f, 1.0f);
            this.playSoundVol(SoundEvents.BEACON_ACTIVATE, 1.4f, 1.0f);
        } else {
            this.playClaimFx();
        }
    }

    private void playSoundVol(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)event, (float)pitch, (float)volume));
    }

    protected void playDenied() {
        // Silenciado: sin sonido en interacciones bloqueadas (solo al entrar a menus).
    }

    protected void playSound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)event, (float)pitch, (float)1.0f));
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    public boolean isPauseScreen() {
        return false;
    }
}

