package com.revivemod.client;

import com.revivemod.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Optional client component (install alongside the server jar):
 *  - forces the local player into the crawl pose while downed (see
 *    PlayerEntityClientMixin), so YOU see yourself crawling, not standing
 *  - small HUD prompt at the bottom of the screen (no chat, no GUI)
 *  - SHIFT-hold = surrender, F = self-revive (3s each)
 *
 * The server tells us when we are downed via the DownStart / DownEnd payloads.
 */
public final class RevivemodClient implements ClientModInitializer {

    private static final int CHANNEL_TICKS = 60; // 3 s @ 20 tps

    /** True while the local player is in the downed/crawl state (set by the server). */
    public static volatile boolean LOCAL_DOWNED = false;

    private int sneakTicks = 0;
    private boolean selfActive = false;
    private int selfTicks = 0;
    private boolean prevSelfKey = false;

    @Override
    public void onInitializeClient() {
        // NOTE: payload TYPES are registered once in the common ReviveMod (runs
        // on both sides). Here we only register the client-side receivers.
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DOWN_START_ID, (payload, ctx) ->
                ctx.client().execute(() -> LOCAL_DOWNED = true));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DOWN_END_ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    LOCAL_DOWNED = false;
                    sneakTicks = 0;
                    selfActive = false;
                    selfTicks = 0;
                }));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHud);
    }

    private void onTick(MinecraftClient mc) {
        if (!LOCAL_DOWNED || mc.player == null) {
            sneakTicks = 0;
            selfActive = false;
            selfTicks = 0;
            prevSelfKey = false;
            return;
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
        if (!LOCAL_DOWNED) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int y = sh - 40;

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
