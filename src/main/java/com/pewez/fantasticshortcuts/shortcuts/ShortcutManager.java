package com.pewez.fantasticshortcuts.shortcuts;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.audit.AuditLog;
import com.pewez.fantasticshortcuts.brigadier.CommandTreeService;
import com.pewez.fantasticshortcuts.brigadier.ReplacedCommands;
import com.pewez.fantasticshortcuts.config.FSConfig;
import com.pewez.fantasticshortcuts.integration.luckperms.LuckPermsIntegration;
import com.pewez.fantasticshortcuts.security.SecurityGuard;
import com.pewez.fantasticshortcuts.storage.ShortcutStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Núcleo de gestión de atajos (lado servidor).
 *
 * <p>Mantiene el estado en memoria, persiste en {@code shortcuts.json}, valida con
 * {@link SecurityGuard}, audita cada operación y dispara la sincronización en vivo del árbol de
 * comandos. Es un singleton: hay un único conjunto de atajos GLOBALES por servidor.
 */
public final class ShortcutManager {

    private static final ShortcutManager INSTANCE = new ShortcutManager();

    public static ShortcutManager get() {
        return INSTANCE;
    }

    private final Map<String, Shortcut> byAlias = new LinkedHashMap<>();
    private ShortcutStorage storage;
    private AuditLog audit;
    private MinecraftServer server;

    private ShortcutManager() {}

    /** Resultado de una operación CRUD, con mensaje listo para mostrar al usuario. */
    public record Result(boolean success, String message) {
        public static Result ok(String msg) { return new Result(true, msg); }
        public static Result fail(String msg) { return new Result(false, msg); }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Inicializa el almacenamiento y la auditoría y carga los atajos desde disco. Se llama pronto
     * (en {@code FMLCommonSetupEvent}), antes de que se registre cualquier comando.
     */
    public synchronized void init(Path baseDir) {
        this.storage = new ShortcutStorage(baseDir);
        this.audit = new AuditLog(baseDir);
        reload();
    }

    /** Asocia el servidor activo (en {@code ServerStartingEvent}) para la sincronización en vivo. */
    public synchronized void attachServer(MinecraftServer server) {
        this.server = server;
    }

    public synchronized void shutdown() {
        ReplacedCommands.clear();
        this.server = null;
    }

    /** Recarga la lista desde disco (o deja la lista vacía si el fichero no existe). */
    public synchronized void reload() {
        byAlias.clear();
        if (storage != null) {
            for (Shortcut s : storage.load()) {
                byAlias.put(s.alias(), s);
            }
        }
        recomputeReplaced();
        FantasticShortcuts.LOGGER.info("[F-Shortcuts] Cargados {} atajo(s) desde {}.",
                byAlias.size(), storage != null ? storage.file() : "(sin almacenamiento)");
    }

    public AuditLog audit() {
        return audit != null ? audit : (audit = new AuditLog(FantasticShortcuts.baseDir()));
    }

