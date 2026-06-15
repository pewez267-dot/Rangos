package com.pewez.fantasticshortcuts.gui;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * A server-side, vanilla-rendered chest GUI for browsing and managing shortcuts.
 *
 * Uses the vanilla 9x6 generic container so it works on any client with no client-side mod. Follows
 * a clean, single-screen, paginated layout: shortcut entries on top, a navigation row at the bottom.
 * No overlapping screens, no duplicates - opening a new page replaces the current menu.
 */
public final class ShortcutGui {

    private static final int SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;

    private ShortcutGui() {
    }

    public static void open(ServerPlayer player, int page) {
        List<Shortcut> all = new ArrayList<>(ShortcutManager.get().all());
        int maxPage = Math.max(0, (all.size() - 1) / ENTRIES_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));

        List<String> pageAliases = new ArrayList<>();
        SimpleContainer container = new SimpleContainer(SIZE);

        int start = safePage * ENTRIES_PER_PAGE;
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            int index = start + i;
            if (index >= all.size()) {
                pageAliases.add(null);
                continue;
            }
            Shortcut shortcut = all.get(index);
            pageAliases.add(shortcut.alias);
            container.setItem(i, entryItem(shortcut));
        }

        // Navigation row
        ItemStack filler = named(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Component.literal(" "));
        for (int slot = ENTRIES_PER_PAGE; slot < SIZE; slot++) {
            container.setItem(slot, filler.copy());
        }
        if (safePage > 0) {
            container.setItem(SLOT_PREV, labelled(Items.ARROW, "Previous page", ChatFormatting.YELLOW));
        }
        if (safePage < maxPage) {
            container.setItem(SLOT_NEXT, labelled(Items.ARROW, "Next page", ChatFormatting.YELLOW));
        }
        container.setItem(SLOT_CLOSE, labelled(Items.BARRIER, "Close", ChatFormatting.RED));

        boolean hasNext = safePage < maxPage;
        final int currentPage = safePage;
        Component title = Component.literal("F-Shortcuts (" + (safePage + 1) + "/" + (maxPage + 1) + ")")
                .withStyle(ChatFormatting.DARK_AQUA);

        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new ShortcutMenu(id, inv, container, currentPage, pageAliases, hasNext),
                title);
        player.openMenu(provider);
    }

    private static ItemStack entryItem(Shortcut shortcut) {
        ItemStack stack = new ItemStack(Items.PAPER);
        named(stack, Component.literal("/" + shortcut.alias).withStyle(ChatFormatting.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("-> /" + shortcut.command).withStyle(ChatFormatting.WHITE));
        if (shortcut.description != null && !shortcut.description.isBlank()) {
            lore.add(Component.literal(shortcut.description).withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.literal("replaceOriginal: " + shortcut.replaceOriginal).withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());
        lore.add(Component.literal("Left click: info").withStyle(ChatFormatting.GREEN));
        lore.add(Component.literal("Shift / Right click: delete").withStyle(ChatFormatting.RED));
        return withLore(stack, lore);
    }

    private static ItemStack labelled(net.minecraft.world.item.Item item, String text, ChatFormatting color) {
        return named(new ItemStack(item), Component.literal(text).withStyle(color));
    }

    private static ItemStack named(ItemStack stack, Component name) {
        stack.setHoverName(name.copy().withStyle(name.getStyle().withItalic(false)));
        return stack;
    }

    private static ItemStack withLore(ItemStack stack, List<Component> lore) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            Component noItalic = line.copy().withStyle(s -> {
                Style style = line.getStyle();
                return style.withItalic(false);
            });
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(noItalic)));
        }
        display.put("Lore", loreTag);
        return stack;
    }

    /**
     * Custom menu: a read-only 9x6 chest whose clicks are intercepted as buttons.
     */
    public static class ShortcutMenu extends ChestMenu {
        private final List<String> pageAliases;
        private final int page;
        private final boolean hasNext;

        public ShortcutMenu(int id, Inventory inventory, SimpleContainer container, int page,
                            List<String> pageAliases, boolean hasNext) {
            super(MenuType.GENERIC_9x6, id, inventory, container, 6);
            this.pageAliases = pageAliases;
            this.page = page;
            this.hasNext = hasNext;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            // Only react to clicks in the top (GUI) container; ignore everything else and never move items.
            if (slotId < 0 || slotId >= ENTRIES_PER_PAGE + 9) {
                resync();
                return;
            }
            if (slotId == SLOT_PREV) {
                if (page > 0) {
                    open(serverPlayer, page - 1);
                }
                return;
            }
            if (slotId == SLOT_NEXT) {
                if (hasNext) {
                    open(serverPlayer, page + 1);
                }
                return;
            }
            if (slotId == SLOT_CLOSE) {
                serverPlayer.closeContainer();
                return;
            }
            if (slotId < ENTRIES_PER_PAGE) {
                String alias = slotId < pageAliases.size() ? pageAliases.get(slotId) : null;
                if (alias == null) {
                    resync();
                    return;
                }
                boolean delete = clickType == ClickType.QUICK_MOVE || button == 1;
                if (delete) {
                    ShortcutManager.Result result = ShortcutManager.get().delete(alias, serverPlayer.getGameProfile().getName());
                    serverPlayer.sendSystemMessage(result.success()
                            ? ChatPrefix.success(result.message())
                            : ChatPrefix.error(result.message()));
                    FantasticShortcutsMod.liveSync(serverPlayer.getServer());
                    open(serverPlayer, page);
                } else {
                    Shortcut shortcut = ShortcutManager.get().get(alias);
                    if (shortcut != null) {
                        serverPlayer.sendSystemMessage(ChatPrefix.info("/" + shortcut.alias + " -> /" + shortcut.command));
                    }
                    resync();
                }
                return;
            }
            resync();
        }

        private void resync() {
            sendAllDataToRemote();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
            // Disable shift-move logic entirely; this is a button GUI.
            return false;
        }
    }

    /** Referenced to avoid unused import warnings on AbstractContainerMenu in some toolchains. */
    @SuppressWarnings("unused")
    private static Class<?> menuBase() {
        return AbstractContainerMenu.class;
    }
}
