package com.pewez.fantasticshortcuts.brigadier;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.config.FSConfig;
import com.pewez.fantasticshortcuts.security.SecurityGuard;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/**
 * Convierte un {@link Shortcut} en un nodo de comando Brigadier registrado en el dispatcher.
 *
 * <p>Flujo de ejecución de un atajo: el jugador escribe {@code /gc} -> el nodo del alias se ejecuta
 * -> se construye el comando real {@code gamemode creative} -> se reenvía con
 * {@code Commands.performPrefixedCommand} usando el MISMO source del jugador. El sistema de permisos
 * (vanilla / mods / LuckPerms) valida el comando real, exactamente igual que si el jugador lo
 * hubiese escrito completo. Nunca se eleva el contexto ni se ejecuta como consola.
 *
 * <p>Visibilidad: el alias hereda el {@code requires()} del nodo del comando original, de modo que
 * un jugador que no puede ver/usar el comando original tampoco puede usar el atajo.
 */
public final class ShortcutCommandBuilder {

    private ShortcutCommandBuilder() {}

    /**
     * Registra el atajo en el dispatcher dado. Detecta conflictos con comandos existentes de otros
     * mods (sin crashear) y respeta {@code shortcutPriority}.
     *
     * @return {@code true} si el nodo fue registrado.
     */
    public static boolean register(CommandDispatcher<CommandSourceStack> dispatcher, Shortcut shortcut) {
        if (dispatcher == null || shortcut == null) {
            return false;
        }
        final String alias = shortcut.alias();
        if (alias.isBlank()) {
            return false;
        }

        // --- Detección de conflictos (no crashea, solo avisa y respeta la prioridad). ---
        final CommandNode<CommandSourceStack> existing = dispatcher.getRoot().getChild(alias);
        if (existing != null) {
            ShortcutManager.get().audit().record(AuditEvent.CONFLICT, "SYSTEM", null,
                    "El alias '/" + alias + "' ya existe en el arbol de comandos (posible conflicto con otro mod).");
            FantasticShortcuts.LOGGER.warn("[F-Shortcuts] Conflicto de alias '/{}': ya existe un comando con ese nombre.", alias);
            if (FSConfig.shortcutPriority() == FSConfig.Priority.ORIGINAL_FIRST) {
                FantasticShortcuts.LOGGER.warn("[F-Shortcuts] shortcutPriority=ORIGINAL_FIRST: se mantiene el comando existente, atajo '/{}' NO registrado.", alias);
                return false;
            }
            FantasticShortcuts.LOGGER.warn("[F-Shortcuts] shortcutPriority=SHORTCUT_FIRST: el atajo '/{}' tomara precedencia.", alias);
        }

        // --- requires() heredado del comando original (si existe). ---
        final Predicate<CommandSourceStack> requires = inheritedRequires(dispatcher, shortcut);

        final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(alias).requires(requires);

        if (shortcut.containsArgsToken() || shortcut.useArgs()) {
            // Ejecutable sin argumentos (args vacíos) y con argumento greedy.
            node.executes(ctx -> execute(ctx, shortcut, ""));
            node.then(Commands.argument("args", StringArgumentType.greedyString())
                    .executes(ctx -> execute(ctx, shortcut, StringArgumentType.getString(ctx, "args"))));
        } else {
            node.executes(ctx -> execute(ctx, shortcut, ""));
        }

        dispatcher.register(node);
        return true;
    }

    /** Construye el predicado de visibilidad heredado del nodo del comando original. */
    private static Predicate<CommandSourceStack> inheritedRequires(CommandDispatcher<CommandSourceStack> dispatcher, Shortcut shortcut) {
        final String root = shortcut.rootCommand();
        final CommandNode<CommandSourceStack> originalRoot = root.isBlank() ? null : dispatcher.getRoot().getChild(root);
        if (originalRoot != null) {
            // Mismo gate de visibilidad/uso que el comando original.
            return originalRoot::canUse;
        }
        // No hay nodo original (comando de otro contexto): permitimos parsear y dejamos que la
        // ejecución del comando real valide los permisos. Nunca concedemos nada por nuestra cuenta.
        return source -> true;
    }

    /** Ejecuta el comando real asociado al atajo, sin elevar permisos. */
    private static int execute(CommandContext<CommandSourceStack> ctx, Shortcut shortcut, String args) {
        final CommandSourceStack source = ctx.getSource();

        // Defensa anti-inyección sobre lo que el jugador escribió tras el alias.
        if (SecurityGuard.hasInjection(args)) {
            ShortcutManager.get().auditWithActor(AuditEvent.INJECTION_ATTEMPT, source,
                    "/" + shortcut.alias() + " args='" + args + "'");
            source.sendFailure(Component.literal("§c[F-Shortcuts] Argumentos no permitidos."));
            return 0;
        }

        final String real = shortcut.buildCommand(args);
        ShortcutManager.get().auditWithActor(AuditEvent.EXECUTE_SHORTCUT, source,
                "/" + shortcut.alias() + (args.isBlank() ? "" : " " + args) + " -> /" + real);

        // Reenvío con el MISMO source del jugador (sin elevar, sin consola).
        return source.getServer().getCommands().performPrefixedCommand(source, real);
    }
}
