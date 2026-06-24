package com.fantasticchest.item;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.gui.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Editor wand. Right-clicking a Fantastic Chest with it (as an OP) opens the admin GUI
 * (Interface 1) in edit mode. Carries the {@code fantasticchest:editor_wand} NBT marker.
 * Only obtainable via {@code /fschest editor give}.
 */
public final class EditorWandItem extends Item {

    public static final String MARKER = "fantasticchest:editor_wand";

    public EditorWandItem(final Properties properties) {
        super(properties);
    }

    /** Builds a tagged editor wand stack. */
    public static ItemStack buildWand() {
        final ItemStack stack = new ItemStack(ModItems.EDITOR_WAND.get());
        stack.getOrCreateTag().putByte(MARKER, (byte) 1);
        return stack;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.hasPermissions(4)) {
            return InteractionResult.PASS;
        }
        final BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof ChestBlockEntity chest) {
            ModMenus.openAdminEdit(serverPlayer, chest);
            return InteractionResult.CONSUME;
        }
        serverPlayer.sendSystemMessage(Component.literal("§eApunta la varita a un Fantastic Chest colocado."));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§7Click derecho sobre un Fantastic Chest para editarlo (solo OP)."));
    }
}
