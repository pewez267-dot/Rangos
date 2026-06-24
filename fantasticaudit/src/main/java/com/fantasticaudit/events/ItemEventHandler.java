package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.util.ItemSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Captures the ITEMS category: pickup, drop, use/consume and craft.
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemEventHandler {

    private ItemEventHandler() {
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!AuditConfig.LOG_ITEMS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();

        String data = ItemSerializer.itemId(stack) + " x" + stack.getCount()
                + " @(" + ItemSerializer.pos(itemEntity) + ") "
                + ItemSerializer.dimShort(player.level());

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "ITEM_PICKUP", data);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!AuditConfig.LOG_ITEMS.get()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();

        String data = ItemSerializer.itemId(stack) + " x" + stack.getCount()
                + " @(" + ItemSerializer.pos(itemEntity) + ") "
                + ItemSerializer.dimShort(player.level());

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "ITEM_DROP", data);
    }

    /**
     * Fires when a continuous-use item (food, potion, bow draw, etc.) finishes being used.
     * This is the authoritative "consumed" signal.
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!AuditConfig.LOG_ITEMS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();

        // quantity 1 represents the single consumption action; the residual stack count is the
        // post-use remainder and is intentionally not what we report here.
        String data = ItemSerializer.itemId(stack) + " x1 consumed"
                + " @(" + ItemSerializer.pos(player) + ")";

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "ITEM_USE", data);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!AuditConfig.LOG_ITEMS.get() || event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        String data = ItemSerializer.itemId(stack) + " x" + stack.getCount() + " interacted"
                + " @(" + ItemSerializer.pos(player) + ")";

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "ITEM_USE", data);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!AuditConfig.LOG_ITEMS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = event.getCrafting();

        String data = ItemSerializer.itemId(crafted) + " x" + crafted.getCount()
                + " recipe=" + resolveRecipeId(player.level(), event);

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "ITEM_CRAFT", data);
    }

    /**
     * The crafted event does not carry the recipe id directly, so we re-resolve it from the
     * crafting matrix when one is available. Falls back to {@code unknown} for non-crafting-grid
     * sources (furnaces, modded machines) where a CraftingContainer is not provided.
     */
    private static String resolveRecipeId(Level level, PlayerEvent.ItemCraftedEvent event) {
        if (event.getInventory() instanceof CraftingContainer craftingContainer) {
            Optional<CraftingRecipe> recipe =
                    level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
            if (recipe.isPresent()) {
                return recipe.get().getId().toString();
            }
        }
        return "unknown";
    }
}
