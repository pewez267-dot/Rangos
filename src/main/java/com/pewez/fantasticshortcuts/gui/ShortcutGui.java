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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium "Fantastic"-style management GUI for shortcuts.
 *
 * Server-driven, vanilla-rendered (9x6 generic chest), so it works on any client without a client
 * mod. Layout follows the FantasticCrates / FantasticSpawners / FantasticKits aesthetic:
 *
 *   Row 0: [B][B][B][B][HEADER][PAGE][B][B][B]    -- bordered header with title and page info
 *   Row 1: [B][.][.][.][.][.][.][.][B]
 *   Row 2: [B][.][.][.][.][.][.][.][B]    -- 28 shortcut entries per page (4 rows of 7)
 *   Row 3: [B][.][.][.][.][.][.][.][B]
 *   Row 4: [B][.][.][.][.][.][.][.][B]
 *   Row 5: [PREV][B][CREATE][RELOAD][CLOSE][INFO][B][B][NEXT]
 *
 * Single-screen navigation: changing page replaces the menu in place. No overlapping screens.
 */
public final class ShortcutGui {

    private static final int SIZE = 54;

    // Border slots (top + bottom + left/right column for rows 1-4).
    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,           // top row except header (4) and page-info (no separate slot)
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            46, 51, 52                         // bottom border slots
    };

    private static final int SLOT_HEADER = 4;

    // Entry area: rows 1-4, columns 1-7 (7 entries per row * 4 rows = 28).
    private static final int ENTRIES_PER_ROW = 7;
    private static final int ENTRY_ROWS = 4;
    private static final int ENTRIES_PER_PAGE = ENTRIES_PER_ROW * ENTRY_ROWS;

    // Bottom action row.
    private static final int SLOT_PREV = 45;
    private static final int SLOT_CREATE = 47;
    private static final int SLOT_RELOAD = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_INFO = 50;
    private static final int SLOT_NEXT = 53;

    private ShortcutGui() {
    }

    public static void open(ServerPlayer player, int page) {
        List<Shortcut> all = new ArrayList<>(ShortcutManager.get().all());
        int totalPages = Math.max(1, (all.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        SimpleContainer container = new SimpleContainer(SIZE);
        List<String> pageAliases = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            pageAliases.add(null);
        }

        // --- Borders ---
        ItemStack border = blankPane(Items.BLACK_STAINED_GLASS_PANE);
        for (int slot : BORDER_SLOTS) {
            container.setItem(slot, border.copy());
        }

        // --- Header (title + brand) ---
        container.setItem(SLOT_HEADER, headerItem(all.size(), safePage + 1, totalPages));

        // --- Entries ---
        int start = safePage * ENTRIES_PER_PAGE;
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            int row = i / ENTRIES_PER_ROW;       // 0..3
            int col = i % ENTRIES_PER_ROW;       // 0..6
            int slot = (row + 1) * 9 + (col + 1); // rows 1..4, columns 1..7
            int index = start + i;
            if (index >= all.size()) {
                continue;
            }
            Shortcut shortcut = all.get(index);
            container.setItem(slot, entryItem(shortcut));
            pageAliases.set(slot, shortcut.alias);
        }

        // --- Bottom action row ---
        if (safePage > 0) {
            container.setItem(SLOT_PREV, navItem(Items.SPECTRAL_ARROW, "Previous Page",
                    ChatFormatting.YELLOW, "Page " + safePage + " of " + totalPages));
        } else {
            container.setItem(SLOT_PREV, blankPane(Items.GRAY_STAINED_GLASS_PANE));
        }
        container.setItem(SLOT_CREATE, navItem(Items.LIME_CONCRETE, "Create Shortcut",
                ChatFormatting.GREEN,
                "Use the chat command:",
                ChatFormatting.WHITE + "/fshortcuts create <alias> <command>",
                "",
                ChatFormatting.DARK_GRAY + "Example: /fshortcuts create gc gamemode creative"));
        container.setItem(SLOT_RELOAD, navItem(Items.CLOCK, "Reload",
                ChatFormatting.AQUA, "Reload shortcuts from disk."));
        container.setItem(SLOT_CLOSE, navItem(Items.BARRIER, "Close",
                ChatFormatting.RED, "Close this menu."));
        container.setItem(SLOT_INFO, navItem(Items.WRITTEN_BOOK, "About",
                ChatFormatting.LIGHT_PURPLE,
                "Fantastic Shortcuts",
                ChatFormatting.DARK_GRAY + "by Pewez777",
                "",
                ChatFormatting.GRAY + "Left click an entry: info",
                ChatFormatting.GRAY + "Right / shift click: delete"));
        if (safePage < totalPages - 1) {
            container.setItem(SLOT_NEXT, navItem(Items.SPECTRAL_ARROW, "Next Page",
                    ChatFormatting.YELLOW, "Page " + (safePage + 2) + " of " + totalPages));
        } else {
            container.setItem(SLOT_NEXT, blankPane(Items.GRAY_STAINED_GLASS_PANE));
        }

        boolean hasNext = safePage < totalPages - 1;
        final int currentPage = safePage;
        Component title = Component.literal("F-Shortcuts ")
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal("(" + (safePage + 1) + "/" + totalPages + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));

        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new ShortcutMenu(id, inv, container, currentPage, pageAliases, hasNext),
                title);
        player.openMenu(provider);
    }

    // ---------- Item builders ----------

    private static ItemStack headerItem(int total, int page, int totalPages) {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        named(stack, Component.literal("Fantastic Shortcuts").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return withLore(stack, List.of(
                Component.literal("Premium command shortcut manager").withStyle(ChatFormatting.GRAY),
                Component.empty(),
                Component.literal("Total shortcuts: ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.AQUA)),
                Component.literal("Page: ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(page + " / " + totalPages).withStyle(ChatFormatting.AQUA))
        ));
    }

    private static ItemStack entryItem(Shortcut shortcut) {
        ItemStack stack = new ItemStack(Items.COMMAND_BLOCK);
        named(stack, Component.literal("/" + shortcut.alias).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("-> /" + shortcut.command).withStyle(ChatFormatting.WHITE));
        if (shortcut.description != null && !shortcut.description.isBlank()) {
            lore.add(Component.literal(shortcut.description).withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(Component.literal("Allow arguments: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(String.valueOf(shortcut.allowArguments))
                        .withStyle(shortcut.allowArguments ? ChatFormatting.GREEN : ChatFormatting.RED)));
        lore.add(Component.literal("Replace original: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(String.valueOf(shortcut.replaceOriginal))
                        .withStyle(shortcut.replaceOriginal ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY)));
        lore.add(Component.empty());
        lore.add(Component.literal("Left click: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("info").withStyle(ChatFormatting.GREEN)));
        lore.add(Component.literal("Right / shift click: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("delete").withStyle(ChatFormatting.RED)));
        return withLore(stack, lore);
    }

    private static ItemStack navItem(net.minecraft.world.item.Item item, String name, ChatFormatting nameColor,
                                     String... loreLines) {
        ItemStack stack = new ItemStack(item);
        named(stack, Component.literal(name).withStyle(nameColor, ChatFormatting.BOLD));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            if (line == null || line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        return withLore(stack, lore);
    }

    private static ItemStack blankPane(net.minecraft.world.item.Item paneItem) {
        ItemStack stack = new ItemStack(paneItem);
        named(stack, Component.literal(" "));
        return stack;
    }

    private static ItemStack named(ItemStack stack, Component name) {
        // Disable italic styling that vanilla applies to renamed items, so we get clean labels.
        Component clean = name.copy().withStyle(name.getStyle().withItalic(false));
        stack.setHoverName(clean);
        return stack;
    }

    private static ItemStack withLore(ItemStack stack, List<Component> lore) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            Component noItalic = line.copy().withStyle(line.getStyle().withItalic(false));
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(noItalic)));
        }
        display.put("Lore", loreTag);
        return stack;
    }

    // ---------- Menu (read-only with click handlers) ----------

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
            // Ignore clicks outside the GUI container; never allow taking items.
            if (slotId < 0 || slotId >= SIZE) {
                resync();
                return;
            }

            switch (slotId) {
                case SLOT_PREV -> {
                    if (page > 0) {
                        open(serverPlayer, page - 1);
                    } else {
                        resync();
                    }
                    return;
                }
                case SLOT_NEXT -> {
                    if (hasNext) {
                        open(serverPlayer, page + 1);
                    } else {
                        resync();
                    }
                    return;
                }
                case SLOT_CLOSE -> {
                    serverPlayer.closeContainer();
                    return;
                }
                case SLOT_RELOAD -> {
                    ShortcutManager.get().reload();
                    FantasticShortcutsMod.liveSync(serverPlayer.getServer());
                    serverPlayer.sendSystemMessage(ChatPrefix.success(
                            "Reloaded. " + ShortcutManager.get().all().size() + " shortcuts loaded."));
                    open(serverPlayer, page);
                    return;
                }
                case SLOT_CREATE -> {
                    serverPlayer.sendSystemMessage(ChatPrefix.info(
                            "To create a shortcut: /fshortcuts create <alias> <command>"));
                    serverPlayer.sendSystemMessage(ChatPrefix.info(
                            "Example: /fshortcuts create gc gamemode creative"));
                    resync();
                    return;
                }
                case SLOT_INFO, SLOT_HEADER -> {
                    resync();
                    return;
                }
                default -> {
                    // Entry click?
                    String alias = pageAliases.get(slotId);
                    if (alias == null) {
                        resync();
                        return;
                    }
                    boolean delete = clickType == ClickType.QUICK_MOVE || button == 1;
                    if (delete) {
                        ShortcutManager.Result result = ShortcutManager.get()
                                .delete(alias, serverPlayer.getGameProfile().getName());
                        serverPlayer.sendSystemMessage(result.success()
                                ? ChatPrefix.success(result.message())
                                : ChatPrefix.error(result.message()));
                        FantasticShortcutsMod.liveSync(serverPlayer.getServer());
                        open(serverPlayer, page);
                    } else {
                        Shortcut shortcut = ShortcutManager.get().get(alias);
                        if (shortcut != null) {
                            serverPlayer.sendSystemMessage(ChatPrefix.info(
                                    "/" + shortcut.alias + " -> /" + shortcut.command
                                            + "  (allowArgs=" + shortcut.allowArguments
                                            + ", replace=" + shortcut.replaceOriginal + ")"));
                        }
                        resync();
                    }
                }
            }
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
            return false; // pure button GUI, never let items move
        }
    }
}
