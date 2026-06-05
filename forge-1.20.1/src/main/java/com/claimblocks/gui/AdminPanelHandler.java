package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;

public class AdminPanelHandler extends ChestMenu {
    private static final int CLAIMS_PER_PAGE = 45;

    private final SimpleContainer inv;
    private final ServerPlayer viewer;
    private final int page;
    private final List<Claim> claims;

    public AdminPanelHandler(int syncId, Inventory pInv, int page) {
        this(syncId, pInv, new SimpleContainer(54), page);
    }

    private AdminPanelHandler(int syncId, Inventory pInv, SimpleContainer inv, int page) {
        super(MenuType.GENERIC_9x6, syncId, pInv, inv, 6);
        this.inv = inv;
        this.viewer = (ServerPlayer) pInv.player;
        this.page = page;
        this.claims = ClaimManager.getInstance().getAllClaims();
        this.rebuild();
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    private void rebuild() {
        ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Component.literal(" "));
        for (int i = 0; i < 54; ++i) this.inv.setItem(i, bg.copy());

        int start = this.page * CLAIMS_PER_PAGE;
        int end = Math.min(start + CLAIMS_PER_PAGE, this.claims.size());
        for (int i = start; i < end; ++i) {
            this.inv.setItem(i - start, claimItem(this.claims.get(i)));
        }
        if (this.page > 0) {
            this.inv.setItem(45, withName(new ItemStack(Items.ARROW), Component.literal("<< P\u00e1gina anterior").withStyle(ChatFormatting.AQUA)));
        }
        this.inv.setItem(46, withLore(withName(new ItemStack(Items.BOOK), Component.literal("Estad\u00edsticas").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)),
            List.of(Component.literal("Resumen del servidor").withStyle(ChatFormatting.GRAY))));
        this.inv.setItem(47, withLore(withName(new ItemStack(Items.COMPARATOR), Component.literal("Flags Globales").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)),
            List.of(Component.literal("PVP / Mob griefing / Fire").withStyle(ChatFormatting.GRAY))));
        boolean bypassing = ClaimManager.getInstance().isBypassing(this.viewer.getUUID());
        this.inv.setItem(48, withLore(withName(new ItemStack(Items.ENDER_EYE), Component.literal("Modo Bypass: " + (bypassing ? "ON" : "OFF")).withStyle(bypassing ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD)),
            List.of(Component.literal("Ignorar protecciones de zonas").withStyle(ChatFormatting.GRAY))));
        this.inv.setItem(49, withName(new ItemStack(Items.BARRIER), Component.literal("Cerrar panel").withStyle(ChatFormatting.WHITE)));
        if (end < this.claims.size()) {
            this.inv.setItem(53, withName(new ItemStack(Items.ARROW), Component.literal("P\u00e1gina siguiente >>").withStyle(ChatFormatting.AQUA)));
        }
        this.broadcastChanges();
    }

    private static ItemStack claimItem(Claim c) {
        ClaimTier tier = c.getTier();
        Block block = tier != null ? ClaimBlocks.blockForTier(tier) : null;
        ItemStack stack = block != null ? new ItemStack(block.asItem()) : new ItemStack(Items.PAPER);
        Component name = Component.literal(c.getOwnerName() + " - " + c.sizeLabel()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        return withLore(withName(stack, name), List.of(
            Component.literal("Posici\u00f3n: X:" + c.getX() + " Z:" + c.getZ()).withStyle(ChatFormatting.GRAY),
            Component.literal("Dimensi\u00f3n: " + c.getWorld()).withStyle(ChatFormatting.DARK_AQUA),
            Component.literal("Clic para gestionar este claim").withStyle(ChatFormatting.YELLOW)));
    }

    static ItemStack withName(ItemStack s, Component t) {
        s.setHoverName(t);
        return s;
    }

    static ItemStack withLore(ItemStack s, List<Component> lore) {
        ClaimBlocks.setLore(s, lore);
        return s;
    }

    @Override
    public void clicked(int slot, int button, ClickType clickType, Player player) {
        if (slot < 0 || slot >= 54) return;
        if (slot == 45 && this.page > 0) { open(this.viewer, this.page - 1); return; }
        if (slot == 53) {
            int max = (this.claims.size() - 1) / CLAIMS_PER_PAGE;
            if (this.page < max) open(this.viewer, this.page + 1);
            return;
        }
        if (slot == 49) { this.viewer.closeContainer(); return; }
        if (slot == 46) {
            this.viewer.closeContainer();
            this.viewer.server.getCommands().performPrefixedCommand(this.viewer.createCommandSourceStack(), "claimadmin stats");
            return;
        }
        if (slot == 47) { AdminGlobalFlagsHandler.open(this.viewer); return; }
        if (slot == 48) { ClaimManager.getInstance().toggleBypass(this.viewer.getUUID()); this.rebuild(); return; }
        int idx = this.page * CLAIMS_PER_PAGE + slot;
        if (idx < this.claims.size()) {
            AdminClaimSubMenuHandler.open(this.viewer, this.claims.get(idx).getClaimId());
        }
    }

    public static void open(ServerPlayer player, int page) {
        int p = Math.max(0, page);
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Panel de Administraci\u00f3n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
                return new AdminPanelHandler(id, inv, p);
            }
        });
    }

    public static Claim findClaim(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }
}
