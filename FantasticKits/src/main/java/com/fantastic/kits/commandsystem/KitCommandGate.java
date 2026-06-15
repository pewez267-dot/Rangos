package com.fantastic.kits.commandsystem;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.kits.KitClaimService;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Runtime barrier for kit-bound commands.
 *
 * <p>The chain is the following: when a player runs {@code /someCmd}, Forge
 * fires {@link CommandEvent}. We extract the literal root name, look up which
 * kits are bound to it across the catalogue, and verify the player's primary
 * group matches at least one of those kits. If not, the command is cancelled
 * and the attempt is logged as {@code COMMAND_ACCESS_DENIED}.
 *
 * <p>Commands that are not bound to any kit are never affected.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class KitCommandGate {

    private KitCommandGate() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        try {
            CommandContext<CommandSourceStack> ctx = event.getParseResults().getContext().build(
                    event.getParseResults().getReader().getString());
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayer();
            if (player == null) return; // console & datapacks bypass the kit gate

            String rootName = rootLiteral(ctx);
            if (rootName.isEmpty()) return;
            // The mod's own command must always work for ops.
            if (rootName.equalsIgnoreCase(Reference.COMMAND_ROOT)) return;

            String key = KitClaimService.normalizeCommandKey(rootName);
            Optional<Kit> bound = FantasticKits.kits().all().stream()
                    .filter(k -> k.commands().stream().anyMatch(c -> KitClaimService.normalizeCommandKey(c).equals(key)))
                    .findFirst();
            if (bound.isEmpty()) return; // not a kit-bound command, allow

            // For at least one bound kit, the player must be the exact primary group.
            String playerGroup = FantasticKits.luckPerms().primaryGroup(player.getUUID());
            boolean allowed = FantasticKits.kits().all().stream()
                    .filter(k -> k.commands().stream().anyMatch(c -> KitClaimService.normalizeCommandKey(c).equals(key)))
                    .anyMatch(k -> FantasticKits.luckPerms().isPrimaryGroupExactly(player.getUUID(), k.ownerGroup()));
            if (!allowed) {
                event.setCanceled(true);
                FantasticKits.security().log(SecurityEventType.COMMAND_ACCESS_DENIED,
                        player, playerGroup, bound.get().ownerGroup(), bound.get(),
                        "/" + rootName, "BLOCKED",
                        "Player primary group does not match any kit owning this command.");
                player.sendSystemMessage(Component.literal(FantasticKits.config().chatPrefix +
                        ChatFormatting.RED + "Solo el grupo " + ChatFormatting.GOLD +
                        bound.get().ownerGroup() + ChatFormatting.RED + " puede usar este comando."));
            }
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("KitCommandGate failure", t);
        }
    }

    @SuppressWarnings("rawtypes")
    private static String rootLiteral(CommandContext<CommandSourceStack> ctx) {
        // Walk down the parsed nodes until we find the first literal.
        CommandContext<CommandSourceStack> cursor = ctx;
        while (cursor != null) {
            for (var node : cursor.getNodes()) {
                CommandNode<?> n = node.getNode();
                if (n instanceof LiteralCommandNode<?> lit) {
                    return lit.getLiteral();
                }
            }
            cursor = cursor.getChild();
        }
        return "";
    }
}
