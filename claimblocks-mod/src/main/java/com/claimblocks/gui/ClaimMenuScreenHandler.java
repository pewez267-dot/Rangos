package com.claimblocks.gui;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side screen handler that re-uses the vanilla 9x6 chest GUI for
 * rendering, so the client requires no extra code. Each "button" is just an
 * item-stack the player cannot pick up; clicks are intercepted in
 * {@link #onSlotClick}.
 */
public class ClaimMenuScreenHandler extends ScreenHandler {
    public static final int ROWS = 6;
    public static final int SIZE = 9 * ROWS;

    /** flag-id -> slot index in the chest grid */
    private static final java.util.Map<Integer, String> FLAG_SLOTS = new java.util.LinkedHashMap<>();
    static {
        FLAG_SLOTS.put(10, "CREEPING");
        FLAG_SLOTS.put(11, "BREAKING");
        FLAG_SLOTS.put(12, "EXPLOSIONS");
        FLAG_SLOTS.put(13, "FIRE");
        FLAG_SLOTS.put(14, "MOBS");
        FLAG_SLOTS.put(15, "PVP");
        FLAG_SLOTS.put(16, "MOB_DAMAGE");
        FLAG_SLOTS.put(17, "TRESPASSER_ALERTS");
    }

    private static final int INFO_SLOT = 4;
    private static final int ADD_MEMBER_SLOT = 49;
    private static final int CLOSE_SLOT = 47;
    private static final int DELETE_SLOT = 53;
    private static final int FIRST_MEMBER_SLOT = 27;
    private static final int LAST_MEMBER_SLOT = 35;

    private final SimpleInventory chestInv = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
    };
    private final Claim claim;
    private final ServerPlayerEntity owner;

    public ClaimMenuScreenHandler(int syncId, PlayerInventory playerInv, Claim claim) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.claim = claim;
        this.owner = (ServerPlayerEntity) playerInv.player;

        // Chest slots (read-only buttons)
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                final int sIdx = col + row * 9;
                this.addSlot(new Slot(chestInv, sIdx, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean canTakeItems(PlayerEntity p) { return false; }
                    @Override public boolean canInsert(ItemStack stack) { return false; }
                });
            }
        }

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }

        rebuild();
    }

    public Claim getClaim() { return claim; }

    private void rebuild() {
        chestInv.clear();
        // Filler glass-panes
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        for (int i = 0; i < SIZE; i++) {
            chestInv.setStack(i, filler.copy());
        }

        // Info slot (top-center)
        Block iconBlock = ModBlocks.getBlockForTier(claim.getTier());
        ItemStack infoStack = new ItemStack(iconBlock != null ? iconBlock.asItem() : Items.NAME_TAG);
        infoStack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Claim Info - Tier " + claim.getTier()).formatted(Formatting.GOLD, Formatting.BOLD));
        BlockPos c = claim.getCenter();
        int side = claim.getRadius() * 2 + 1;
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("Owner: ").formatted(Formatting.GRAY)
            .append(Text.literal(claim.getOwnerName()).formatted(Formatting.GREEN)));
        lore.add(Text.literal("Tier: ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(claim.getTier())).formatted(Formatting.AQUA)));
        lore.add(Text.literal("Center: ").formatted(Formatting.GRAY)
            .append(Text.literal("[" + c.getX() + ", " + c.getY() + ", " + c.getZ() + "]").formatted(Formatting.YELLOW)));
        lore.add(Text.literal("Area: ").formatted(Formatting.GRAY)
            .append(Text.literal(side + "x" + side + "x" + side + " blocks").formatted(Formatting.LIGHT_PURPLE)));
        lore.add(Text.literal("Members: ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(claim.getMembers().size())).formatted(Formatting.WHITE)));
        infoStack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        chestInv.setStack(INFO_SLOT, infoStack);

        // Flag toggles
        ClaimFlags flags = claim.getFlags();
        FLAG_SLOTS.forEach((slot, flagKey) -> {
            chestInv.setStack(slot, makeFlagButton(flagKey, flags.get(flagKey)));
        });

        // Member heads (up to 9)
        List<UUID> members = claim.getMembers();
        List<String> memberNames = claim.getMemberNames();
        int memberCount = Math.min(members.size(), LAST_MEMBER_SLOT - FIRST_MEMBER_SLOT + 1);
        for (int i = 0; i < memberCount; i++) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            String name = i < memberNames.size() ? memberNames.get(i) : members.get(i).toString();
            head.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).formatted(Formatting.AQUA));
            head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Right-click to remove").formatted(Formatting.RED)
            )));
            chestInv.setStack(FIRST_MEMBER_SLOT + i, head);
        }
        // Empty member slots show a "-"
        for (int i = memberCount; i < (LAST_MEMBER_SLOT - FIRST_MEMBER_SLOT + 1); i++) {
            ItemStack empty = new ItemStack(Items.WHITE_STAINED_GLASS_PANE);
            empty.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("(empty member slot)").formatted(Formatting.DARK_GRAY));
            chestInv.setStack(FIRST_MEMBER_SLOT + i, empty);
        }

        // Action buttons (bottom row)
        ItemStack add = new ItemStack(Items.WRITABLE_BOOK);
        add.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Add Member").formatted(Formatting.GREEN, Formatting.BOLD));
        add.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("Click, then type the player name in chat").formatted(Formatting.GRAY)
        )));
        chestInv.setStack(ADD_MEMBER_SLOT, add);

        ItemStack close = new ItemStack(Items.OAK_DOOR);
        close.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Close").formatted(Formatting.WHITE));
        chestInv.setStack(CLOSE_SLOT, close);

        ItemStack del = new ItemStack(Items.BARRIER);
        del.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal("Delete Claim").formatted(Formatting.RED, Formatting.BOLD));
        del.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("Shift-click to confirm").formatted(Formatting.YELLOW),
            Text.literal("This breaks the block and refunds the item").formatted(Formatting.GRAY)
        )));
        chestInv.setStack(DELETE_SLOT, del);

        sendContentUpdates();
    }

    private ItemStack makeFlagButton(String key, boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? Items.LIME_DYE : Items.GRAY_DYE);
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.literal(prettyName(key))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("State: ").formatted(Formatting.GRAY)
            .append(Text.literal(enabled ? "ENABLED" : "DISABLED")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED)));
        lore.add(Text.literal(describe(key)).formatted(Formatting.DARK_GRAY));
        lore.add(Text.literal("Click to toggle").formatted(Formatting.YELLOW));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private String prettyName(String key) {
        return switch (key) {
            case "CREEPING" -> "Creeping (others place)";
            case "BREAKING" -> "Breaking (others break)";
            case "EXPLOSIONS" -> "Explosions";
            case "FIRE" -> "Fire / Lava";
            case "MOBS" -> "Mob Spawning";
            case "PVP" -> "PvP";
            case "MOB_DAMAGE" -> "Mob Damage";
            case "TRESPASSER_ALERTS" -> "Trespasser Alerts";
            default -> key;
        };
    }

    private String describe(String key) {
        return switch (key) {
            case "CREEPING" -> "Allow non-members to place blocks";
            case "BREAKING" -> "Allow non-members to break blocks";
            case "EXPLOSIONS" -> "Allow explosions inside the claim";
            case "FIRE" -> "Allow fire and lava spread";
            case "MOBS" -> "Allow mobs to spawn";
            case "PVP" -> "Allow PvP between players";
            case "MOB_DAMAGE" -> "Allow mobs to damage";
            case "TRESPASSER_ALERTS" -> "Notify owner of intruders";
            default -> "";
        };
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType action, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= SIZE) {
            // Player inventory click - block all transfers between menu and inventory
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slotIndex, button, action, player);
            return;
        }

        // Menu (chest) slots: handle as button
        if (FLAG_SLOTS.containsKey(slotIndex)) {
            String flagKey = FLAG_SLOTS.get(slotIndex);
            claim.getFlags().toggle(flagKey);
            ClaimManager.getInstance().markDirty();
            ClaimManager.getInstance().saveClaims(owner.getServer());
            owner.sendMessage(Text.literal("§e[Claim] §f" + prettyName(flagKey) + ": "
                + (claim.getFlags().get(flagKey) ? "§aON" : "§cOFF")), true);
            rebuild();
            return;
        }
        if (slotIndex == ADD_MEMBER_SLOT) {
            ClaimMenuScreen.requestAddMember(owner, claim);
            owner.closeHandledScreen();
            return;
        }
        if (slotIndex == CLOSE_SLOT) {
            owner.closeHandledScreen();
            return;
        }
        if (slotIndex == DELETE_SLOT) {
            if (button == 0 && action == SlotActionType.QUICK_CRAFT) return;
            // Require shift-click (QUICK_MOVE) as confirmation
            if (action != SlotActionType.QUICK_MOVE) {
                owner.sendMessage(Text.literal("§eShift-click to confirm deletion"), true);
                return;
            }
            BlockPos center = claim.getCenter();
            net.minecraft.world.World world = owner.getWorld();
            if (world.getBlockState(center).getBlock() instanceof com.claimblocks.block.ClaimBlock) {
                world.breakBlock(center, true, owner);
            } else {
                ClaimManager.getInstance().removeClaim(world, center);
                ClaimManager.getInstance().saveClaims(owner.getServer());
            }
            owner.sendMessage(Text.literal("§a[Claim] §fClaim deleted."), false);
            owner.closeHandledScreen();
            return;
        }
        if (slotIndex >= FIRST_MEMBER_SLOT && slotIndex <= LAST_MEMBER_SLOT) {
            int idx = slotIndex - FIRST_MEMBER_SLOT;
            List<UUID> members = claim.getMembers();
            if (idx >= 0 && idx < members.size()) {
                UUID id = members.get(idx);
                String name = idx < claim.getMemberNames().size() ? claim.getMemberNames().get(idx) : id.toString();
                claim.removeMember(id);
                ClaimManager.getInstance().markDirty();
                ClaimManager.getInstance().saveClaims(owner.getServer());
                owner.sendMessage(Text.literal("§e[Claim] §fRemoved member: §c" + name), true);
                rebuild();
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY; // disable shift-click transfers between inventories
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public Inventory getChestInv() { return chestInv; }
}
