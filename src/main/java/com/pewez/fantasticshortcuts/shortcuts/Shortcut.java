package com.pewez.fantasticshortcuts.shortcuts;

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
