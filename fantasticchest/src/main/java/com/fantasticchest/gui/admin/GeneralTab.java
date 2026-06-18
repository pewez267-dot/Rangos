package com.fantasticchest.gui.admin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * "General" tab (creation only): the chest's unique id and coloured display name.
 */
public final class GeneralTab {

    public void build(final ChestAdminScreen s) {
        final int x = s.bx();
        final int y = s.by() + 14;

        final EditBox id = new EditBox(s.font(), x + 110, y, 240, 16, Component.empty());
        id.setMaxLength(48);
        id.setValue(s.draftId);
        id.setHint(Component.literal("id_unico"));
        id.setResponder(v -> s.draftId = v);
        s.addW(id);

        final EditBox name = new EditBox(s.font(), x + 110, y + 26, 240, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(s.draftName);
        name.setHint(Component.literal("Nombre (acepta & de color)"));
        name.setResponder(v -> s.draftName = v);
        s.addW(name);
    }

    public void renderLabels(final ChestAdminScreen s, final GuiGraphics g) {
        final int x = s.bx();
        final int y = s.by() + 14;
        g.drawString(s.font(), "§7ID unico:", x, y + 4, 10133680, false);
        g.drawString(s.font(), "§7Nombre:", x, y + 30, 10133680, false);

        final String normalized = s.draftId == null ? "" : s.draftId.trim().toLowerCase(Locale.ROOT);
        final String hint;
        if (normalized.isBlank()) {
            hint = "§8Escribe un ID (minusculas, numeros, _ y -).";
        } else if (!normalized.matches("[a-z0-9_\\-]+")) {
            hint = "§cID invalido: solo minusculas, numeros, '_' y '-'.";
        } else if (s.existingIds.stream().anyMatch(e -> e != null && e.toLowerCase(Locale.ROOT).equals(normalized))) {
            hint = "§cEse ID ya existe.";
        } else {
            hint = "§aID disponible.";
        }
        g.drawString(s.font(), hint, x, y + 56, 16777215, false);
    }
}
