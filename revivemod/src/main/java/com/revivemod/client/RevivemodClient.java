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
    /** XP level cost to self-revive (sent by the server on DownStart). */
    public static volatile int SELF_COST = 10;

    private int sneakTicks = 0;
    private int selfTicks = 0;

    private void reset() {
        LOCAL_DOWNED = false;
        sneakTicks = 0;
        selfTicks = 0;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DOWN_START_ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    LOCAL_DOWNED = true;
                    SELF_COST = payload.selfCost();
                    sneakTicks = 0;
                    selfTicks = 0;
                }));
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
            selfTicks = 0;
            return;
        }

        // Hold SHIFT 4s = surrender.
        if (mc.options.sneakKey.isPressed()) {
            sneakTicks++;
            if (sneakTicks == CHANNEL_TICKS) {
                ClientPlayNetworking.send(new Payloads.SurrenderToggle());
            }
        } else {
            sneakTicks = 0;
        }

        // Hold F 4s = self-revive (only if you can afford it).
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        if (mc.options.swapHandsKey.isPressed() && canAfford) {
            selfTicks++;
            if (selfTicks == CHANNEL_TICKS) {
                ClientPlayNetworking.send(new Payloads.SelfReviveToggle());
            }
        } else {
            selfTicks = 0;
        }
    }

    private void onHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        if (!LOCAL_DOWNED) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int y = 38; // a bit lower so a "looking at" tooltip mod doesn't cover it

        boolean canAfford = mc.player.experienceLevel >= SELF_COST;

        if (sneakTicks > 0) {
            int pct = Math.min(100, sneakTicks * 100 / CHANNEL_TICKS);
            Text t = Text.literal("Rindiendote " + pct + "%").formatted(Formatting.RED, Formatting.BOLD);
            ctx.drawCenteredTextWithShadow(tr, t, cx, y, 0xFFFF5555);
            return;
        }
        if (selfTicks > 0) {
            int pct = Math.min(100, selfTicks * 100 / CHANNEL_TICKS);
            Text t = Text.literal("Auto-reviviendo " + pct + "%").formatted(Formatting.GREEN, Formatting.BOLD);
            ctx.drawCenteredTextWithShadow(tr, t, cx, y, 0xFF55FF55);
            return;
        }

        // Idle: two coloured prompts on separate lines.
        Text surrender = Text.literal("[SHIFT] Rendirte").formatted(Formatting.RED, Formatting.BOLD);
        Text self = Text.literal("[F] Auto-revivir (" + SELF_COST + " niveles de XP)")
                .formatted(canAfford ? Formatting.GREEN : Formatting.GRAY,
                           canAfford ? Formatting.BOLD : Formatting.BOLD);
        ctx.drawCenteredTextWithShadow(tr, surrender, cx, y, 0xFFFF5555);
        ctx.drawCenteredTextWithShadow(tr, self, cx, y + 11, canAfford ? 0xFF55FF55 : 0xFFAAAAAA);
    }
}
