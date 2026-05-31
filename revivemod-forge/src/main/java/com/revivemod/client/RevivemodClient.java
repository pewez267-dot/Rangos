package com.revivemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.revivemod.network.ReviveNetwork;
import com.revivemod.network.SelfReviveTogglePacket;
import com.revivemod.network.SurrenderTogglePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Pose;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only state + behaviour: HUD overlay, hold-to-confirm key handling (E surrender / F self),
 * and forcing the local crawl pose. Driven by S2C DownStart/DownEnd packets.
 * Replaces Fabric's RevivemodClient + the client mixins.
 */
public final class RevivemodClient {
    private static final int CHANNEL_TICKS = 80;
    public static volatile boolean LOCAL_DOWNED = false;
    public static volatile int SELF_COST = 10;
    private static int sneakTicks = 0;
    private static int selfTicks = 0;

    private RevivemodClient() {
    }

    public static void reset() {
        LOCAL_DOWNED = false;
        sneakTicks = 0;
        selfTicks = 0;
    }

    public static void onDownStart(int selfCost) {
        LOCAL_DOWNED = true;
        SELF_COST = selfCost;
        sneakTicks = 0;
        selfTicks = 0;
    }

    public static void onDownEnd() {
        reset();
    }

    private static boolean isHeld(Minecraft mc, KeyMapping kb) {
        InputConstants.Key key = InputConstants.getKey(kb.saveString());
        long win = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(win, key.getValue()) == 1;
        }
        int code = key.getValue();
        if (code == InputConstants.UNKNOWN.getValue()) {
            return kb.isDown();
        }
        return InputConstants.isKeyDown(win, code);
    }

    public static void onClientTick(Minecraft mc) {
        if (!LOCAL_DOWNED || mc.player == null) {
            sneakTicks = 0;
            selfTicks = 0;
            return;
        }
        if (isHeld(mc, mc.options.keyInventory)) {
            ++sneakTicks;
            if (sneakTicks == CHANNEL_TICKS) {
                ReviveNetwork.CHANNEL.sendToServer(new SurrenderTogglePacket());
                sneakTicks = 0;
            }
        } else {
            sneakTicks = 0;
        }
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        if (canAfford && isHeld(mc, mc.options.keySwapOffhand)) {
            ++selfTicks;
            if (selfTicks == CHANNEL_TICKS) {
                ReviveNetwork.CHANNEL.sendToServer(new SelfReviveTogglePacket());
                selfTicks = 0;
            }
        } else {
            selfTicks = 0;
        }
        // Force the local crawl pose for the downed player's own view.
        mc.player.setPose(Pose.SWIMMING);
    }

    public static void onHud(GuiGraphics ctx, int screenWidth) {
        if (!LOCAL_DOWNED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Font tr = mc.font;
        int cx = screenWidth / 2;
        int y = 55;
        if (sneakTicks > 0) {
            drawBar(ctx, tr, cx, y, "Rindi\u00e9ndote", sneakTicks, -43691);
            return;
        }
        if (selfTicks > 0) {
            drawBar(ctx, tr, cx, y, "Auto-revivi\u00e9ndote", selfTicks, -11141291);
            return;
        }
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        MutableComponent surrender = Component.literal("")
                .append(Component.literal("[E]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" Rendirte").withStyle(ChatFormatting.WHITE));
        MutableComponent self = Component.literal("")
                .append(Component.literal("[F]").withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .append(Component.literal(" Auto-revivir (" + SELF_COST + " niveles de XP)").withStyle(ChatFormatting.WHITE));
        ctx.drawCenteredString(tr, surrender, cx, y, -1);
        ctx.drawCenteredString(tr, self, cx, y + 11, -1);
    }

    private static void drawBar(GuiGraphics ctx, Font tr, int cx, int y, String label, int ticks, int color) {
        int pct = Math.min(100, ticks * 100 / CHANNEL_TICKS);
        int filled = pct / 5;
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; ++i) {
            b.append(i < filled ? '|' : '.');
        }
        MutableComponent t = Component.literal(label + " ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("[" + b + "] " + pct + "%").withStyle(ChatFormatting.YELLOW));
        ctx.drawCenteredString(tr, t, cx, y, color);
    }
}