    public MinecraftServer server() {
        return server;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public synchronized List<Shortcut> list() {
        final List<Shortcut> out = new ArrayList<>(byAlias.size());
        for (Shortcut s : byAlias.values()) {
            out.add(s.copy());
        }
        return out;
    }

    public synchronized Shortcut get(String alias) {
        return byAlias.get(Shortcut.normalizeAlias(alias));
    }

    public synchronized boolean exists(String alias) {
        return byAlias.containsKey(Shortcut.normalizeAlias(alias));
    }

    public synchronized int size() {
        return byAlias.size();
    }

    // ------------------------------------------------------------------
    // CRUD (siempre invocado tras validar permiso 4 en la capa de red)
    // ------------------------------------------------------------------

    public Result create(CommandSourceStack actor, String rawAlias, String rawCommand) {
        final SecurityGuard.Result check = SecurityGuard.validate(rawAlias, rawCommand);
        if (!check.ok()) {
            auditInvalid(actor, check, rawAlias, rawCommand);
            return Result.fail(check.message());
        }
        final String alias = Shortcut.normalizeAlias(rawAlias);
        synchronized (this) {
            if (byAlias.containsKey(alias)) {
                return Result.fail("Ya existe un atajo con el alias '/" + alias + "'.");
            }
            final Shortcut shortcut = new Shortcut(alias, rawCommand);
            byAlias.put(alias, shortcut);
            persist();
        }
        auditWithActor(AuditEvent.CREATE_SHORTCUT, actor, "/" + alias + " -> /" + Shortcut.normalizeCommand(rawCommand));
        liveSync();
        return Result.ok("Atajo '/" + alias + "' creado.");
    }

    public Result update(CommandSourceStack actor, String originalAlias, Shortcut edited) {
        final String oldAlias = Shortcut.normalizeAlias(originalAlias);
        final SecurityGuard.Result check = SecurityGuard.validate(edited);
        if (!check.ok()) {
            auditInvalid(actor, check, edited.alias(), edited.command());
            return Result.fail(check.message());
        }
        synchronized (this) {
            if (!byAlias.containsKey(oldAlias)) {
                return Result.fail("No existe el atajo '/" + oldAlias + "'.");
            }
            final String newAlias = edited.alias();
            if (!newAlias.equals(oldAlias) && byAlias.containsKey(newAlias)) {
                return Result.fail("Ya existe un atajo con el alias '/" + newAlias + "'.");
            }
            byAlias.remove(oldAlias);
            byAlias.put(newAlias, edited.copy());
            persist();
        }
        auditWithActor(AuditEvent.EDIT_SHORTCUT, actor,
                "/" + oldAlias + " => /" + edited.alias() + " -> /" + edited.command()
                        + " [args=" + edited.useArgs() + ", replace=" + edited.replaceOriginal() + "]");
        liveSync();
        return Result.ok("Atajo '/" + edited.alias() + "' guardado.");
    }

    public Result delete(CommandSourceStack actor, String rawAlias) {
        final String alias = Shortcut.normalizeAlias(rawAlias);
        final boolean removed;
        synchronized (this) {
            removed = byAlias.remove(alias) != null;
            if (removed) {
                persist();
            }
        }
        if (!removed) {
            return Result.fail("No existe el atajo '/" + alias + "'.");
        }
        auditWithActor(AuditEvent.DELETE_SHORTCUT, actor, "/" + alias);
        liveSync();
        return Result.ok("Atajo '/" + alias + "' eliminado.");
    }

    // ------------------------------------------------------------------
    // Replace-mode / Brigadier tab hiding
    // ------------------------------------------------------------------

    /** Recalcula el conjunto de literales raíz que deben ocultarse del TAB del cliente. */
    public synchronized void recomputeReplaced() {
        final Set<String> roots = new HashSet<>();
        if (replaceModeEnabledSafe()) {
            for (Shortcut s : byAlias.values()) {
                if (s.replaceOriginal()) {
                    final String root = s.rootCommand();
                    if (!root.isBlank() && !SecurityGuard.isProtectedName(root)) {
                        roots.add(root);
                    }
                }
            }
        }
        ReplacedCommands.set(roots);
    }

    /** Lee {@code enableReplaceMode} de forma segura aunque la config aún no esté cargada. */
    private static boolean replaceModeEnabledSafe() {
        try {
            return FSConfig.enableReplaceMode();
        } catch (IllegalStateException notLoadedYet) {
            return true; // valor por defecto hasta que la config esté disponible
        }
    }

    // ------------------------------------------------------------------
    // Audit helpers
    // ------------------------------------------------------------------

    public void auditWithActor(AuditEvent event, CommandSourceStack source, String details) {
        audit().record(event, describeActor(source), resolveGroup(source), details);
    }

    private void auditInvalid(CommandSourceStack actor, SecurityGuard.Result check, String alias, String command) {
        final AuditEvent event = switch (check) {
            case INJECTION_ALIAS, INJECTION_COMMAND -> AuditEvent.INJECTION_ATTEMPT;
            default -> AuditEvent.INVALID_SHORTCUT;
        };
        auditWithActor(event, actor, check.name() + " alias='/" + alias + "' command='/" + command + "'");
    }

    public static String describeActor(CommandSourceStack source) {
        if (source == null) {
            return "UNKNOWN";
        }
        final ServerPlayer player = source.getPlayer();
        if (player != null) {
            return player.getName().getString() + "(" + player.getUUID() + ")";
        }
        try {
            return source.getTextName();
        } catch (Exception e) {
            return "SERVER";
        }
    }

    private static String resolveGroup(CommandSourceStack source) {
        if (source == null) {
            return null;
        }
        final ServerPlayer player = source.getPlayer();
        return player != null ? LuckPermsIntegration.primaryGroup(player) : null;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void persist() {
        if (storage != null) {
            storage.save(new ArrayList<>(byAlias.values()));
        }
    }

    private void liveSync() {
        recomputeReplaced();
        CommandTreeService.rebuildAndSync(server);
    }
}
