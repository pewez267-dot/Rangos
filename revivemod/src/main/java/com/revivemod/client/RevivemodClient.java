package com.revivemod.client;

import com.revivemod.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.BedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Optional client component:
 *  - small HUD prompt at the bottom of the screen (no chat clutter, no GUI)
 *  - SHIFT-hold = surrender, F = self-revive (3s each), sent via custom payloads
 *  - only active while the local player is in the bleeding state (sleeping pose
 *    on a block that is NOT a real bed)
 *
 * Vanilla clients still work without this; they fall back to the chat buttons.
 */
public final class RevivemodClient implements ClientModInitializer {

    private static final int CHANNEL_TICKS = 60; // 3 s @ 20 tps

    private int sneakTicks = 0;
    private boolean selfActive = false;
    private int selfTicks = 0;
    private boolean prevSelfKey = false;
    /** Set to true between F-press start and F-completion so the HUD shows the cast bar. */

    /** Public flag read by MinecraftClientMixin to suppress SleepingChatScreen. */
    public static volatile boolean SUPPRESS_SLEEP_SCREEN = false;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(Payloads.SURRENDER_ID, Payloads.SurrenderToggle.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SELF_ID, Payloads.SelfReviveToggle.CODEC);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHud);
    }

    private static boolean isBleeding(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return false;
        if (!p.isSleeping()) return false;
        BlockPos pos = p.getSleepingPosition().orElse(null);
        if (pos == null) return false;
        return !(mc.world.getBlockState(pos).getBlock() instanceof BedBlock);
    }

    private void onTick(MinecraftClient mc) {
        if (!isBleeding(mc)) {
            sneakTicks = 0;
            selfActive = false;
            selfTicks = 0;
            prevSelfKey = false;
            SUPPRESS_SLEEP_SCREEN = false;
            return;
        }
        SUPPRESS_SLEEP_SCREEN = true;
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.SleepingChatScreen) {
            mc.setScreen(null);
        }

        // SHIFT-hold = surrender (3 s).
        boolean sneak = mc.options.sneakKey.isPressed();
        if (sneak) {
            sneakTicks++;
            if (sneakTicks == CHANNEL_TICKS) {
                ClientPlayNetworking.send(new Payloads.SurrenderToggle());
            }
        } else {
            sneakTicks = 0;
        }

        // F edge-press toggles the self-revive cast.
        boolean fNow = mc.options.swapHandsKey.isPressed();
        if (fNow && !prevSelfKey) {
            selfActive = !selfActive;
            selfTicks = 0;
            ClientPlayNetworking.send(new Payloads.SelfReviveToggle());
        }
        prevSelfKey = fNow;
        if (selfActive) {
            selfTicks++;
            if (selfTicks >= CHANNEL_TICKS) {
                selfActive = false;
                selfTicks = 0;
            }
        }
    }

    private void onHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isBleeding(mc)) return;

        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int y = sh - 38;

        Text t;
        if (sneakTicks > 0) {
            int pct = Math.min(100, sneakTicks * 100 / CHANNEL_TICKS);
            t = Text.literal("Rindiendote " + pct + "%").formatted(Formatting.RED);
        } else if (selfActive) {
            int pct = Math.min(100, selfTicks * 100 / CHANNEL_TICKS);
            t = Text.literal("Auto-reviviendo " + pct + "%").formatted(Formatting.GREEN);
        } else {
            t = Text.literal("[SHIFT] ").formatted(Formatting.RED, Formatting.BOLD)
                    .append(Text.literal("rendirse   ").formatted(Formatting.GRAY))
                    .append(Text.literal("[F] ").formatted(Formatting.GREEN, Formatting.BOLD))
                    .append(Text.literal("auto-revivir").formatted(Formatting.GRAY));
        }
        ctx.drawCenteredTextWithShadow(tr, t, sw / 2, y, 0xFFFFFFFF);
    }
}
