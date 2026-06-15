package com.fantastickits.commands;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.security.SecurityManager;
import com.fantastickits.util.NBTSerializer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * /fkits get <player> <kit>
 * Admin command to give a kit to a player manually (bypasses claim restrictions).
 * This is the ONLY way to replenish lost items.
 * Requires admin permission. Does NOT mark kit as claimed again.
 */
public class GetCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Validate source is a player (for permission check)
        ServerPlayer adminPlayer;
        try {
            adminPlayer = source.getPlayerOrException();
        } catch (Exception e) {
            // Allow console to use this command
            adminPlayer = null;
        }

        // Check admin permission if executed by player
        if (adminPlayer != null && !SecurityManager.hasAdminPermission(adminPlayer)) {
            source.sendFailure(Component.literal("§cYou do not have permission to give kits."));
            return 0;
        }

        // Get target player
        ServerPlayer targetPlayer;
        try {
            targetPlayer = EntityArgument.getPlayer(context, "player");
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cPlayer not found or not online."));
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "kit");
        KitData kitData = FantasticKits.getInstance().getKitData();

        // Verify kit exists
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit '" + kitName + "' does not exist."));
            return 0;
        }

        // Give items to target player
        List<CompoundTag> itemTags = kit.getItemsAsNbt();
        if (itemTags.isEmpty()) {
            source.sendFailure(Component.literal("§cKit '" + kit.getName() + "' has no items."));
            return 0;
        }

        int given = 0;
        for (CompoundTag tag : itemTags) {
            ItemStack stack = NBTSerializer.deserializeItemStack(tag);
            if (!stack.isEmpty()) {
                if (!targetPlayer.getInventory().add(stack)) {
                    // Drop on ground if inventory full
                    targetPlayer.drop(stack, false);
                }
                given++;
            }
        }

        // Notify target player
        targetPlayer.sendSystemMessage(Component.literal("§aYou have received the kit '" + kit.getName() + "' from an administrator."));

        source.sendSuccess(() -> Component.literal("§aGave kit '" + kit.getName() + "' (" + given + " items) to " + targetPlayer.getName().getString() + "."), true);
        return Command.SINGLE_SUCCESS;
    }
}
