package com.fantastickits.commands;

import com.fantastickits.config.FKConfig;
import com.fantastickits.data.GroupCommandStore;
import com.fantastickits.data.Kit;
import com.fantastickits.data.KitRegistry;
import com.fantastickits.data.KitService;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.network.FKNetwork;
import com.fantastickits.network.OpenKitEditorPacket;
import com.fantastickits.security.AuditLog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The five and only commands of Fantastic Kits, registered through Brigadier:
 *
 * <pre>
 *   /fskits create                (admin) create a kit and open its editor
 *   /fskits edit   &lt;kit&gt;           (admin) open the editor for an existing kit
 *   /fskits delete &lt;kit&gt;           (admin) delete a kit
 *   /fskits get    &lt;kit&gt;           (player) claim your kit (once per UUID, group-gated)
 *   /fskits get    &lt;kit&gt; &lt;player&gt;  (admin) manually deliver / restock a kit
 *   /fskits test   &lt;kit&gt;           (admin) receive a kit for testing, without claiming
 * </pre>
 *
 * Every action is validated server-side; the editor commands additionally require the
 * executor to be a player because the editor is a client-side screen.
 */
public final class FKitsCommand {

    private FKitsCommand() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fskits")
                .executes(FKitsCommand::help);

        // /fskits create  (abre la GUI directamente; el ID se define en la pestana Info)
        root.then(Commands.literal("create")
                .requires(FKitsCommand::isAdmin)
                .executes(FKitsCommand::create));

        // /fskits edit <kit>
        root.then(Commands.literal("edit")
                .requires(FKitsCommand::isAdmin)
                .executes(c -> usage(c, "/fskits edit <kit>"))
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(FKitsCommand::suggestKits)
                        .executes(FKitsCommand::edit)));

        // /fskits delete <kit>
        root.then(Commands.literal("delete")
                .requires(FKitsCommand::isAdmin)
                .executes(c -> usage(c, "/fskits delete <kit>"))
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(FKitsCommand::suggestKits)
                        .executes(FKitsCommand::delete)));

        // /fskits get <kit>            -> reclamo del propio jugador
        // /fskits get <kit> <jugador>  -> reposicion admin (el primer argumento solo sugiere kits)
        root.then(Commands.literal("get")
                .executes(c -> usage(c, "/fskits get <kit>   |   /fskits get <kit> <jugador>"))
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(FKitsCommand::suggestKits)
                        .executes(FKitsCommand::selfClaim)
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(FKitsCommand::isAdmin)
                                .executes(FKitsCommand::adminGet))));

        // /fskits test <kit>
        root.then(Commands.literal("test")
                .requires(FKitsCommand::isAdmin)
                .executes(c -> usage(c, "/fskits test <kit>"))
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests(FKitsCommand::suggestKits)
                        .executes(FKitsCommand::test)));

        dispatcher.register(root);
    }

    private static boolean isAdmin(final CommandSourceStack source) {
        return source.hasPermission(FKConfig.adminPermissionLevel());
    }

    private static CompletableFuture<Suggestions> suggestKits(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(KitRegistry.get().ids(), builder);
    }

    private static int usage(final CommandContext<CommandSourceStack> ctx, final String usage) {
        ctx.getSource().sendSystemMessage(Component.literal("§eUso: §f" + usage));
        return 1;
    }

    private static int help(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        source.sendSystemMessage(Component.literal("§d\u2726 §fFantastic Kits §d\u2726 §7comandos:"));
        source.sendSystemMessage(Component.literal("§e/fskits create §7- crea un kit y abre el editor"));
        source.sendSystemMessage(Component.literal("§e/fskits edit <kit> §7- edita un kit existente"));
        source.sendSystemMessage(Component.literal("§e/fskits delete <kit> §7- elimina un kit"));
        source.sendSystemMessage(Component.literal("§e/fskits get <kit> §7- reclama tu kit (1 vez por jugador)"));
        source.sendSystemMessage(Component.literal("§e/fskits get <kit> <jugador> §7- (admin) entrega/repone un kit"));
        source.sendSystemMessage(Component.literal("§e/fskits test <kit> §7- (admin) prueba un kit sin reclamarlo"));
        return 1;
    }

    private static ServerPlayer requirePlayer(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (final Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cEste comando debe ejecutarlo un jugador."));
            return null;
        }
    }

    // ---- editor commands -----------------------------------------------------

    private static int create(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        // Abre el editor directamente; el ID y el nombre se definen en la pestana Info.
        openEditor(player, new Kit());
        ctx.getSource().sendSystemMessage(Component.literal("§aAbriendo el editor de kit nuevo..."));
        return 1;
    }

    private static int edit(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        final String id = Kit.normalizeId(StringArgumentType.getString(ctx, "kit"));
        final Kit kit = KitRegistry.get().get(id);
        if (kit == null) {
            ctx.getSource().sendFailure(Component.literal("§cEl kit §e" + id + " §cno existe."));
            return 0;
        }
        openEditor(player, kit);
        return 1;
    }

    private static void openEditor(final ServerPlayer player, final Kit kit) {
        final List<String> groups = LuckPermsIntegration.groupNames();
        final List<String> assigned = kit.hasGroup()
                ? new ArrayList<>(GroupCommandStore.get().commandsFor(kit.group))
                : new ArrayList<>();
        FKNetwork.sendToClient(player, new OpenKitEditorPacket(kit.toNbt(), groups, assigned));
    }

    private static int delete(final CommandContext<CommandSourceStack> ctx) {
        final String id = Kit.normalizeId(StringArgumentType.getString(ctx, "kit"));
        final Kit kit = KitRegistry.get().get(id);
        final boolean removed = KitRegistry.get().remove(id);
        // When the deleted kit's rank is no longer used by any kit, drop its command gating
        // and the LuckPerms nodes we registered for it.
        if (removed && kit != null && kit.hasGroup()) {
            boolean stillUsed = false;
            for (final Kit other : KitRegistry.get().all()) {
                if (other.hasGroup() && kit.group.equalsIgnoreCase(other.group)) {
                    stillUsed = true;
                    break;
                }
            }
            if (!stillUsed) {
                GroupCommandStore.get().removeGroup(kit.group);
                if (FKConfig.manageLuckPermsPermissions()) {
                    LuckPermsIntegration.clearGroupCommandNodes(kit.group);
                }
            }
        }
        final ServerPlayer player = ctx.getSource().getPlayer();
        AuditLog.kitDeleted(player != null ? player.getUUID() : null, ctx.getSource().getTextName(), id, removed);
        ctx.getSource().sendSystemMessage(Component.literal(removed
                ? "§aKit §e" + id + " §aeliminado."
                : "§cEl kit §e" + id + " §cno existe."));
        return removed ? 1 : 0;
    }

    // ---- claim / give / test -------------------------------------------------

    private static int selfClaim(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        KitService.claim(player, StringArgumentType.getString(ctx, "kit"));
        return 1;
    }

    private static int adminGet(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(ctx, "target");
        } catch (final Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cJugador no encontrado."));
            return 0;
        }
        final String kit = StringArgumentType.getString(ctx, "kit");
        return KitService.adminGet(ctx.getSource(), target, kit) ? 1 : 0;
    }

    private static int test(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        return KitService.test(player, StringArgumentType.getString(ctx, "kit")) ? 1 : 0;
    }
}
