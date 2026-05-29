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
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/**
 * Optional client component (install alongside the server jar):
 *  - forces the local player into the crawl pose while downed (PlayerEntityClientMixin)
 *  - on-screen prompts + a progress bar (no chat, no inventory HUD)
 *  - hold E 4s = surrender, hold F 4s = self-revive
 *
 * Keys are polled directly from GLFW (not KeyBinding.isPressed) because "tap"
 * bindings like swap-hands (F) are consumed by vanilla each tick and their
 * isPressed() is unreliable -> that was the self-revive bug.
 */
public final class RevivemodClient implements ClientModInitializer {

    private static final int CHANNEL_TICKS = 80; // 4 s @ 20 tps

    public static volatile boolean LOCAL_DOWNED = false;
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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHud);
    }

    /** Reliable physical-key poll that respects rebinds. */
    private static boolean isHeld(MinecraftClient mc, KeyBinding kb) {
        InputUtil.Key key = InputUtil.fromTranslationKey(kb.getBoundKeyTranslationKey());
        long win = mc.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(win, key.getCode()) == GLFW.GLFW_PRESS;
        }
        int code = key.getCode();
        if (code == InputUtil.UNKNOWN_KEY.getCode()) return kb.isPressed();
        return InputUtil.isKeyPressed(win, code);
    }

    private void onTick(MinecraftClient mc) {
        if (!LOCAL_DOWNED || mc.player == null) {
            sneakTicks = 0;
            selfTicks = 0;
            return;
        }

        // Hold E (inventory key) 4s = surrender. The inventory screen itself is
        // blocked by MinecraftClientMixin while downed, so E is free to reuse.
        if (isHeld(mc, mc.options.inventoryKey)) {
            sneakTicks++;
            if (sneakTicks == CHANNEL_TICKS) {
                ClientPlayNetworking.send(new Payloads.SurrenderToggle());
                sneakTicks = 0;
            }
        } else {
            sneakTicks = 0;
        }

        // Hold F 4s = self-revive (only if affordable).
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        if (canAfford && isHeld(mc, mc.options.swapHandsKey)) {
            selfTicks++;
            if (selfTicks == CHANNEL_TICKS) {
                ClientPlayNetworking.send(new Payloads.SelfReviveToggle());
                selfTicks = 0; // wait for server DOWN_END; avoid double-send
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
        int y = 55; // lower, so a look-at tooltip mod doesn't cover it

        if (sneakTicks > 0) {
            drawBar(ctx, tr, cx, y, "Rindiendote", sneakTicks, 0xFFFF5555);
            return;
        }
        if (selfTicks > 0) {
            drawBar(ctx, tr, cx, y, "Auto-reviviendo", selfTicks, 0xFF55FF55);
            return;
        }

        // Idle prompts: only the key is coloured + bold; the label is plain
        // white and NOT bold. Build under a plain root so bold doesn't inherit.
        boolean canAfford = mc.player.experienceLevel >= SELF_COST;
        MutableText surrender = Text.literal("")
                .append(Text.literal("[E]").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" Rendirte").formatted(Formatting.WHITE));
        MutableText self = Text.literal("")
                .append(Text.literal("[F]").formatted(canAfford ? Formatting.GREEN : Formatting.DARK_GRAY, Formatting.BOLD))
                .append(Text.literal(" Auto-revivir (" + SELF_COST + " niveles de XP)").formatted(Formatting.WHITE));
        ctx.drawCenteredTextWithShadow(tr, surrender, cx, y, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, self, cx, y + 11, 0xFFFFFFFF);
    }

    /** v1.1.0-style text progress bar: Label [||||||....] 60% */
    private void drawBar(DrawContext ctx, TextRenderer tr, int cx, int y, String label, int ticks, int color) {
        int pct = Math.min(100, ticks * 100 / CHANNEL_TICKS);
        int filled = pct / 5; // 20-char bar
        StringBuilder b = new StringBuilder(20);
        for (int i = 0; i < 20; i++) b.append(i < filled ? '|' : '.');
        Text t = Text.literal(label + " ").formatted(Formatting.WHITE)
                .append(Text.literal("[" + b + "] " + pct + "%").formatted(Formatting.YELLOW));
        ctx.drawCenteredTextWithShadow(tr, t, cx, y, color);
    }
}
