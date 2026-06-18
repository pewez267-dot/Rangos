package com.fantasticchest.gui.admin;

import com.fantasticchest.gui.widget.ScrollSelector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Items" tab: bulk fill (overwrites everything) and per-item additions/overrides. Both are
 * combinable; the heavy registry iteration for bulk fill happens once, server-side.
 */
public final class ItemsTab {

    public void build(final ChestAdminScreen s) {
        final int x = s.bx();
        final int yTop = s.by();
        final int y = yTop + 12;
        final int colW = (s.bw() - 8) / 2;
        final int rightX = x + colW + 8;

        // Left: bulk fill.
        final EditBox bulk = new EditBox(s.font(), x, y, colW - 90, 16, Component.empty());
        bulk.setHint(Component.literal("cantidad"));
        if (s.bulkValue > 0L) {
            bulk.setValue(Long.toString(s.bulkValue));
        }
        bulk.setResponder(v -> {
            try {
                s.bulkValue = Long.parseLong(v.trim());
            } catch (final NumberFormatException e) {
                s.bulkValue = 0L;
            }
        });
        s.addW(bulk);
        s.addW(Button.builder(Component.literal("§eAnadir todos"), b -> {
            s.doBulk = true;
            s.refresh();
        }).bounds(x + colW - 86, y, 86, 16).build());

        // Left: search + item picker.
        final EditBox search = new EditBox(s.font(), x, y + 22, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        s.addW(search);
        final ScrollSelector<Item> picker = new ScrollSelector<>(x, y + 42, colW, s.bh() - 54, 18,
                item -> new ItemStack(item).getHoverName().getString(),
                item -> new ItemStack(item).getHoverName().getString() + " " + id(item),
                ItemsTab::stackOf);
        picker.setItems(allItems());
        picker.onSelect(item -> {
            s.selectedItem = item;
            s.refresh();
        });
        search.setResponder(picker::setQuery);
        s.addW(picker);

        // Right: individual quantity + add.
        final EditBox qty = new EditBox(s.font(), rightX, y, colW - 90, 16, Component.empty());
        qty.setHint(Component.literal("cant."));
        s.addW(qty);
        s.addW(Button.builder(Component.literal("§aAnadir item"), b -> {
            if (s.selectedItem != null) {
                long q;
                try {
                    q = Long.parseLong(qty.getValue().trim());
                } catch (final NumberFormatException e) {
                    q = 0L;
                }
                if (q > 0L) {
                    s.overrides.put(s.selectedItem, q);
                    s.refresh();
                }
            }
        }).bounds(rightX + colW - 86, y, 86, 16).build());

        // Right: list of individual overrides (click to remove).
        final ScrollSelector<Item> overrides = new ScrollSelector<>(rightX, y + 22, colW, s.bh() - 34, 18,
                item -> new ItemStack(item).getHoverName().getString() + " §7x" + s.overrides.getOrDefault(item, 0L),
                item -> new ItemStack(item).getHoverName().getString(),
                ItemsTab::stackOf);
        overrides.setItems(new ArrayList<>(s.overrides.keySet()));
        overrides.onSelect(item -> {
            s.overrides.remove(item);
            s.refresh();
        });
        s.addW(overrides);
    }

    public void renderLabels(final ChestAdminScreen s, final GuiGraphics g) {
        final int x = s.bx();
        final int yTop = s.by();
        final int colW = (s.bw() - 8) / 2;
        final int rightX = x + colW + 8;
        final String bulkInfo = s.doBulk ? ("§aMasiva activa: " + (s.bulkValue > 0 ? s.bulkValue : "default")) : "§7Carga masiva (sobreescribe todo)";
        g.drawString(s.font(), bulkInfo, x, yTop, 10133680, false);
        final String sel = s.selectedItem == null ? "ninguno" : new ItemStack(s.selectedItem).getHoverName().getString();
        g.drawString(s.font(), "§7Sel: §f" + sel + "  §7Individuales: §f" + s.overrides.size(), rightX, yTop, 10133680, false);
    }

    private static ItemStack stackOf(final Item item) {
        return new ItemStack(item);
    }

    private static String id(final Item item) {
        final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    private static List<Item> allItems() {
        final List<Item> list = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing(ItemsTab::id));
        return list;
    }
}
