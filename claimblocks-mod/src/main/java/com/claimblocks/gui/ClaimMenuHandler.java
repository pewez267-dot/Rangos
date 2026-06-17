package com.claimblocks.gui;

import com.claimblocks.block.ClaimBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
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
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The owner-only management menu, rendered as a vanilla 9x6 chest screen on
 * the client.  Each "button" is just an ItemStack with a custom name + lore;
 * clicks are intercepted in {@link #onSlotClick} and never mutate inventories.
 *
 * Slot layout (per spec, 0..53):
 *   row 0 (0..8):    title paper at slot 4
 *   row 1 (9..17):   info at 11/13/15/17, header glass elsewhere
 *   row 2 (18..26):  flag toggles 1..4 + first label
 *   row 3 (27..35):  flag toggles 5..8
 *   row 4 (36..44):  member view (38), add-member (42)
 *   row 5 (45..53):  delete (46), close (49), list (52)
 */
public class ClaimMenuHandler extends ScreenHandler {
    public static final int SIZE = 54;

    private static final int SLOT_TITLE       = 4;
    private static final int SLOT_COORDS      = 11;
    private static final int SLOT_OWNER       = 13;
    private static final int SLOT_TIER        = 15;
    private static final int SLOT_WORLD       = 17;

    // Flags row (18..25) - 8 flags
    private static final int FLAG_BUILDING    = 19;
    private static final int FLAG_BREAKING    = 20;
    private static final int FLAG_EXPLOSIONS  = 21;
    private static final int FLAG_FIRE        = 22;
    private static final int FLAG_MOB_SPAWN   = 23;
    private static final int FLAG_PVP         = 24;
    private static final int FLAG_MOB_DAMAGE  = 25;
    private static final int FLAG_ALERTS      = 28;

    // Members row
    private static final int SLOT_VIEW_MEMBERS = 38;
    private static final int SLOT_ADD_MEMBER   = 42;

    // Bottom row
    private static final int SLOT_DELETE      = 46;
    private static final int SLOT_CLOSE       = 49;
    private static final int SLOT_LIST        = 52;

    private static final Map<Integer, ClaimFlags.FlagId> FLAG_BY_SLOT = new HashMap<>();
    static {
        FLAG_BY_SLOT.put(FLAG_BUILDING,   ClaimFlags.FlagId.BUILDING);
        FLAG_BY_SLOT.put(FLAG_BREAKING,   ClaimFlags.FlagId.BREAKING);
        FLAG_BY_SLOT.put(FLAG_EXPLOSIONS, ClaimFlags.FlagId.EXPLOSIONS);
        FLAG_BY_SLOT.put(FLAG_FIRE,       ClaimFlags.FlagId.FIRE);
        FLAG_BY_SLOT.put(FLAG_MOB_SPAWN,  ClaimFlags.FlagId.MOB_SPAWN);
        FLAG_BY_SLOT.put(FLAG_PVP,        ClaimFlags.FlagId.PVP);
        FLAG_BY_SLOT.put(FLAG_MOB_DAMAGE, ClaimFlags.FlagId.MOB_DAMAGE);
        FLAG_BY_SLOT.put(FLAG_ALERTS,     ClaimFlags.FlagId.ALERTS);
    }

    /** Players awaiting a chat-input "add member" reply, keyed by uuid. */
    private static final Map<UUID, Claim> pendingAddMember = new HashMap<>();
    /** Players who have shift-clicked delete once (need a second click to confirm). */
    private static final Map<UUID, UUID> deleteConfirm = new HashMap<>();

    private final SimpleInventory chest = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final Claim claim;
    private final ServerPlayerEntity viewer;

    public ClaimMenuHandler(int syncId, PlayerInventory pInv, Claim claim) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.claim = claim;
        this.viewer = (ServerPlayerEntity) pInv.player;

        // chest slots (read-only buttons)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                final int idx = col + row * 9;
                addSlot(new Slot(chest, idx, 8 + col * 18, 18 + row * 18) {
                    @Override public boolean canTakeItems(PlayerEntity p) { return false; }
                    @Override public boolean canInsert(ItemStack s) { return false; }
                });
            }
        }
        // player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(pInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(pInv, col, 8 + col * 18, 198));
        }

        rebuild();
    }

    public Claim getClaim() { return claim; }

    private void rebuild() {
        chest.clear();
        // Background filler (gray glass panes)
        ItemStack bg = withName(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) chest.setStack(i, bg.copy());

        // Title
        ItemStack title = withName(
            new ItemStack(Items.PAPER),
            Text.literal("📦 Administrar Zona — Tier " + claim.getTier() + " de "
                + claim.getOwnerName())
                .formatted(Formatting.GOLD, Formatting.BOLD)
        );
        chest.setStack(SLOT_TITLE, title);

        // Info row
        chest.setStack(SLOT_COORDS, withLore(
            withName(new ItemStack(Items.MAP),
                Text.literal("📍 Coordenadas").formatted(Formatting.AQUA)),
            List.of(Text.literal("X=" + claim.getX() + " Y=" + claim.getY() + " Z=" + claim.getZ())
                .formatted(Formatting.WHITE))
        ));
        chest.setStack(SLOT_OWNER, withLore(
            withName(new ItemStack(Items.WRITTEN_BOOK),
                Text.literal("👤 Dueño").formatted(Formatting.AQUA)),
            List.of(Text.literal(claim.getOwnerName()).formatted(Formatting.GREEN))
        ));
        chest.setStack(SLOT_TIER, withLore(
            withName(new ItemStack(Items.NETHER_STAR),
                Text.literal("⭐ Tier " + claim.getTier()).formatted(Formatting.YELLOW)),
            List.of(Text.literal("Radio: " + claim.getRadius() + " bloques").formatted(Formatting.WHITE),
                    Text.literal("Cubo de "
                        + (claim.getRadius() * 2 + 1) + "×"
                        + (claim.getRadius() * 2 + 1) + "×"
                        + (claim.getRadius() * 2 + 1) + " bloques").formatted(Formatting.GRAY))
        ));
        chest.setStack(SLOT_WORLD, withLore(
            withName(new ItemStack(Items.CLOCK),
                Text.literal("🌍 Mundo").formatted(Formatting.AQUA)),
            List.of(Text.literal(claim.getWorld()).formatted(Formatting.WHITE))
        ));

        // Flags
        ClaimFlags f = claim.getFlags();
        chest.setStack(FLAG_BUILDING,   flagButton(ClaimFlags.FlagId.BUILDING,   f.blockBuilding,
            "Bloquear Construcción", "Otros NO pueden colocar bloques"));
        chest.setStack(FLAG_BREAKING,   flagButton(ClaimFlags.FlagId.BREAKING,   f.blockBreaking,
            "Bloquear Destrucción", "Otros NO pueden romper bloques"));
        chest.setStack(FLAG_EXPLOSIONS, flagButton(ClaimFlags.FlagId.EXPLOSIONS, f.blockExplosions,
            "Bloquear Explosiones", "TNT/creepers no dañan bloques"));
        chest.setStack(FLAG_FIRE,       flagButton(ClaimFlags.FlagId.FIRE,       f.blockFire,
            "Bloquear Fuego", "El fuego no se propaga"));
        chest.setStack(FLAG_MOB_SPAWN,  flagButton(ClaimFlags.FlagId.MOB_SPAWN,  f.blockMobSpawn,
            "Bloquear Spawn de Mobs", "Mobs hostiles no aparecen"));
        chest.setStack(FLAG_PVP,        flagButton(ClaimFlags.FlagId.PVP,        f.blockPVP,
            "Bloquear PvP", "Jugadores no se atacan"));
        chest.setStack(FLAG_MOB_DAMAGE, flagButton(ClaimFlags.FlagId.MOB_DAMAGE, f.blockMobDamage,
            "Bloquear Daño de Mobs", "Mobs no dañan jugadores"));
        chest.setStack(FLAG_ALERTS,     flagButton(ClaimFlags.FlagId.ALERTS,     f.trespasserAlerts,
            "Alertas de Intrusos", "Avisar cuando alguien entra"));

        // Members
        chest.setStack(SLOT_VIEW_MEMBERS, withLore(
            withName(new ItemStack(Items.PLAYER_HEAD),
                Text.literal("👥 Ver Miembros (" + claim.getMembers().size() + ")")
                    .formatted(Formatting.YELLOW)),
            buildMemberLore()
        ));
        chest.setStack(SLOT_ADD_MEMBER, withLore(
            withName(new ItemStack(Items.WRITABLE_BOOK),
                Text.literal("➕ Añadir Miembro").formatted(Formatting.GREEN)),
            List.of(Text.literal("Click izquierdo para añadir un miembro.").formatted(Formatting.GRAY),
                    Text.literal("Te pedirá el nombre por chat.").formatted(Formatting.DARK_GRAY))
        ));

        // Bottom row
        chest.setStack(SLOT_DELETE, withLore(
            withName(new ItemStack(Items.BARRIER),
                Text.literal("🗑️ ELIMINAR ZONA").formatted(Formatting.RED, Formatting.BOLD)),
            List.of(Text.literal("Shift+Click para confirmar").formatted(Formatting.YELLOW),
                    Text.literal("La piedra vuelve a tu inventario").formatted(Formatting.GRAY))
        ));
        chest.setStack(SLOT_CLOSE, withName(new ItemStack(Items.OAK_DOOR),
            Text.literal("❌ Cerrar").formatted(Formatting.WHITE)));
        chest.setStack(SLOT_LIST, withName(new ItemStack(Items.ARROW),
            Text.literal("📋 Ver Lista de Zonas").formatted(Formatting.AQUA)));

        sendContentUpdates();
    }

    private List<Text> buildMemberLore() {
        List<Text> lore = new ArrayList<>();
        if (claim.getMembers().isEmpty()) {
            lore.add(Text.literal("(sin miembros)").formatted(Formatting.DARK_GRAY));
        } else {
            for (int i = 0; i < claim.getMembers().size(); i++) {
                String n = i < claim.getMemberNames().size()
                    ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
                lore.add(Text.literal("• " + n).formatted(Formatting.WHITE));
            }
            lore.add(Text.literal("Click derecho aquí para abrir la lista")
                .formatted(Formatting.DARK_GRAY));
        }
        return lore;
    }

    private ItemStack flagButton(ClaimFlags.FlagId id, boolean enabled, String name, String desc) {
        ItemStack stack = new ItemStack(enabled ? Items.LIME_STAINED_GLASS : Items.RED_STAINED_GLASS);
        Text title = Text.literal((enabled ? "✅ " : "❌ ") + name)
            .formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD);
        return withLore(withName(stack, title), List.of(
            Text.literal("Estado: ").formatted(Formatting.GRAY)
                .append(Text.literal(enabled ? "ACTIVADO" : "DESACTIVADO")
                    .formatted(enabled ? Formatting.GREEN : Formatting.RED)),
            Text.literal(desc).formatted(Formatting.DARK_GRAY),
            Text.literal("Click para alternar").formatted(Formatting.YELLOW)
        ));
    }

    private static ItemStack withName(ItemStack stack, Text name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        return stack;
    }
    private static ItemStack withLore(ItemStack stack, List<Text> lore) {
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    /* ------------------------------------------------------------ click handler */

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType action, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= SIZE) {
            // player inventory click - no transfers allowed
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slotIndex, button, action, player);
            return;
        }

        // Reset delete confirmation if click is anything other than delete slot
        if (slotIndex != SLOT_DELETE) deleteConfirm.remove(viewer.getUuid());

        if (FLAG_BY_SLOT.containsKey(slotIndex)) {
            ClaimFlags.FlagId id = FLAG_BY_SLOT.get(slotIndex);
            claim.getFlags().toggle(id);
            ClaimManager.getInstance().save();
            viewer.sendMessage(Text.literal("§e[Claim] §fFlag actualizada."), true);
            rebuild();
            return;
        }
        if (slotIndex == SLOT_VIEW_MEMBERS) {
            // For now display in chat
            viewer.sendMessage(Text.literal("§e=== Miembros de la zona ==="), false);
            if (claim.getMembers().isEmpty()) {
                viewer.sendMessage(Text.literal("§7(sin miembros)"), false);
            } else {
                for (int i = 0; i < claim.getMembers().size(); i++) {
                    String n = i < claim.getMemberNames().size()
                        ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
                    viewer.sendMessage(Text.literal("§7• §f" + n
                        + " §7(usa §f/claim ban " + n + "§7 o §f/claim unban " + n + "§7)"), false);
                }
            }
            return;
        }
        if (slotIndex == SLOT_ADD_MEMBER) {
            requestAddMember(viewer, claim);
            viewer.closeHandledScreen();
            return;
        }
        if (slotIndex == SLOT_CLOSE) {
            viewer.closeHandledScreen();
            return;
        }
        if (slotIndex == SLOT_LIST) {
            viewer.closeHandledScreen();
            viewer.getServer().getCommandManager()
                .executeWithPrefix(viewer.getCommandSource(), "claim list");
            return;
        }
        if (slotIndex == SLOT_DELETE) {
            UUID confirmedFor = deleteConfirm.get(viewer.getUuid());
            boolean shift = action == SlotActionType.QUICK_MOVE;
            if (!shift) {
                viewer.sendMessage(Text.literal("§e⚠️ Usa Shift+Click para confirmar la eliminación."), true);
                return;
            }
            if (confirmedFor == null || !confirmedFor.equals(claim.getClaimId())) {
                deleteConfirm.put(viewer.getUuid(), claim.getClaimId());
                viewer.sendMessage(Text.literal("§e⚠️ Haz clic de nuevo con SHIFT para confirmar la eliminación."), true);
                return;
            }
            // Confirmed - delete the claim and refund the block
            deleteConfirm.remove(viewer.getUuid());
            World world = viewer.getWorld();
            BlockPos centre = claim.getCenter();
            if (world.getBlockState(centre).getBlock() instanceof ClaimBlock) {
                world.breakBlock(centre, true, viewer);
            } else {
                ClaimManager.getInstance().removeClaim(world, centre);
            }
            // ensure refund (in case breakBlock didn't drop)
            Block b = ModBlocks.forTier(claim.getTier());
            if (b != null) {
                ItemStack stack = new ItemStack(b);
                if (!viewer.getInventory().insertStack(stack)) {
                    viewer.dropItem(stack, false);
                }
            }
            viewer.sendMessage(Text.literal("§a✅ Zona eliminada. El bloque fue devuelto a tu inventario."), false);
            viewer.closeHandledScreen();
            return;
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    /* ----------------------------------------------------- entry/factory API */

    public static void open(ServerPlayerEntity player, Claim claim) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, p) -> new ClaimMenuHandler(syncId, pInv, claim),
            Text.literal("📦 Administrar Zona — Tier " + claim.getTier() + " de " + claim.getOwnerName())
        ));
    }

    public static void requestAddMember(ServerPlayerEntity player, Claim claim) {
        pendingAddMember.put(player.getUuid(), claim);
        player.sendMessage(Text.literal("§eEscribe el nombre del jugador a añadir (o §c'cancelar'§e):"), false);
    }

    public static void registerScreenHandler() {
        // GENERIC_9X6 is the vanilla type - no custom registration required.
    }

    public static void registerChatListener() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID id = sender.getUuid();
            Claim c = pendingAddMember.get(id);
            if (c == null) return true;
            String text = message.getContent().getString().trim();
            pendingAddMember.remove(id);
            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")
                || text.startsWith("/")) {
                sender.sendMessage(Text.literal("§7Operación cancelada."), false);
                return false;
            }
            PlayerManager pm = sender.getServer().getPlayerManager();
            ServerPlayerEntity target = pm.getPlayer(text);
            if (target == null) {
                sender.sendMessage(Text.literal("§c❌ Jugador '" + text + "' no está en línea."), false);
                return false;
            }
            if (c.isOwner(target.getUuid())) {
                sender.sendMessage(Text.literal("§c❌ Ese jugador ya es el dueño."), false);
                return false;
            }
            c.addMember(target.getUuid(), target.getName().getString());
            ClaimManager.getInstance().save();
            sender.sendMessage(Text.literal("§a✅ §b" + target.getName().getString()
                + " §aañadido como miembro."), false);
            target.sendMessage(Text.literal("§a[Claim] §fHas sido añadido como miembro de la zona de §b"
                + sender.getName().getString()), false);
            return false; // don't broadcast
        });
    }
}
