package com.fantastic.kits.commands;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.audit.AuditEventType;
import com.fantastic.kits.gui.GuiOpener;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.kits.KitClaimService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Single, locked-down command tree.
 *
 * <p>The mod ships exactly five subcommands and intentionally registers no
 * aliases and no hidden helpers, exactly as the spec demands.
 */
public final class FKitsCommand {

    private FKitsCommand() {}

    /** Suggests existing kit ids, ranked alphabetically. */
    private static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    FantasticKits.kits().all().stream().map(Kit::id).toList(),
                    builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Reference.COMMAND_ROOT)
                .requires(src -> src.hasPermission(Reference.OP_LEVEL))
                // /fkits  -> open the main administrative kit list (edit mode by default)
                .executes(FKitsCommand::executeRoot)

                // /fkits create
                .then(Commands.literal("create").executes(FKitsCommand::executeCreate))

                // /fkits edit [kitId]
                .then(Commands.literal("edit")
                        .executes(FKitsCommand::executeEditList)
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::executeEdit)))

                // /fkits delete [kitId]
                .then(Commands.literal("delete")
                        .executes(FKitsCommand::executeDeleteList)
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::executeDelete)))

                // /fkits get <kitId>
                .then(Commands.literal("get")
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::executeGet)))

                // /fkits test [kitId]
                .then(Commands.literal("test")
                        .executes(FKitsCommand::executeTestList)
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::executeTest)));

        dispatcher.register(root);
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------

    private static int executeRoot(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        GuiOpener.openList(player, GuiOpener.ListMode.EDIT);
        return 1;
    }

    private static int executeCreate(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        // Open the editor with a brand-new in-memory kit. It only persists when
        // the operator hits "Save" in the screen, mirroring FantasticCrates.
        Kit fresh = new Kit("kit_" + (System.currentTimeMillis() % 100000L),
                "Nuevo Kit",
                FantasticKits.config().defaultGroupName);
        GuiOpener.openEditor(player, fresh);
        feedback(ctx, "\u00A77Abriendo editor de un kit nuevo...");
        return 1;
    }

    private static int executeEditList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        GuiOpener.openList(player, GuiOpener.ListMode.EDIT);
        return 1;
    }

    private static int executeEdit(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        String kitId = StringArgumentType.getString(ctx, "kit");
        Optional<Kit> kit = FantasticKits.kits().byId(kitId);
        if (kit.isEmpty()) {
            error(ctx, "El kit '" + kitId + "' no existe.");
            return 0;
        }
        FantasticKits.audit().log(AuditEventType.EDIT_KIT, player, kit.get(),
                "OPEN", "Editor opened from /fkits edit.");
        GuiOpener.openEditor(player, kit.get());
        return 1;
    }

    private static int executeDeleteList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        GuiOpener.openList(player, GuiOpener.ListMode.DELETE);
        return 1;
    }

    private static int executeDelete(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        String kitId = StringArgumentType.getString(ctx, "kit");
        Optional<Kit> kit = FantasticKits.kits().byId(kitId);
        if (kit.isEmpty()) {
            error(ctx, "El kit '" + kitId + "' no existe.");
            return 0;
        }
        // The actual confirmation happens inside the GUI - direct deletion is
        // disallowed to avoid accidental destruction.
        GuiOpener.openList(player, GuiOpener.ListMode.DELETE);
        return 1;
    }

    private static int executeGet(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        String kitId = StringArgumentType.getString(ctx, "kit");
        Optional<Kit> kitOpt = FantasticKits.kits().byId(kitId);
        if (kitOpt.isEmpty()) {
            error(ctx, "El kit '" + kitId + "' no existe.");
            return 0;
        }
        Kit kit = kitOpt.get();
        KitClaimService.Outcome outcome = KitClaimService.claim(player, kit);
        Component msg = KitClaimService.outcomeMessage(outcome, kit);
        ctx.getSource().sendSuccess(() -> msg, false);
        return outcome == KitClaimService.Outcome.SUCCESS ? 1 : 0;
    }

    private static int executeTestList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        GuiOpener.openList(player, GuiOpener.ListMode.TEST);
        return 1;
    }

    private static int executeTest(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;
        String kitId = StringArgumentType.getString(ctx, "kit");
        Optional<Kit> kit = FantasticKits.kits().byId(kitId);
        if (kit.isEmpty()) {
            error(ctx, "El kit '" + kitId + "' no existe.");
            return 0;
        }
        KitClaimService.Outcome outcome = KitClaimService.testClaim(player, kit.get());
        Component msg = KitClaimService.outcomeMessage(outcome, kit.get());
        ctx.getSource().sendSuccess(() -> msg, false);
        return outcome == KitClaimService.Outcome.SUCCESS ? 1 : 0;
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            error(ctx, "Solo los jugadores pueden ejecutar este comando.");
            return null;
        }
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(FantasticKits.config().chatPrefix + msg), false);
    }

    private static void error(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(FantasticKits.config().chatPrefix + "\u00A7c" + msg));
    }
}
