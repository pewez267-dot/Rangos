package com.pewez.fantasticshortcuts.shortcuts;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * Modelo de datos de un atajo de comando.
 *
 * <p>Un atajo transforma un alias corto en un comando real. Ejemplo:
 * <pre>alias = "gc", command = "gamemode creative"</pre>
 * de modo que escribir {@code /gc} ejecuta {@code /gamemode creative} usando el MISMO
 * {@code CommandSourceStack} del jugador (sin elevar permisos).
 *
 * <p>Soporta la variable dinámica {@code {args}}: si el comando contiene {@code {args}} se
 * sustituye por lo que el jugador escriba tras el alias; si no contiene {@code {args}} pero
 * {@link #useArgs} está activo, los argumentos se anexan al final.
 */
public final class Shortcut {

    /** Placeholder de argumentos dinámicos dentro del comando. */
    public static final String ARGS_TOKEN = "{args}";

    private String alias;
    private String command;
    private String description;
    private boolean useArgs;
    private boolean replaceOriginal;

    public Shortcut() {
        this("", "", "", false, false);
    }

    public Shortcut(String alias, String command) {
        this(alias, command, "", commandUsesArgs(command), false);
    }

    public Shortcut(String alias, String command, String description, boolean useArgs, boolean replaceOriginal) {
        this.alias = normalizeAlias(alias);
        this.command = normalizeCommand(command);
        this.description = description == null ? "" : description;
        this.useArgs = useArgs;
        this.replaceOriginal = replaceOriginal;
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String alias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = normalizeAlias(alias);
    }

    public String command() {
        return command;
    }

    public void setCommand(String command) {
        this.command = normalizeCommand(command);
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public boolean useArgs() {
        return useArgs;
    }

    public void setUseArgs(boolean useArgs) {
        this.useArgs = useArgs;
    }

    public boolean replaceOriginal() {
        return replaceOriginal;
    }

    public void setReplaceOriginal(boolean replaceOriginal) {
        this.replaceOriginal = replaceOriginal;
    }

    // ---------------------------------------------------------------------
    // Derived helpers
    // ---------------------------------------------------------------------

    /** Primer token (literal raíz) del comando original, p. ej. {@code "gamemode"}. */
    public String rootCommand() {
        final String c = command.trim();
        if (c.isEmpty()) {
            return "";
        }
        final int sp = c.indexOf(' ');
        return (sp < 0 ? c : c.substring(0, sp)).toLowerCase(Locale.ROOT);
    }

    /** {@code true} si el comando contiene el placeholder {@code {args}}. */
    public boolean containsArgsToken() {
        return command.contains(ARGS_TOKEN);
    }

    /**
     * Construye el comando real (SIN barra inicial) a partir de los argumentos escritos por el
     * jugador. No eleva permisos ni inyecta nada: solo sustituye/anexa texto.
     *
     * @param args texto escrito tras el alias (puede ser vacío o {@code null}).
     */
    public String buildCommand(String args) {
        final String safeArgs = args == null ? "" : args.trim();
        String result;
        if (containsArgsToken()) {
            result = command.replace(ARGS_TOKEN, safeArgs);
        } else if (useArgs && !safeArgs.isEmpty()) {
            result = command + " " + safeArgs;
        } else {
            result = command;
        }
        return result.trim();
    }

    /** Texto de la fila para la lista de la GUI: {@code §b/gc §8-> §f/gamemode creative}. */
    public String listLabel() {
        return "§b/" + alias + " §8-> §f/" + command;
    }

    // ---------------------------------------------------------------------
    // Network serialization
    // ---------------------------------------------------------------------

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(alias);
        buf.writeUtf(command);
        buf.writeUtf(description);
        buf.writeBoolean(useArgs);
        buf.writeBoolean(replaceOriginal);
    }

    public static Shortcut decode(FriendlyByteBuf buf) {
        final String alias = buf.readUtf();
        final String command = buf.readUtf();
        final String description = buf.readUtf();
        final boolean useArgs = buf.readBoolean();
        final boolean replaceOriginal = buf.readBoolean();
        return new Shortcut(alias, command, description, useArgs, replaceOriginal);
    }

    public Shortcut copy() {
        return new Shortcut(alias, command, description, useArgs, replaceOriginal);
    }

    // ---------------------------------------------------------------------
    // Normalization
    // ---------------------------------------------------------------------

    public static String normalizeAlias(String alias) {
        if (alias == null) {
            return "";
        }
        String a = alias.trim().toLowerCase(Locale.ROOT);
        while (a.startsWith("/")) {
            a = a.substring(1);
        }
        return a;
    }

    public static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        String c = command.trim();
        while (c.startsWith("/")) {
            c = c.substring(1);
        }
        return c;
    }

    public static boolean commandUsesArgs(String command) {
        return command != null && command.contains(ARGS_TOKEN);
    }

    @Override
    public String toString() {
        return "Shortcut{/" + alias + " -> /" + command + ", args=" + useArgs + ", replace=" + replaceOriginal + "}";
    }
}
