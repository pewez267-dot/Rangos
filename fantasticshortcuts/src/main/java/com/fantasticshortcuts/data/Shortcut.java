package com.fantasticshortcuts.data;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Immutable-ish data model for a single shortcut (alias). Serialised to
 * {@code shortcuts.json} via Gson using the snake_case keys defined in the spec.
 */
public final class Shortcut {

    /** Placeholder, in the original command, replaced by everything the player types after the alias. */
    public static final String ARGS_TOKEN = "{args}";

    private String id;
    private String name;
    private String description;
    @SerializedName("original_command")
    private String originalCommand;
    private String alias;
    @SerializedName("replace_original")
    private boolean replaceOriginal;
    @SerializedName("created_by")
    private String createdBy;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("last_modified")
    private String lastModified;

    public Shortcut() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.description = "";
        this.originalCommand = "";
        this.alias = "";
        this.replaceOriginal = false;
        this.createdBy = "";
        this.createdAt = nowIso();
        this.lastModified = this.createdAt;
    }

    public static String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    /** Normalises a command/alias string to a bare form without the leading slash, trimmed. */
    public static String stripSlash(final String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    // ---- getters / setters ----
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getOriginalCommand() {
        return originalCommand == null ? "" : originalCommand;
    }

    public void setOriginalCommand(final String originalCommand) {
        this.originalCommand = originalCommand;
    }

    public String getAlias() {
        return alias == null ? "" : alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public boolean isReplaceOriginal() {
        return replaceOriginal;
    }

    public void setReplaceOriginal(final boolean replaceOriginal) {
        this.replaceOriginal = replaceOriginal;
    }

    public String getCreatedBy() {
        return createdBy == null ? "" : createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void touchModified() {
        this.lastModified = nowIso();
    }

    // ---- derived helpers ----

    /** The alias as a lowercase bare literal (no slash), e.g. "gc". This is the cache key. */
    public String aliasKey() {
        return stripSlash(getAlias()).toLowerCase(Locale.ROOT);
    }

    /** The first literal of the original command, e.g. "gamemode" for "/gamemode creative". */
    public String originalRootLiteral() {
        final String s = stripSlash(getOriginalCommand());
        final int space = s.indexOf(' ');
        return (space >= 0 ? s.substring(0, space) : s).toLowerCase(Locale.ROOT);
    }

    public boolean usesArgs() {
        return getOriginalCommand().contains(ARGS_TOKEN);
    }

    /**
     * Resolves the concrete command to run (without leading slash), substituting the
     * player-typed arguments into {@code {args}}. When the original has no {@code {args}}
     * placeholder, the player arguments are not appended here (the caller decides whether
     * extra args are ignored or denied based on config).
     */
    public String resolve(final String playerArgs) {
        String base = stripSlash(getOriginalCommand());
        if (base.contains(ARGS_TOKEN)) {
            final String args = playerArgs == null ? "" : playerArgs.trim();
            base = base.replace(ARGS_TOKEN, args);
        }
        return base.replaceAll("\\s+", " ").trim();
    }

    /** True when the original command is exactly "{@code <root> {args}}" (redirect-eligible). */
    public boolean isSingleRootWithArgs() {
        return stripSlash(getOriginalCommand()).trim().matches("[a-zA-Z0-9_\\-]+\\s+\\{args\\}");
    }

    public Shortcut copy() {
        final Shortcut c = new Shortcut();
        c.id = this.id;
        c.name = this.name;
        c.description = this.description;
        c.originalCommand = this.originalCommand;
        c.alias = this.alias;
        c.replaceOriginal = this.replaceOriginal;
        c.createdBy = this.createdBy;
        c.createdAt = this.createdAt;
        c.lastModified = this.lastModified;
        return c;
    }
}
