package com.fantastickits.commands;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.GroupCommandData;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.data.PlayerData;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.security.SecurityManager;
import com.fantastickits.util.NBTSerializer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * /fkits test <name>
 * Tests kit claim eligibility for the executing player without actually claiming.
 * Shows detailed diagnostic information about the kit, player group, and claim status.
 * Any player can use this command to check their eligibility.
 */
public class TestCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "name");
        FantasticKits mod = FantasticKits.getInstance();
        KitData kitData = mod.getKitData();
        PlayerData playerData = mod.getPlayerData();
        LuckPermsIntegration luckPerms = mod.getLuckPermsIntegration();
        GroupCommandData groupCmdData = mod.getGroupCommandData();

        // Check kit exists
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit '" + kitName + "' does not exist."));
            return 0;
        }

        // Build diagnostic report
        player.sendSystemMessage(Component.literal("§6§l=== Kit Test: " + kit.getName() + " ==="));
        player.sendSystemMessage(Component.literal(""));

        // Kit info
        player.sendSystemMessage(Component.literal("§eKit Name: §f" + kit.getName()));
        player.sendSystemMessage(Component.literal("§eAssigned Group: §f" + (kit.getAssignedGroup().isEmpty() ? "NONE" : kit.getAssignedGroup())));
        player.sendSystemMessage(Component.literal("§eItems Count: §f" + kit.getItemNbtList().size()));

        // Show items summary
        List<CompoundTag> items = kit.getItemsAsNbt();
        for (int i = 0; i < items.size() && i < 9; i++) {
            ItemStack stack = NBTSerializer.deserializeItemStack(items.get(i));
            if (!stack.isEmpty()) {
                player.sendSystemMessage(Component.literal("  §7- " + stack.getHoverName().getString() + " x" + stack.getCount()));
            }
        }
        if (items.size() > 9) {
            player.sendSystemMessage(Component.literal("  §7... and " + (items.size() - 9) + " more items"));
        }

        player.sendSystemMessage(Component.literal(""));

        // Player status
        player.sendSystemMessage(Component.literal("§e--- Player Status ---"));
        boolean claimed = playerData.hasClaimed(player.getUUID(), kitName);
        player.sendSystemMessage(Component.literal("§eAlready Claimed: " + (claimed ? "§cYES" : "§aNO")));

        if (luckPerms.isAvailable()) {
            String primaryGroup = luckPerms.getPrimaryGroup(player.getUUID());
            List<String> playerGroups = luckPerms.getPlayerGroups(player.getUUID());
            player.sendSystemMessage(Component.literal("§ePrimary Group: §f" + primaryGroup));
            player.sendSystemMessage(Component.literal("§eAll Groups: §f" + String.join(", ", playerGroups)));

            if (!kit.getAssignedGroup().isEmpty()) {
                boolean inGroup = luckPerms.playerInGroup(player.getUUID(), kit.getAssignedGroup());
                player.sendSystemMessage(Component.literal("§eIn Required Group: " + (inGroup ? "§aYES" : "§cNO")));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cLuckPerms: NOT AVAILABLE"));
        }

        player.sendSystemMessage(Component.literal(""));

        // Command associations
        if (!kit.getAssignedGroup().isEmpty()) {
            Set<String> commands = groupCmdData.getCommandsForGroup(kit.getAssignedGroup());
            player.sendSystemMessage(Component.literal("§e--- Associated Commands (" + commands.size() + ") ---"));
            if (commands.isEmpty()) {
                player.sendSystemMessage(Component.literal("  §7(none)"));
            } else {
                for (String cmd : commands) {
                    player.sendSystemMessage(Component.literal("  §7- /" + cmd));
                }
            }
        }

        player.sendSystemMessage(Component.literal(""));

        // Final eligibility
        SecurityManager.ClaimResult result = SecurityManager.validateClaim(player, kitName);
        if (result.isAllowed()) {
            player.sendSystemMessage(Component.literal("§a§lELIGIBLE: You can claim this kit."));
        } else {
            player.sendSystemMessage(Component.literal("§c§lNOT ELIGIBLE: " + result.getReason()));
        }

        player.sendSystemMessage(Component.literal("§6§l================================"));

        return Command.SINGLE_SUCCESS;
    }
}
