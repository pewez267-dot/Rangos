package com.fantasticchest.gui.admin;

import com.fantasticchest.gui.widget.ScrollSelector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * "Seguridad" tab: the list of players (name or UUID) allowed to open the terminal. The
 * owner always has access implicitly. In edit mode, changes are applied on the server
 * immediately.
 */
public final class SecurityTab {

    public void build(final ChestAdminScreen s) {
        final int x = s.bx();
        final int y = s.by() + 14;
        final int w = s.bw();

        final EditBox add = new EditBox(s.font(), x, y, w - 90, 16, Component.empty());
        add.setHint(Component.literal("Nombre o UUID del jugador"));
        s.addW(add);
        s.addW(Button.builder(Component.literal("§aAnadir"), b -> {
            final String value = add.getValue().trim();
            if (!value.isBlank() && !s.permitted.contains(value)) {
                s.permitted.add(value);
                s.sendPermissionsNow();
                s.refresh();
            }
        }).bounds(x + w - 86, y, 86, 16).build());

        final ScrollSelector<String> list = new ScrollSelector<>(x, y + 22, w, s.bh() - 34, 16,
                p -> "§f" + p,
                p -> p,
                null);
        list.setItems(new ArrayList<>(s.permitted));
        list.onSelect(p -> {
            s.permitted.remove(p);
            s.sendPermissionsNow();
            s.refresh();
        });
        s.addW(list);
    }

    public void renderLabels(final ChestAdminScreen s, final GuiGraphics g) {
        final int x = s.bx();
        final int yTop = s.by();
        g.drawString(s.font(), "§7Jugadores con acceso (clic en la lista para quitar):", x, yTop, 10133680, false);
        g.drawString(s.font(), "§8El dueno siempre tiene acceso. Hay " + s.permitted.size() + " permitido(s).",
                x, yTop + s.bh() + 2, 10133680, false);
    }
}
