package com.claimblocks.gui;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-claim admin sub-menu (3 rows / 27 slots, but reuses 9x6 generic for
 * vanilla compatibility - upper rows are filler).
 *
 *   slot 11: teleport to claim
 *   slot 12: view/edit flags
 *   slot 13: delete claim (with confirm flow)
 *   slot 15: transfer claim
 *   slot 22: back to admin panel
 */
public class AdminClaimSubMenuHandler extends ScreenHandler {
    public static final int SIZE = 54;

    private static final int SLOT_TELEPORT = 11;
    private static final int SLOT_FLAGS    = 12;
    private static final int SLOT_DELETE   = 13;
    private static final int SLOT_TRANSFER = 15;
    private static final int SLOT_BACK     = 22;

    /** Claim id awaiting an OP transfer; OP id -> claim id. */
    private static final Map<UUID, UUID> pendingTransfers = new HashMap<>();

    private final SimpleInventory inv = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final ServerPlayerEntity viewer;
    private final UUID claimId;
    private boolean awaitingDeleteConfirm = false;

    public AdminClaimSubMenuHandler(int syncId, PlayerInventory pInv, UUID claimId) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.viewer = (ServerPlayerEntity) pInv.player;
        this.claimId = claimId;

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

    private Claim claim() { return AdminPanelHandler.findClaim(claimId); }

    private void rebuild() {
        inv.clear();
        ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) inv.setStack(i, bg.copy());

        Claim c = claim();
        if (c == null) return;
        String owner = c.getOwnerName();

        inv.setStack(SLOT_TELEPORT, withLore(
            withName(new ItemStack(Items.COMPASS),
                Text.literal("Teleportar al claim").formatted(Formatting.AQUA, Formatting.BOLD)),
            List.of(Text.literal("Te lleva al centro del claim de " + owner).formatted(Formatting.GRAY))
        ));
        inv.setStack(SLOT_FLAGS, withLore(
            withName(new ItemStack(Items.LEVER),
                Text.literal("Ver y editar flags").formatted(Formatting.YELLOW, Formatting.BOLD)),
            List.of(Text.literal("Abre el menú de flags de este claim").formatted(Formatting.GRAY))
        ));
        if (awaitingDeleteConfirm) {
            inv.setStack(SLOT_DELETE, withLore(
                withName(new ItemStack(Items.TNT),
                    Text.literal("¿Confirmar eliminación?").formatted(Formatting.RED, Formatting.BOLD)),
                List.of(
                    Text.literal("Esto eliminará la zona de " + owner).formatted(Formatting.YELLOW),
                    Text.literal("El bloque NO se devuelve al dueño").formatted(Formatting.RED),
                    Text.literal("Clic de nuevo para confirmar").formatted(Formatting.GRAY)
                )
            ));
        } else {
            inv.setStack(SLOT_DELETE, withLore(
                withName(new ItemStack(Items.BARRIER),
                    Text.literal("Eliminar este claim").formatted(Formatting.RED, Formatting.BOLD)),
                List.of(
                    Text.literal("Elimina la zona de " + owner).formatted(Formatting.YELLOW),
                    Text.literal("Clic para pedir confirmación").formatted(Formatting.GRAY)
                )
            ));
        }
        inv.setStack(SLOT_TRANSFER, withLore(
            withName(new ItemStack(Items.PAPER),
                Text.literal("Transferir claim").formatted(Formatting.GREEN, Formatting.BOLD)),
            List.of(Text.literal("Cambia el dueño de esta zona").formatted(Formatting.GRAY))
        ));
        inv.setStack(SLOT_BACK, withName(new ItemStack(Items.ARROW),
            Text.literal("Volver al panel").formatted(Formatting.AQUA)));

