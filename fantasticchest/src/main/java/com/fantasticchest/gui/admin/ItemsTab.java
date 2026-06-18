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
        s.addW(Button.builder(Component.literal(s.doBulk ? "§a\u2714 Masiva ON" : "§eAnadir todos"), b -> {
            s.doBulk = !s.doBulk;
            s.refresh();
        }).bounds(x + colW - 86, y, 86, 16).build());

        // Left: search + item picker.
        final EditBox search = new EditBox(s.font(), x, y + 22, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        search.setValue(s.draftItemSearch);
        s.addW(search);
        final ScrollSelector<Item> picker = new ScrollSelector<>(x, y + 42, colW, s.bh() - 54, 18,
                item -> new ItemStack(item).getHoverName().getString(),
                item -> new ItemStack(item).getHoverName().getString() + " " + id(item),
                ItemsTab::stackOf);
        picker.setItems(allItems());
        picker.setQuery(s.draftItemSearch);
        picker.onSelect(item -> {
            s.selectedItem = item;
            s.refresh();
        });
        search.setResponder(v -> {
            s.draftItemSearch = v;
            picker.setQuery(v);
        });
        s.addW(picker);

        // Right: individual quantity + add.
        final EditBox qty = new EditBox(s.font(), rightX, y, colW - 90, 16, Component.empty());
        qty.setHint(Component.literal("cant."));
        if (s.draftItemQty > 0L) {
            qty.setValue(Long.toString(s.draftItemQty));
        }
        qty.setResponder(v -> {
            try {
                s.draftItemQty = Long.parseLong(v.trim());
            } catch (final NumberFormatException e) {
                s.draftItemQty = 0L;
            }
        });
        s.addW(qty);
        s.addW(Button.builder(Component.literal("§aAnadir item"), b -> {
            if (s.selectedItem != null && s.draftItemQty > 0L) {
                s.overrides.put(s.selectedItem, s.draftItemQty);
                s.refresh();
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
        if (s.doBulk) {
            // Prominent, highlighted banner so it is unmistakable that the bulk fill is active.
            g.fill(x, yTop - 2, x + colW, yTop + 10, 0xC02E7D32);
            final String value = s.bulkValue > 0 ? Long.toString(s.bulkValue) : "default";
            g.drawString(s.font(), "\u2714 Carga masiva ACTIVA (" + value + ") - clic en 'Masiva ON' para cancelar",
                    x + 3, yTop, 0xFFFFFF, false);
        } else {
            g.drawString(s.font(), "§7Carga masiva (sobreescribe todo)", x, yTop, 10133680, false);
        }
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

    private static final java.util.Set<String> OPERATOR_ITEMS = java.util.Set.of(
            "minecraft:command_block", "minecraft:chain_command_block",
            "minecraft:repeating_command_block", "minecraft:command_block_minecart",
            "minecraft:barrier", "minecraft:debug_stick", "minecraft:light",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw",
            "minecraft:spawner", "minecraft:moving_piston", "minecraft:piston_head",
            "minecraft:bundle", "minecraft:knowledge_book", "minecraft:filled_map"
    );

    private static List<Item> allItems() {
        final List<Item> list = new ArrayList<>();
        for (final Item item : ForgeRegistries.ITEMS.getValues()) {
            final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;
            // Exclude operator-only vanilla items; include ALL mod items.
            if ("minecraft".equals(rl.getNamespace()) && OPERATOR_ITEMS.contains(rl.toString())) continue;
            // Exclude items with no real name (air, internal)
            if (rl.getPath().equals("air")) continue;
            list.add(item);
        }
        list.sort(Comparator.comparing(ItemsTab::id));
        return list;
    }
}
