package com.pewez.fantasticshortcuts.shortcuts;

import net.minecraft.network.FriendlyByteBuf;

/**
 * A single shortcut mapping: a short alias that expands into a real game command.
 *
 * The command may contain the {@code {args}} placeholder, which is replaced by any text the player
 * types after the alias. If no placeholder is present and {@code allowArguments} is true, the typed
 * text is appended to the end of the command instead.
 */
public class Shortcut {

    /** The alias, without a leading slash. e.g. "gc". */
    public String alias = "";

    /** The full command to run, without a leading slash. e.g. "gamemode creative" or "tp {args}". */
    public String command = "";

    /** If true, hide the original command from the client command tree (requires global replace mode). */
    public boolean replaceOriginal = false;

    /** If true, append any extra typed text to the command (ignored if {args} placeholder is used). */
    public boolean allowArguments = true;

    /** Optional description, purely informational. */
    public String description = "";

    /** UUID of the creator (for audit), may be null. */
    public String createdBy = "";

    public Shortcut() {
    }

    public Shortcut(String alias, String command) {
        this.alias = alias;
        this.command = command;
    }

    public boolean usesArgsPlaceholder() {
        return command != null && command.contains("{args}");
    }

    public Shortcut copy() {
        Shortcut c = new Shortcut(this.alias, this.command);
        c.replaceOriginal = this.replaceOriginal;
        c.allowArguments = this.allowArguments;
        c.description = this.description;
        c.createdBy = this.createdBy;
        return c;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(safe(alias));
        buf.writeUtf(safe(command));
        buf.writeBoolean(replaceOriginal);
        buf.writeBoolean(allowArguments);
        buf.writeUtf(safe(description));
        buf.writeUtf(safe(createdBy));
    }

    public static Shortcut decode(FriendlyByteBuf buf) {
        Shortcut shortcut = new Shortcut();
        shortcut.alias = buf.readUtf(64);
        shortcut.command = buf.readUtf(512);
        shortcut.replaceOriginal = buf.readBoolean();
        shortcut.allowArguments = buf.readBoolean();
        shortcut.description = buf.readUtf(512);
        shortcut.createdBy = buf.readUtf(64);
        return shortcut;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Build the final command to execute given the player-supplied arguments (may be empty/null).
     */
    public String buildCommand(String args) {
        String base = command == null ? "" : command.trim();
        String extra = args == null ? "" : args.trim();
        if (usesArgsPlaceholder()) {
            return base.replace("{args}", extra).trim();
        }
        if (allowArguments && !extra.isEmpty()) {
            return (base + " " + extra).trim();
        }
        return base;
    }
}
