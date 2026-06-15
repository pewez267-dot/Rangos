package com.pewez.fantasticshortcuts.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.audit.AuditLog;
import com.pewez.fantasticshortcuts.config.ModConfig;
import com.pewez.fantasticshortcuts.integration.luckperms.LuckPermsIntegration;
import com.pewez.fantasticshortcuts.security.SecurityRules;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Registers shortcuts as real Brigadier commands.
 *
 * Core behaviour (the "Golden Rule"): a shortcut runs its target command AS THE PLAYER who typed it,
 * never as console and never with elevated permissions. The alias node also inherits the permission
 * requirement of the original command, so a player who cannot use the original command neither sees
 * nor can run the shortcut. The vanilla / mod / LuckPerms permission system therefore stays fully in
 * control.
 */
public final class ShortcutCommandRegistrar {

    private ShortcutCommandRegistrar() {
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        int count = 0;
        int conflicts = 0;
        for (Shortcut shortcut : ShortcutManager.get().all()) {
            switch (registerOne(dispatcher, shortcut)) {
                case REGISTERED -> count++;
                case CONFLICT -> conflicts++;
                default -> {
                }
            }
        }
        FantasticShortcutsMod.LOGGER.info("Registered {} shortcuts ({} skipped due to conflicts)", count, conflicts);
    }

    /**
     * Register any shortcuts that are not yet present in the live dispatcher. Used after a runtime
     * create/reload so newly added shortcuts work immediately (Brigadier cannot remove existing
     * nodes, so renames/removals require a full /reload or restart).
     */
    public static int registerMissing(CommandDispatcher<CommandSourceStack> dispatcher) {
        int added = 0;
        for (Shortcut shortcut : ShortcutManager.get().all()) {
            if (shortcut.alias == null || shortcut.alias.isBlank()) {
                continue;
            }
            if (dispatcher.getRoot().getChild(shortcut.alias.toLowerCase()) != null) {
                continue;
            }
            if (registerOne(dispatcher, shortcut) == Outcome.REGISTERED) {
                added++;
            }
        }
        return added;
    }

    private enum Outcome {REGISTERED, CONFLICT, SKIPPED}

    private static Outcome registerOne(CommandDispatcher<CommandSourceStack> dispatcher, Shortcut shortcut) {
        if (shortcut == null || shortcut.alias == null || shortcut.alias.isBlank()
                || shortcut.command == null || shortcut.command.isBlank()) {
            return Outcome.SKIPPED;
        }
        String alias = shortcut.alias.toLowerCase().trim();

        if (SecurityRules.isProtected(alias) || SecurityRules.isProtected(SecurityRules.firstToken(shortcut.command))) {
            AuditLog.record(AuditEvent.INVALID_ACCESS, "system", "Refused to register protected mapping: " + alias);
            return Outcome.SKIPPED;
        }

        // Conflict detection: an alias that already exists as another command.
        CommandNode<CommandSourceStack> existing = dispatcher.getRoot().getChild(alias);
        if (existing != null) {
            if (ModConfig.WARN_ON_CONFLICT.get()) {
                FantasticShortcutsMod.LOGGER.warn("Shortcut alias '/{}' conflicts with an existing command.", alias);
            }
            AuditLog.record(AuditEvent.CONFLICT, "system", "Alias '" + alias + "' conflicts with an existing command");
            if (!ModConfig.keepShortcutOnConflict()) {
                return Outcome.CONFLICT;
            }
            // priority=SHORTCUT: fall through and register (Brigadier will merge / our executor wins).
        }

        // Inherit the permission requirement of the target command so visibility mirrors the original.
        Predicate<CommandSourceStack> requirement = inheritedRequirement(dispatcher, shortcut.command);

        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(alias)
                .requires(requirement)
                .executes(ctx -> execute(ctx, shortcut, null));

        if (shortcut.allowArguments || shortcut.usesArgsPlaceholder()) {
            builder.then(Commands.argument("args", StringArgumentType.greedyString())
                    .requires(requirement)
                    .executes(ctx -> execute(ctx, shortcut, StringArgumentType.getString(ctx, "args"))));
        }

        dispatcher.register(builder);
        return Outcome.REGISTERED;
    }

    private static Predicate<CommandSourceStack> inheritedRequirement(CommandDispatcher<CommandSourceStack> dispatcher,
                                                                      String command) {
        String firstToken = SecurityRules.firstToken(command);
        CommandNode<CommandSourceStack> targetNode = dispatcher.getRoot().getChild(firstToken);
        if (targetNode != null && targetNode.getRequirement() != null) {
            return targetNode.getRequirement();
        }
        return source -> true;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, Shortcut shortcut, String args) {
        CommandSourceStack source = ctx.getSource();
        String actor = source.getTextName();
        String finalCommand = shortcut.buildCommand(args);

        // Defence in depth: reject anything that looks like command injection.
        if (SecurityRules.looksLikeInjection(finalCommand)) {
            AuditLog.record(AuditEvent.INJECTION_ATTEMPT, actor, "alias=" + shortcut.alias + " built=" + finalCommand);
            source.sendFailure(ChatPrefix.error("This shortcut produced an invalid command."));
            return 0;
        }

        String context = "alias=/" + shortcut.alias + " -> /" + finalCommand;
        if (source.getEntity() instanceof ServerPlayer player) {
            Optional<String> group = LuckPermsIntegration.getPrimaryGroup(player.getUUID());
            if (group.isPresent()) {
                context += " group=" + group.get();
            }
        }
        AuditLog.record(AuditEvent.EXECUTE_SHORTCUT, actor, context);

        // Run the real command AS THE PLAYER (no elevation, no console). Permissions are enforced by
        // the underlying command's own requirements (vanilla / mods / LuckPerms).
        return source.getServer().getCommands().performPrefixedCommand(source, finalCommand);
    }
}