        sendContentUpdates();
    }

    @Override
    public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        if (slot < 0 || slot >= SIZE) {
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slot, button, action, player);
            return;
        }

        Claim c = claim();
        if (c == null) {
            viewer.closeHandledScreen();
            return;
        }

        if (slot != SLOT_DELETE && awaitingDeleteConfirm) {
            awaitingDeleteConfirm = false;
        }

        if (slot == SLOT_BACK) {
            AdminPanelHandler.open(viewer, 0);
            return;
        }
        if (slot == SLOT_TELEPORT) {
            teleportToClaim(c);
            return;
        }
        if (slot == SLOT_FLAGS) {
            String title = "[Admin] Flags de " + c.getOwnerName() + " - " + c.sizeLabel();
            ClaimMenuHandler.open(viewer, c, 0, title);
            return;
        }
        if (slot == SLOT_DELETE) {
            if (!awaitingDeleteConfirm) {
                awaitingDeleteConfirm = true;
                rebuild();
                return;
            }
            adminDelete(c);
            return;
        }
        if (slot == SLOT_TRANSFER) {
            startTransfer(c);
            return;
        }
        rebuild();
    }

    private void teleportToClaim(Claim c) {
        ServerWorld world = null;
        for (ServerWorld w : viewer.getServer().getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(c.getWorld())) {
                world = w;
                break;
            }
        }
        if (world == null) {
            viewer.sendMessage(Text.literal("[x] No se pudo encontrar la dimensión.")
                .formatted(Formatting.RED), false);
            viewer.closeHandledScreen();
            return;
        }
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
        BlockPos target = new BlockPos(c.getX(), topY, c.getZ());
        viewer.teleport(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
            java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
            viewer.getYaw(), viewer.getPitch());
        viewer.sendMessage(Text.literal("✔ Teletransportado a la zona de " + c.getOwnerName() + ".")
            .formatted(Formatting.GREEN), false);
        viewer.closeHandledScreen();
    }

    private void adminDelete(Claim c) {
        String ownerName = c.getOwnerName();
        UUID ownerId = c.getOwnerUUID();
        ServerWorld world = null;
        for (ServerWorld w : viewer.getServer().getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(c.getWorld())) {
                world = w;
                break;
            }
        }
        if (world != null) {
            BlockPos pos = c.getCenter();
            if (world.getBlockState(pos).getBlock() instanceof ClaimStoneBlock) {
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
        }
        ClaimManager.getInstance().removeClaim(world, c.getCenter());
        viewer.sendMessage(Text.literal("✔ Zona de " + ownerName + " eliminada por admin.")
            .formatted(Formatting.GREEN, Formatting.BOLD), false);
        // Notify owner if online
        ServerPlayerEntity owner = viewer.getServer().getPlayerManager().getPlayer(ownerId);
        Text msg = Text.literal("[!] Un administrador eliminó tu zona ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(c.sizeLabel()).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" en X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.YELLOW));
        if (owner != null) {
            owner.sendMessage(msg, false);
        } else {
            ClaimManager.getInstance().queueMessage(ownerId, msg);
        }
        viewer.closeHandledScreen();
    }

    private void startTransfer(Claim c) {
        pendingTransfers.put(viewer.getUuid(), c.getClaimId());
        viewer.sendMessage(Text.literal("[i] Escribe el nombre del nuevo dueño en el chat.")
            .formatted(Formatting.AQUA), false);
        viewer.sendMessage(Text.literal("    Escribe 'cancelar' para abortar.")
            .formatted(Formatting.GRAY), false);
        viewer.closeHandledScreen();
    }

    public static UUID popPendingTransfer(UUID opId) {
        return pendingTransfers.remove(opId);
    }

    public static boolean hasPendingTransfer(UUID opId) {
        return pendingTransfers.containsKey(opId);
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

    public static void open(ServerPlayerEntity player, UUID claimId) {
        Claim c = AdminPanelHandler.findClaim(claimId);
        if (c == null) {
            player.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            return;
        }
        String title = "Admin - " + c.getOwnerName() + " " + c.sizeLabel();
        if (title.length() > 40) title = title.substring(0, 37) + "...";
        final String t = title;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new AdminClaimSubMenuHandler(syncId, pInv, claimId),
            Text.literal(t).formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }
}
