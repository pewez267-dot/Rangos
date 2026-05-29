package com.revivemod.client;

import com.revivemod.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
 *    PlayerEntityClientMixin), so YOU see yourself crawling and the camera drops
 *  - a small prompt just under the time bar (no chat, no inventory HUD)
 *  - SHIFT-hold = surrender, F = self-revive (4s each)
 */
public final class RevivemodClient implements ClientModInitializer {

    private static final int CHANNEL_TICKS = 80; // 4 s @ 20 tps

    /** True while the local player is in the downed/crawl state (set by the server). */
    public static volatile boolean LOCAL_DOWNED = false;

    private int sneakTicks = 0;
    private boolean selfActive = false;
    private int selfTicks = 0;
    private boolean prevSelfKey = false;

    private void reset() {
        LOCAL_DOWNED = false;
        sneakTicks = 0;
        selfActive = false;
        selfTicks = 0;
        prevSelfKey = false;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DOWN_START_ID, (payload, ctx) ->
                ctx.client().execute(() -> LOCAL_DOWNED = true));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DOWN_END_ID, (payload, ctx) ->
                ctx.client().execute(this::reset));

        // Reset the local flag on (dis)connect so a stale "downed" state can't
        // carry over into a new session / world.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

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

        // SHIFT-hold = surrender (4 s).
        if (mc.options.sneakKey.isPressed()) {
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
        int cx = mc.getWindow().getScaledWidth() / 2;
        int y = 22; // just under the boss bar (the time counter)

        Text t;
        if (sneakTicks > 0) {
            int pct = Math.min(100, sneakTicks * 100 / CHANNEL_TICKS);
            t = Text.literal("Rindiendote " + pct + "%").formatted(Formatting.RED);
        } else if (selfActive) {
            int pct = Math.min(100, selfTicks * 100 / CHANNEL_TICKS);
            t = Text.literal("Auto-reviviendo " + pct + "%").formatted(Formatting.GREEN);
        } else {
            t = Text.literal("SHIFT rendirse  ·  F auto-revivir").formatted(Formatting.GRAY);
        }
        ctx.drawCenteredTextWithShadow(tr, t, cx, y, 0xFFFFFFFF);
    }
}
