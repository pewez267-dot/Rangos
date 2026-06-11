package com.fscrates.client.screen;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side crate-opening animation player. A single screen that renders many
 * different animation styles driven by {@link CrateAnimation.Style} and themed
 * by {@link CrateAnimation.Theme}. It is deliberately self-contained and
 * data-driven so new animations require no new screens.
 *
 * <p>The reward is purely cosmetic here (the server already granted it); this
 * screen just provides the show. SHIFT (when allowed) skips to the reveal.
 */
public class AnimationScreen extends Screen {

    private final CrateAnimation animation;
    private final int rarityColor;
    private final ItemStack rewardIcon;
    private final String rewardLabel;
    private final List<ItemStack> candidates = new ArrayList<>();
    private final boolean allowSkip;

    private int ticks = 0;
    private boolean revealed = false;

    public AnimationScreen(String animationId, int rarityColor, CompoundTag rewardItem,
                           CompoundTag candidatesTag, boolean allowSkip) {
        super(Component.literal("Crate"));
        this.animation = AnimationRegistry.get(animationId);
        this.rarityColor = 0xFF000000 | (rarityColor & 0xFFFFFF);
        this.allowSkip = allowSkip;

        ItemStack icon = ItemStack.EMPTY;
        String label = "";
        if (rewardItem != null) {
            label = rewardItem.getString("label");
            CompoundTag copy = rewardItem.copy();
            copy.remove("label");
            if (!copy.isEmpty()) {
                icon = ItemStack.of(copy);
            }
        }
        this.rewardIcon = icon;
        this.rewardLabel = label;

        if (candidatesTag != null) {
            net.minecraft.nbt.ListTag list = candidatesTag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                candidates.add(ItemStack.of(list.getCompound(i)));
            }
        }
        if (candidates.isEmpty() && !icon.isEmpty()) {
            candidates.add(icon);
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= animation.durationTicks()) {
            revealed = true;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = this.width / 2;
        int cy = this.height / 2;
        float t = Math.min(1f, ticks / (float) Math.max(1, animation.durationTicks()));

        // soft themed backdrop
        g.fillGradient(0, 0, this.width, this.height, 0x90000000, 0xC0000000);

        if (animation.style() == CrateAnimation.Style.INSTANT) {
            revealed = true;
        }

        if (!revealed) {
            switch (animation.style()) {
                case ROULETTE, SLOT_MACHINE -> renderReel(g, cx, cy, t, animation.style());
                case SPIN, GALAXY_SWIRL, ORBIT -> renderOrbit(g, cx, cy, t);
                case ITEM_RAIN -> renderRain(g, cx, cy, t);
                case LOOT_EXPLOSION, FIREWORKS -> renderBurst(g, cx, cy, t, false);
                case BEAM_REVEAL, PORTAL, SUMMON_CIRCLE, WAVE_PULSE -> renderBeam(g, cx, cy, t);
                case CARD_FLIP -> renderCardFlip(g, cx, cy, t);
                case SHATTER -> renderShatter(g, cx, cy, t);
                default -> renderOrbit(g, cx, cy, t);
            }
            // title + skip hint
            g.drawCenteredString(font, "\u00A7l" + animation.displayName(), cx, 30, rarityColor);
            if (allowSkip) {
                g.drawCenteredString(font, "\u00A77Mant\u00e9n SHIFT para saltar", cx, this.height - 24, 0xA0A0A0);
            }
            if (allowSkip && hasShiftDown()) {
                ticks = animation.durationTicks();
            }
        } else {
            renderReveal(g, cx, cy);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------
    // Style renderers (lightweight, GuiGraphics-based)
    // ------------------------------------------------------------------

    private void renderReel(GuiGraphics g, int cx, int cy, float t, CrateAnimation.Style style) {
        // ease-out: fast then slow
        float eased = 1f - (1f - t) * (1f - t);
        int slots = 7;
        int spacing = 36;
        int frame = (int) (eased * candidates.size() * 6);
        boolean vertical = style == CrateAnimation.Style.SLOT_MACHINE;

        // frame box
        int half = (slots / 2) * spacing + 18;
        g.fill(cx - half, cy - 24, cx + half, cy + 24, 0xC0101018);
        g.fill(cx - 2, cy - 30, cx + 2, cy + 30, rarityColor); // center marker

        for (int i = -slots / 2; i <= slots / 2; i++) {
            if (candidates.isEmpty()) {
                break;
            }
            ItemStack icon = candidates.get(Math.floorMod(frame + i, candidates.size()));
            int px = vertical ? cx - 8 : cx + i * spacing - 8;
            int py = vertical ? cy + i * spacing - 8 : cy - 8;
            g.renderItem(icon, px, py);
        }
    }

    private void renderOrbit(GuiGraphics g, int cx, int cy, float t) {
        int n = Math.max(1, candidates.size());
        float spin = t * 12f;
        float radius = 70 * (1f - t) + 12;
        for (int i = 0; i < n; i++) {
            double ang = spin + (Math.PI * 2 * i / n);
            int px = cx + (int) (Math.cos(ang) * radius) - 8;
            int py = cy + (int) (Math.sin(ang) * radius) - 8;
            g.renderItem(candidates.get(i), px, py);
        }
        // condensing glow at the center
        int s = (int) (4 + t * 18);
        g.fill(cx - s, cy - s, cx + s, cy + s, (rarityColor & 0x00FFFFFF) | 0x50000000);
    }

    private void renderRain(GuiGraphics g, int cx, int cy, float t) {
        int n = Math.max(1, candidates.size());
        for (int i = 0; i < 10; i++) {
            ItemStack icon = candidates.get(i % n);
            int px = cx - 120 + (i * 240 / 10);
            int py = (int) (((t * 400) + i * 47) % (this.height + 40)) - 20;
            g.renderItem(icon, px, py);
        }
    }

    private void renderBurst(GuiGraphics g, int cx, int cy, float t, boolean reveal) {
        int n = Math.max(1, candidates.size());
        float radius = t * 120;
        for (int i = 0; i < 12; i++) {
            double ang = Math.PI * 2 * i / 12;
            int px = cx + (int) (Math.cos(ang) * radius) - 8;
            int py = cy + (int) (Math.sin(ang) * radius) - 8;
            g.renderItem(candidates.get(i % n), px, py);
        }
    }

    private void renderBeam(GuiGraphics g, int cx, int cy, float t) {
        int w = (int) (6 + t * 30);
        int alpha = (int) (0x40 + t * 0x90) << 24;
        g.fill(cx - w, 0, cx + w, this.height, (rarityColor & 0x00FFFFFF) | alpha);
        // rising ring
        int r = (int) (60 * (1f - t)) + 10;
        g.fill(cx - r, cy + 40 - 2, cx + r, cy + 40 + 2, rarityColor);
        if (!rewardIcon.isEmpty()) {
            g.renderItem(rewardIcon, cx - 8, cy - 8);
        }
    }

    private void renderCardFlip(GuiGraphics g, int cx, int cy, float t) {
        // simulate a horizontal flip by shrinking/expanding width
        float phase = (float) Math.abs(Math.cos(t * Math.PI));
        int w = (int) (40 * phase) + 2;
        g.fill(cx - w, cy - 56, cx + w, cy + 56, 0xF0202040);
        g.fill(cx - w, cy - 56, cx + w, cy - 52, rarityColor);
        g.fill(cx - w, cy + 52, cx + w, cy + 56, rarityColor);
        if (t > 0.5f && !rewardIcon.isEmpty()) {
            g.renderItem(rewardIcon, cx - 8, cy - 8);
        }
    }

    private void renderShatter(GuiGraphics g, int cx, int cy, float t) {
        int pieces = 14;
        for (int i = 0; i < pieces; i++) {
            double ang = Math.PI * 2 * i / pieces;
            int dist = (int) (t * 90);
            int px = cx + (int) (Math.cos(ang) * dist);
            int py = cy + (int) (Math.sin(ang) * dist);
            int s = 8 - (int) (t * 6);
            g.fill(px - s, py - s, px + s, py + s, rarityColor);
        }
        if (t > 0.6f && !rewardIcon.isEmpty()) {
            g.renderItem(rewardIcon, cx - 8, cy - 8);
        }
    }

    private void renderReveal(GuiGraphics g, int cx, int cy) {
        // glowing frame around the reward
        g.fill(cx - 40, cy - 40, cx + 40, cy + 40, (rarityColor & 0x00FFFFFF) | 0x40000000);
        g.fill(cx - 42, cy - 42, cx + 42, cy - 38, rarityColor);
        g.fill(cx - 42, cy + 38, cx + 42, cy + 42, rarityColor);
        g.fill(cx - 42, cy - 42, cx - 38, cy + 42, rarityColor);
        g.fill(cx + 38, cy - 42, cx + 42, cy + 42, rarityColor);

        if (!rewardIcon.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(2.0f, 2.0f, 1.0f);
            g.renderItem(rewardIcon, -8, -8);
            g.pose().popPose();
        }
        g.drawCenteredString(font, "\u00A7l\u00A7e\u00a1Recompensa!", cx, cy - 70, rarityColor);
        if (rewardLabel != null && !rewardLabel.isEmpty()) {
            g.drawCenteredString(font, "\u00A7f" + rewardLabel, cx, cy + 60, 0xFFFFFF);
        }
        g.drawCenteredString(font, "\u00A77Pulsa una tecla o clic para continuar", cx, this.height - 30, 0xA0A0A0);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (revealed) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (revealed) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
