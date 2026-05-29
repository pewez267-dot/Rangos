package com.revivemod.event;

import com.revivemod.state.DownManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

/**
 * While a player is downed they can do NOTHING except crawl: no breaking blocks,
 * no placing/using blocks, no using items (eat/drink/bow/etc.), no attacking, no
 * interacting with entities. Hotbar slot is locked in DownManager.enforceProne.
 */
public final class RestrictionHandler {

    private RestrictionHandler() {}

    private static boolean down(net.minecraft.entity.player.PlayerEntity player) {
        return player instanceof ServerPlayerEntity sp && DownManager.isDown(sp);
    }

    public static void register() {
        // Block breaking blocks.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !down(player));

        // Block left-click on blocks.
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                down(player) ? ActionResult.FAIL : ActionResult.PASS);

        // Block right-click on blocks (placing / opening containers).
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                down(player) ? ActionResult.FAIL : ActionResult.PASS);

        // Block using items (eating, drinking, bows, ender pearls, etc.).
        UseItemCallback.EVENT.register((player, world, hand) ->
                down(player)
                        ? TypedActionResult.fail(player.getStackInHand(hand))
                        : TypedActionResult.pass(player.getStackInHand(hand)));

        // Block attacking entities.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                down(player) ? ActionResult.FAIL : ActionResult.PASS);

        // Block a downed player interacting with entities (the revive handler
        // only fires when the ACTOR is not downed).
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                down(player) ? ActionResult.FAIL : ActionResult.PASS);
    }
}
