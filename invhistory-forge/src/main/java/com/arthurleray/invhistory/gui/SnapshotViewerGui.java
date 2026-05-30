package com.arthurleray.invhistory.gui;

import com.arthurleray.invhistory.data.InventorySnapshot;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Server-side chest GUI to browse and restore a player's inventory snapshots. */
public class SnapshotViewerGui {
    private static final int SIZE = 54;
    private static final int GRAY = -5592406;
    private static final int GREEN = -11141291;
    private static final int RED = -43691;

    public static void open(ServerPlayer admin, GameProfile target, List<InventorySnapshot> snapshots, int index) {
        if (index < 0 || index >= snapshots.size()) {
            return;
        }
        SimpleContainer container = new SimpleContainer(SIZE);
        fillContainer(container, snapshots, index);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date(snapshots.get(index).getTimestamp()));
        String title = target.getName() + "'s Inventory - " + dateStr;
        admin.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) ->
                        new SnapshotMenu(containerId, playerInventory, container, target, snapshots, index),
                Component.literal(title)));
    }


    private static Component label(String s) {
        return Component.literal(s).withStyle(Style.EMPTY.withItalic(false));
    }

    private static Component label(String s, int color) {
        return Component.literal(s).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    private static ItemStack named(Item item, Component name) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(name);
        return stack;
    }

    private static void setLore(ItemStack stack, List<Component> lore) {
        ListTag list = new ListTag();
        for (Component c : lore) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        stack.getOrCreateTagElement("display").put("Lore", list);
    }

    static void fillContainer(SimpleContainer container, List<InventorySnapshot> snapshots, int index) {
        InventorySnapshot snapshot = snapshots.get(index);
        for (int i = 0; i < SIZE; ++i) {
            container.setItem(i, ItemStack.EMPTY);
        }
        for (InventorySnapshot.SlotData slot : snapshot.getSlots()) {
            ItemStack stack = ItemStack.of(slot.tag());
            if (slot.slot() >= 41) {
                continue;
            }
            container.setItem(slot.slot(), stack);
        }
        ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, label(" "));
        for (int i = 41; i < 45; ++i) {
            container.setItem(i, filler.copy());
        }
        for (int i = 45; i < SIZE; ++i) {
            container.setItem(i, filler.copy());
        }
        if (index > 0) {
            container.setItem(45, named(Items.ARROW, label("\u25c0 Previous Snapshot")));
        }
        if (index < snapshots.size() - 1) {
            container.setItem(53, named(Items.ARROW, label("Next Snapshot \u25b6")));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date(snapshot.getTimestamp()));
        ItemStack info = named(Items.CLOCK, label("Snapshot " + (index + 1) + "/" + snapshots.size()));
        setLore(info, List.of(label(dateStr, GRAY), label("Reason: " + snapshot.getReason(), GRAY)));
        container.setItem(49, info);
        ItemStack restore = named(Items.EMERALD_BLOCK, label("Restore Inventory", GREEN));
        setLore(restore, List.of(label("Click to restore this snapshot", GRAY)));
        container.setItem(50, restore);
    }


    public static class SnapshotMenu extends AbstractContainerMenu {
        private final SimpleContainer container;
        private final GameProfile target;
        private final List<InventorySnapshot> snapshots;
        private int currentIndex;
        private boolean confirmRestore = false;

        public SnapshotMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                            GameProfile target, List<InventorySnapshot> snapshots, int index) {
            super(MenuType.GENERIC_9x6, containerId);
            this.container = container;
            this.target = target;
            this.snapshots = snapshots;
            this.currentIndex = index;
            for (int row = 0; row < 6; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
                }
            }
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
                }
            }
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }


        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (!(player instanceof ServerPlayer admin)) {
                return;
            }
            if (slotId == 45 && this.currentIndex > 0) {
                --this.currentIndex;
                this.confirmRestore = false;
                SnapshotViewerGui.fillContainer(this.container, this.snapshots, this.currentIndex);
                this.broadcastChanges();
                return;
            }
            if (slotId == 53 && this.currentIndex < this.snapshots.size() - 1) {
                ++this.currentIndex;
                this.confirmRestore = false;
                SnapshotViewerGui.fillContainer(this.container, this.snapshots, this.currentIndex);
                this.broadcastChanges();
                return;
            }
            if (slotId == 50) {
                if (!this.confirmRestore) {
                    this.confirmRestore = true;
                    ItemStack confirm = named(Items.RED_WOOL, label("CONFIRM Restore?", RED));
                    setLore(confirm, List.of(label("Click again to confirm", GRAY)));
                    this.container.setItem(50, confirm);
                    this.broadcastChanges();
                } else {
                    ServerPlayer targetPlayer = admin.getServer().getPlayerList().getPlayer(this.target.getId());
                    if (targetPlayer != null) {
                        this.snapshots.get(this.currentIndex).restore(targetPlayer.getInventory());
                        admin.sendSystemMessage(Component.literal("Restored inventory for " + this.target.getName() + "."));
                    } else {
                        admin.sendSystemMessage(Component.literal("Player " + this.target.getName() + " is not online. Cannot restore."));
                    }
                    admin.closeContainer();
                }
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
