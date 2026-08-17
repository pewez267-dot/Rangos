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
        RevivemodClient.reset();
    }

    /**
     * Detecta si la tecla esta fisicamente pulsada, sin importar conflictos de
     * keybind con otras acciones (lee el estado real de GLFW).
     */
    private static boolean isHeld(Minecraft mc, KeyMapping kb) {
        InputConstants.Key key = InputConstants.getKey(kb.saveString());
        long win = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(win, key.getValue()) == GLFW.GLFW_PRESS;
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
        // Rendirse: mantener la tecla dedicada (por defecto E) durante 4s.
        if (RevivemodClient.isHeld(mc, RevivemodKeybinds.SURRENDER)) {
            if (++sneakTicks == CHANNEL_TICKS) {
                ReviveNetwork.CHANNEL.sendToServer(new SurrenderTogglePacket());
                sneakTicks = 0;
            }
        } else {
            sneakTicks = 0;
        }
        // Auto-revivir: mantener la tecla dedicada (por defecto F) durante 4s.
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        if (canAfford && RevivemodClient.isHeld(mc, RevivemodKeybinds.SELF_REVIVE)) {
            if (++selfTicks == CHANNEL_TICKS) {
                ReviveNetwork.CHANNEL.sendToServer(new SelfReviveTogglePacket());
                selfTicks = 0;
            }
        } else {
            selfTicks = 0;
        }
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
            RevivemodClient.drawBar(ctx, tr, cx, y, "Rindi\u00e9ndote", sneakTicks, 0xFFFF5555);
            return;
        }
        if (selfTicks > 0) {
            RevivemodClient.drawBar(ctx, tr, cx, y, "Auto-revivi\u00e9ndote", selfTicks, 0xFF55AAFF);
            return;
        }
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        // Muestra la tecla REALMENTE asignada (no un texto fijo).
        String surrKey = keyName(RevivemodKeybinds.SURRENDER);
        String selfKey = keyName(RevivemodKeybinds.SELF_REVIVE);
        MutableComponent surrender = Component.literal("")
                .append(Component.literal("[" + surrKey + "]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" Rendirte").withStyle(ChatFormatting.WHITE));
        MutableComponent self = Component.literal("")
                .append(Component.literal("[" + selfKey + "]").withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .append(Component.literal(" Auto-revivir (" + SELF_COST + " niveles de XP)").withStyle(ChatFormatting.WHITE));
        ctx.drawCenteredString(tr, surrender, cx, y, 0xFFFFFFFF);
        ctx.drawCenteredString(tr, self, cx, y + 11, 0xFFFFFFFF);
    }

    private static String keyName(KeyMapping km) {
        return km.getTranslatedKeyMessage().getString();
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
