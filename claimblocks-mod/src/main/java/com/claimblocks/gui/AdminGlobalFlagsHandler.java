package com.claimblocks.gui;

import com.claimblocks.data.GlobalFlags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Sub-menu for the 3 server-wide global flags. Click toggles the value.
 *
 *   slot 11: globalPVP
 *   slot 13: globalMobGriefing
 *   slot 15: globalFireSpread
 *   slot 22: back to admin panel
 */
public class AdminGlobalFlagsHandler extends ScreenHandler {
    public static final int SIZE = 54;
    private static final int SLOT_PVP    = 11;
    private static final int SLOT_GRIEF  = 13;
    private static final int SLOT_FIRE   = 15;
    private static final int SLOT_BACK   = 22;

    private final SimpleInventory inv = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final ServerPlayerEntity viewer;

    public AdminGlobalFlagsHandler(int syncId, PlayerInventory pInv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.viewer = (ServerPlayerEntity) pInv.player;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                final int idx = col + row * 9;
                addSlot(new Slot(inv, idx, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean canTakeItems(PlayerEntity p) { return false; }
                    @Override public boolean canInsert(ItemStack s) { return false; }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(pInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(pInv, col, 8 + col * 18, 198));
        }
        rebuild();
    }

    private void rebuild() {
        inv.clear();
        ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) inv.setStack(i, bg.copy());

        GlobalFlags g = GlobalFlags.getInstance();
        inv.setStack(SLOT_PVP, flagButton("PVP global", g.globalPVP, "Permite PVP fuera de claims"));
        inv.setStack(SLOT_GRIEF, flagButton("Mob griefing global", g.globalMobGriefing,
            "Mobs destruyen bloques fuera de claims"));
        inv.setStack(SLOT_FIRE, flagButton("Propagación de fuego", g.globalFireSpread,
            "Fire spread global gamerule"));
        inv.setStack(SLOT_BACK, withName(new ItemStack(Items.ARROW),
            Text.literal("Volver al panel").formatted(Formatting.AQUA)));
        sendContentUpdates();
    }

    private static ItemStack flagButton(String name, boolean on, String desc) {
        ItemStack stack = new ItemStack(on ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE);
        Text title = Text.literal(name + " " + (on ? "[ON]" : "[OFF]"))
            .formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD);
        return withLore(withName(stack, title), List.of(
            Text.literal(desc).formatted(Formatting.GRAY),
            Text.literal("Estado: " + (on ? "ACTIVO" : "INACTIVO") + " - Clic para cambiar")
                .formatted(Formatting.GRAY)
        ));
    }

    @Override
    public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        if (slot < 0 || slot >= SIZE) {
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slot, button, action, player);
            return;
        }
        if (slot == SLOT_BACK) { AdminPanelHandler.open(viewer, 0); return; }

        GlobalFlags g = GlobalFlags.getInstance();
        String name = null;
        boolean newVal = false;
        if (slot == SLOT_PVP)   { name = "globalPVP";         newVal = !g.globalPVP; }
        if (slot == SLOT_GRIEF) { name = "globalMobGriefing"; newVal = !g.globalMobGriefing; }
        if (slot == SLOT_FIRE)  { name = "globalFireSpread";  newVal = !g.globalFireSpread; }
        if (name != null) {
            g.set(name, newVal, viewer.getServer());
            // broadcast
            Text bcast = Text.literal("[!] Un administrador cambió una configuración global del servidor.")
                .formatted(Formatting.YELLOW);
            viewer.getServer().getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(bcast, false));
            rebuild();
        }
    }

    @Override public ItemStack quickMove(PlayerEntity p, int s) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity p) { return true; }

    private static ItemStack withName(ItemStack s, Text t) {
        s.set(DataComponentTypes.CUSTOM_NAME, t);
        return s;
    }
    private static ItemStack withLore(ItemStack s, List<Text> lore) {
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return s;
    }

    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new AdminGlobalFlagsHandler(syncId, pInv),
            Text.literal("Flags Globales").formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }
}
