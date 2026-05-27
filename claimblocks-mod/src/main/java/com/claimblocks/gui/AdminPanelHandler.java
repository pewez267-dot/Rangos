package com.claimblocks.gui;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import net.minecraft.block.Block;
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
import java.util.UUID;

/**
 * Top-level admin panel. Uses a vanilla 9x6 chest GUI:
 *
 *   slots 0..44  - one slot per claim (paginated)
 *   slot 45      - prev page
 *   slot 46      - "Estadísticas" (executes /claimadmin stats)
 *   slot 47      - "Flags Globales"  (opens AdminGlobalFlagsHandler)
 *   slot 48      - "Modo Bypass"     (toggles bypass)
 *   slot 49      - "Cerrar panel"
 *   slot 53      - next page
 *   slots 50..52 - filler glass
 *
 * Click a claim slot -> opens AdminClaimSubMenuHandler for that claim.
 */
public class AdminPanelHandler extends ScreenHandler {
    public static final int SIZE = 54;
    private static final int CLAIMS_PER_PAGE = 45;

    private static final int SLOT_PREV  = 45;
    private static final int SLOT_STATS = 46;
    private static final int SLOT_GFLAG = 47;
    private static final int SLOT_BYPASS = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT  = 53;

    private final SimpleInventory inv = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final ServerPlayerEntity viewer;
    private final int page;
    private final List<Claim> claims;

    public AdminPanelHandler(int syncId, PlayerInventory pInv, int page) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.viewer = (ServerPlayerEntity) pInv.player;
        this.page = page;
        this.claims = ClaimManager.getInstance().getAllClaims();

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
        ItemStack bg = withName(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) inv.setStack(i, bg.copy());

        int start = page * CLAIMS_PER_PAGE;
        int end = Math.min(start + CLAIMS_PER_PAGE, claims.size());
        for (int i = start; i < end; i++) {
            Claim c = claims.get(i);
            int slot = i - start;
            inv.setStack(slot, claimItem(c));
        }

        // Bottom row controls
        if (page > 0) {
            inv.setStack(SLOT_PREV, withName(new ItemStack(Items.ARROW),
                Text.literal("<< Página anterior").formatted(Formatting.AQUA)));
        }
        inv.setStack(SLOT_STATS, withLore(
            withName(new ItemStack(Items.BOOK),
                Text.literal("Estadísticas").formatted(Formatting.GOLD, Formatting.BOLD)),
            List.of(Text.literal("Resumen del servidor").formatted(Formatting.GRAY))
        ));
        inv.setStack(SLOT_GFLAG, withLore(
            withName(new ItemStack(Items.LEVER),
                Text.literal("Flags Globales").formatted(Formatting.GOLD, Formatting.BOLD)),
            List.of(Text.literal("PVP / Mob griefing / Fire").formatted(Formatting.GRAY))
        ));
        boolean bypassing = ClaimManager.getInstance().isBypassing(viewer.getUuid());
        inv.setStack(SLOT_BYPASS, withLore(
            withName(new ItemStack(Items.GOLDEN_SWORD),
                Text.literal("Modo Bypass: " + (bypassing ? "ON" : "OFF"))
                    .formatted(bypassing ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)),
            List.of(Text.literal("Ignorar protecciones de zonas").formatted(Formatting.GRAY))
        ));
        inv.setStack(SLOT_CLOSE, withName(new ItemStack(Items.BARRIER),
            Text.literal("Cerrar panel").formatted(Formatting.WHITE)));
        if (end < claims.size()) {
            inv.setStack(SLOT_NEXT, withName(new ItemStack(Items.ARROW),
                Text.literal("Página siguiente >>").formatted(Formatting.AQUA)));
        }
        sendContentUpdates();
    }

    private static ItemStack claimItem(Claim c) {
        ClaimTier tier = c.getTier();
        Block block = tier != null ? ModBlocks.byId(tier.id) : null;
        ItemStack stack = block != null ? new ItemStack(block.asItem()) : new ItemStack(Items.PAPER);
        Text name = Text.literal(c.getOwnerName() + " - " + c.sizeLabel())
            .formatted(Formatting.GOLD, Formatting.BOLD);
        return withLore(withName(stack, name), List.of(
            Text.literal("Posición: X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.GRAY),
            Text.literal("Dimensión: " + c.getWorld()).formatted(Formatting.DARK_AQUA),
            Text.literal("Clic para gestionar este claim").formatted(Formatting.YELLOW)
        ));
    }

    private static ItemStack withName(ItemStack s, Text t) {
        s.set(DataComponentTypes.CUSTOM_NAME, t);
        return s;
    }

    private static ItemStack withLore(ItemStack s, List<Text> lore) {
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return s;
    }

    @Override
    public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        if (slot < 0 || slot >= SIZE) {
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slot, button, action, player);
            return;
        }

        if (slot == SLOT_PREV && page > 0) { open(viewer, page - 1); return; }
        if (slot == SLOT_NEXT) {
            int max = (claims.size() - 1) / CLAIMS_PER_PAGE;
            if (page < max) { open(viewer, page + 1); }
            return;
        }
        if (slot == SLOT_CLOSE) { viewer.closeHandledScreen(); return; }
        if (slot == SLOT_STATS) {
            viewer.closeHandledScreen();
            viewer.getServer().getCommandManager()
                .executeWithPrefix(viewer.getCommandSource(), "claimadmin stats");
            return;
        }
        if (slot == SLOT_GFLAG) {
            AdminGlobalFlagsHandler.open(viewer);
            return;
        }
        if (slot == SLOT_BYPASS) {
            ClaimManager.getInstance().toggleBypass(viewer.getUuid());
            rebuild();
            return;
        }

        // Claim slot
        int idx = page * CLAIMS_PER_PAGE + slot;
        if (idx < claims.size()) {
            AdminClaimSubMenuHandler.open(viewer, claims.get(idx).getClaimId());
        }
    }

    @Override public ItemStack quickMove(PlayerEntity p, int s) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity p) { return true; }

    public static void open(ServerPlayerEntity player, int page) {
        final int p = Math.max(0, page);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new AdminPanelHandler(syncId, pInv, p),
            Text.literal("Panel de Administración").formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }

    /** Used by sub-menus to come back. */
    public static Claim findClaim(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }
}
