package com.claimblocks.gui;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimFlags.FlagId;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owner-only management GUI rendered as a vanilla 9x6 chest. Now hosts
 * 16 flags split across 2 pages, with prev/next page buttons in the
 * bottom row and pure-ASCII text strings (no emojis) per the v3 spec.
 *
 * Slot map (constant across pages):
 *   row 0: title at slot 4
 *   row 1: info at slots 11, 13, 15, 17
 *   row 2-3: 9 flags on page 0, 7 flags on page 1
 *   row 4: members button (slot 38), add-member (slot 42)
 *   row 5: 45=prev, 46=delete, 49=close, 52=list, 53=next
 */
public class ClaimMenuHandler extends ScreenHandler {
    public static final int SIZE = 54;

    private static final int SLOT_TITLE     = 4;
    private static final int SLOT_COORDS    = 11;
    private static final int SLOT_OWNER     = 13;
    private static final int SLOT_TIER      = 15;
    private static final int SLOT_WORLD     = 17;
    private static final int SLOT_VIEW_MEMBERS = 38;
    private static final int SLOT_ADD_MEMBER   = 42;
    private static final int SLOT_PREV       = 45;
    private static final int SLOT_DELETE     = 46;
    private static final int SLOT_CLOSE      = 49;
    private static final int SLOT_LIST       = 52;
    private static final int SLOT_NEXT       = 53;

    /** Slots used to display flags on each page. Page 0 has 9, page 1 has 7. */
    private static final int[] FLAG_SLOTS_P0 = {19, 20, 21, 22, 23, 24, 25, 28, 29};
    private static final int[] FLAG_SLOTS_P1 = {19, 20, 21, 22, 23, 24, 25};

    private static final FlagId[] PAGE_0 = {
        FlagId.BUILDING, FlagId.BREAKING, FlagId.EXPLOSIONS, FlagId.FIRE,
        FlagId.MOB_SPAWN, FlagId.PVP, FlagId.MOB_DAMAGE, FlagId.ALERTS,
        FlagId.PUBLIC_MODE
    };
    private static final FlagId[] PAGE_1 = {
        FlagId.ITEM_USE, FlagId.ENTITY_INTERACT, FlagId.TRAMPLING, FlagId.FLUIDS,
        FlagId.PVP_ALL, FlagId.TREE_CHOPPING, FlagId.SHOW_WELCOME
    };

    private static final Map<FlagId, String> FLAG_NAMES = new LinkedHashMap<>();
    private static final Map<FlagId, String> FLAG_DESCRIPTIONS = new LinkedHashMap<>();
    static {
        // names limited to <= 25 chars (we append " [ON]"/" [OFF]" later, keeping <= 30)
        FLAG_NAMES.put(FlagId.BUILDING,        "Bloquear construccion");
        FLAG_NAMES.put(FlagId.BREAKING,        "Bloquear destruccion");
        FLAG_NAMES.put(FlagId.EXPLOSIONS,      "Bloquear explosiones");
        FLAG_NAMES.put(FlagId.FIRE,            "Bloquear fuego");
        FLAG_NAMES.put(FlagId.MOB_SPAWN,       "Bloquear spawn mobs");
        FLAG_NAMES.put(FlagId.PVP,             "PVP entre jugadores");
        FLAG_NAMES.put(FlagId.MOB_DAMAGE,      "Bloquear dano de mobs");
        FLAG_NAMES.put(FlagId.ALERTS,          "Alertas de intrusos");
        FLAG_NAMES.put(FlagId.PUBLIC_MODE,     "Modo publico (visita)");
        FLAG_NAMES.put(FlagId.ITEM_USE,        "Bloquear uso de items");
        FLAG_NAMES.put(FlagId.ENTITY_INTERACT, "Interac. entidades");
        FLAG_NAMES.put(FlagId.TRAMPLING,       "Pisar cultivos");
        FLAG_NAMES.put(FlagId.FLUIDS,          "Bloquear fluidos");
        FLAG_NAMES.put(FlagId.PVP_ALL,         "PVP contra todos");
        FLAG_NAMES.put(FlagId.TREE_CHOPPING,   "Talar arboles");
        FLAG_NAMES.put(FlagId.SHOW_WELCOME,    "Bienvenida personaliz.");

        // each description <= 35 chars so the lore line fits the rule
        FLAG_DESCRIPTIONS.put(FlagId.BUILDING,        "Intrusos no colocan bloques");
        FLAG_DESCRIPTIONS.put(FlagId.BREAKING,        "Intrusos no rompen bloques");
        FLAG_DESCRIPTIONS.put(FlagId.EXPLOSIONS,      "TNT/creepers no destruyen");
        FLAG_DESCRIPTIONS.put(FlagId.FIRE,            "El fuego se apaga aqui");
        FLAG_DESCRIPTIONS.put(FlagId.MOB_SPAWN,       "No spawnean mobs hostiles");
        FLAG_DESCRIPTIONS.put(FlagId.PVP,             "Jugadores no se atacan");
        FLAG_DESCRIPTIONS.put(FlagId.MOB_DAMAGE,      "Mobs no danan jugadores");
        FLAG_DESCRIPTIONS.put(FlagId.ALERTS,          "Avisar al entrar intrusos");
        FLAG_DESCRIPTIONS.put(FlagId.PUBLIC_MODE,     "Visitantes no modifican");
        FLAG_DESCRIPTIONS.put(FlagId.ITEM_USE,        "Intrusos no usan items");
        FLAG_DESCRIPTIONS.put(FlagId.ENTITY_INTERACT, "Intrusos no usan mobs");
        FLAG_DESCRIPTIONS.put(FlagId.TRAMPLING,       "Intrusos no destruyen tierra");
        FLAG_DESCRIPTIONS.put(FlagId.FLUIDS,          "No se coloca agua/lava");
        FLAG_DESCRIPTIONS.put(FlagId.PVP_ALL,         "Cualquiera puede atacar");
        FLAG_DESCRIPTIONS.put(FlagId.TREE_CHOPPING,   "Intrusos no talan logs");
        FLAG_DESCRIPTIONS.put(FlagId.SHOW_WELCOME,    "Muestra tu mensaje al entrar");
    }

    /** Players awaiting a chat reply for "add member" or "edit welcome". */
    public enum PendingType { ADD_MEMBER, EDIT_WELCOME }
    public record PendingChat(PendingType type, UUID claimId, int returnPage) {}
    private static final Map<UUID, PendingChat> pending = new HashMap<>();

    /** Players who have shift-clicked delete once (need 2nd to confirm). */
    private static final Map<UUID, UUID> deleteConfirm = new HashMap<>();

    private final SimpleInventory chest = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final Claim claim;
    private final ServerPlayerEntity viewer;
    private final int page;

    public ClaimMenuHandler(int syncId, PlayerInventory pInv, Claim claim, int page) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.claim = claim;
        this.viewer = (ServerPlayerEntity) pInv.player;
        this.page = page;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                final int idx = col + row * 9;
                addSlot(new Slot(chest, idx, 8 + col * 18, 18 + row * 18) {
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

    public Claim getClaim() { return claim; }
    public int getPage() { return page; }

    private void rebuild() {
        chest.clear();
        ItemStack bg = withName(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) chest.setStack(i, bg.copy());

        chest.setStack(SLOT_TITLE, withName(
            new ItemStack(Items.PAPER),
            Text.literal(truncate("Zona " + claim.sizeLabel()
                + " - " + claim.getOwnerName(), 30))
        ));

        chest.setStack(SLOT_COORDS, withLore(
            withName(new ItemStack(Items.MAP),
                Text.literal("Coordenadas")),
            List.of(Text.literal("X=" + claim.getX()
                    + " Y=" + claim.getY() + " Z=" + claim.getZ()))
        ));
        chest.setStack(SLOT_OWNER, withLore(
            withName(new ItemStack(Items.WRITTEN_BOOK), Text.literal("Dueno")),
            List.of(Text.literal(truncate(claim.getOwnerName(), 35)))
        ));
        chest.setStack(SLOT_TIER, withLore(
            withName(new ItemStack(Items.NETHER_STAR),
                Text.literal("Zona " + claim.sizeLabel())),
            List.of(Text.literal(truncate(
                "Zona " + claim.sizeLabel() + " bloques", 35)),
                Text.literal(truncate(
                    "Altura: +/-" + claim.getHeight(), 35)))
        ));
        chest.setStack(SLOT_WORLD, withLore(
            withName(new ItemStack(Items.CLOCK), Text.literal("Mundo")),
            List.of(Text.literal(truncate(claim.getWorld(), 35)))
        ));

        // Flags page
        ClaimFlags f = claim.getFlags();
        FlagId[] ids = page == 0 ? PAGE_0 : PAGE_1;
        int[] slots  = page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        for (int i = 0; i < ids.length; i++) {
            chest.setStack(slots[i], flagButton(ids[i], f.get(ids[i])));
        }

        // Members
        chest.setStack(SLOT_VIEW_MEMBERS, withLore(
            withName(new ItemStack(Items.PLAYER_HEAD),
                Text.literal(truncate("Miembros (" + claim.getMembers().size() + ")", 30))),
            buildMemberLore()
        ));
        chest.setStack(SLOT_ADD_MEMBER, withLore(
            withName(new ItemStack(Items.WRITABLE_BOOK), Text.literal("Anadir miembro")),
            List.of(Text.literal("Pide nombre por chat"),
                    Text.literal("Clic para anadir"))
        ));

        // Bottom row
        if (page > 0) {
            chest.setStack(SLOT_PREV, withName(new ItemStack(Items.ARROW),
                Text.literal("< Pagina anterior")));
        }
        chest.setStack(SLOT_DELETE, withLore(
            withName(new ItemStack(Items.BARRIER), Text.literal("ELIMINAR ZONA")),
            List.of(Text.literal("Shift+Click para confirmar"),
                    Text.literal("Devuelve la piedra al inv."))
        ));
        chest.setStack(SLOT_CLOSE, withName(new ItemStack(Items.OAK_DOOR),
            Text.literal("Cerrar")));
        chest.setStack(SLOT_LIST, withName(new ItemStack(Items.MAP),
            Text.literal("Ver lista de zonas")));
        if (page == 0) {
            chest.setStack(SLOT_NEXT, withName(new ItemStack(Items.ARROW),
                Text.literal("Pagina siguiente >")));
        }

        sendContentUpdates();
    }

    private List<Text> buildMemberLore() {
        List<Text> lore = new ArrayList<>();
        if (claim.getMembers().isEmpty()) {
            lore.add(Text.literal("(sin miembros)"));
            return lore;
        }
        int max = Math.min(5, claim.getMembers().size());
        for (int i = 0; i < max; i++) {
            String n = i < claim.getMemberNames().size()
                ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
            lore.add(Text.literal(truncate(" - " + n, 35)));
        }
        if (claim.getMembers().size() > max) {
            lore.add(Text.literal(" - ... y " + (claim.getMembers().size() - max) + " mas"));
        }
        return lore;
    }

    private ItemStack flagButton(FlagId id, boolean enabled) {
        ItemStack stack = new ItemStack(enabled
            ? Items.LIME_STAINED_GLASS
            : Items.RED_STAINED_GLASS);
        String name = FLAG_NAMES.getOrDefault(id, id.name());
        String label = truncate(name + " " + (enabled ? "[ON]" : "[OFF]"), 30);
        String desc = FLAG_DESCRIPTIONS.getOrDefault(id, "");
        String editOrToggle = id == FlagId.SHOW_WELCOME ? "Clic para editar" : "Clic para cambiar";
        return withLore(
            withName(stack, Text.literal(label)),
            List.of(
                Text.literal(truncate(desc, 35)),
                Text.literal(truncate(
                    "Estado: " + (enabled ? "[ON]" : "[OFF]") + " - " + editOrToggle, 35))
            )
        );
    }

    private static ItemStack withName(ItemStack stack, Text name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        return stack;
    }

    private static ItemStack withLore(ItemStack stack, List<Text> lore) {
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    /* ------------------------------------------------------------ click handler */

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType action, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= SIZE) {
            if (action == SlotActionType.QUICK_MOVE) return;
            super.onSlotClick(slotIndex, button, action, player);
            return;
        }
        if (slotIndex != SLOT_DELETE) deleteConfirm.remove(viewer.getUuid());

        // Pagination
        if (slotIndex == SLOT_PREV && page > 0) {
            open(viewer, claim, page - 1);
            return;
        }
        if (slotIndex == SLOT_NEXT && page == 0) {
            open(viewer, claim, page + 1);
            return;
        }

        // Flags
        FlagId clicked = slotToFlag(slotIndex);
        if (clicked != null) {
            if (clicked == FlagId.SHOW_WELCOME) {
                // Click on welcome opens edit flow (left=edit, right=toggle)
                if (button == 1) {
                    claim.getFlags().showWelcome = !claim.getFlags().showWelcome;
                    ClaimManager.getInstance().save();
                    rebuild();
                } else {
                    requestEditWelcome(viewer, claim, page);
                    viewer.closeHandledScreen();
                }
                return;
            }
            claim.getFlags().toggle(clicked);
            ClaimManager.getInstance().save();
            rebuild();
            return;
        }

        if (slotIndex == SLOT_VIEW_MEMBERS) {
            viewer.sendMessage(Text.literal("[Claim] Miembros de la zona:"), false);
            if (claim.getMembers().isEmpty()) {
                viewer.sendMessage(Text.literal("  (sin miembros)"), false);
            } else {
                for (int i = 0; i < claim.getMembers().size(); i++) {
                    String n = i < claim.getMemberNames().size()
                        ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
                    viewer.sendMessage(Text.literal("  - " + n), false);
                }
            }
            return;
        }
        if (slotIndex == SLOT_ADD_MEMBER) {
            requestAddMember(viewer, claim, page);
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
            boolean shift = action == SlotActionType.QUICK_MOVE;
            if (!shift) {
                viewer.sendMessage(Text.literal("[!] Shift+Click para confirmar."), true);
                return;
            }
            UUID confirmed = deleteConfirm.get(viewer.getUuid());
            if (confirmed == null || !confirmed.equals(claim.getClaimId())) {
                deleteConfirm.put(viewer.getUuid(), claim.getClaimId());
                viewer.sendMessage(Text.literal("[!] Haz clic de nuevo con SHIFT para confirmar."), true);
                return;
            }
            deleteConfirm.remove(viewer.getUuid());
            World world = viewer.getWorld();
            BlockPos centre = claim.getCenter();
            if (world.getBlockState(centre).getBlock() instanceof ClaimStoneBlock) {
                world.breakBlock(centre, false, viewer);
            }
            ClaimManager.getInstance().removeClaim(world, centre);
            // refund the right tier item
            if (claim.getTierId() != null) {
                Block b = ModBlocks.byId(claim.getTierId());
                if (b != null) {
                    ItemStack stack = new ItemStack(b);
                    if (!viewer.getInventory().insertStack(stack)) viewer.dropItem(stack, false);
                }
            }
            viewer.sendMessage(Text.literal("[OK] Zona eliminada. Item devuelto."), false);
            viewer.closeHandledScreen();
            return;
        }
    }

    private FlagId slotToFlag(int slotIndex) {
        FlagId[] ids = page == 0 ? PAGE_0 : PAGE_1;
        int[] slots  = page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slotIndex) return ids[i];
        }
        return null;
    }

    @Override public ItemStack quickMove(PlayerEntity p, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity p) { return true; }

    /* ------------------------------------------------------ entry / chat flow */

    public static void open(ServerPlayerEntity player, Claim claim, int page) {
        final int p = Math.max(0, Math.min(1, page));
        String title = truncate("Zona " + claim.sizeLabel()
            + " - " + claim.getOwnerName(), 40);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new ClaimMenuHandler(syncId, pInv, claim, p),
            Text.literal(title)
        ));
    }

    public static void requestAddMember(ServerPlayerEntity player, Claim claim, int returnPage) {
        pending.put(player.getUuid(), new PendingChat(PendingType.ADD_MEMBER,
            claim.getClaimId(), returnPage));
        player.sendMessage(Text.literal(
            "[Claim] Escribe el nombre del jugador (o 'cancelar'):"), false);
    }

    public static void requestEditWelcome(ServerPlayerEntity player, Claim claim, int returnPage) {
        pending.put(player.getUuid(), new PendingChat(PendingType.EDIT_WELCOME,
            claim.getClaimId(), returnPage));
        player.sendMessage(Text.literal(
            "[Claim] Escribe tu bienvenida (max 60 chars) o 'cancelar':"), false);
    }

    public static void registerChatListener() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID id = sender.getUuid();
            PendingChat p = pending.get(id);
            if (p == null) return true;
            String text = message.getContent().getString().trim();
            pending.remove(id);
            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")
                || text.startsWith("/")) {
                sender.sendMessage(Text.literal("[Claim] Cancelado."), false);
                return false;
            }
            Claim claim = findClaimById(p.claimId());
            if (claim == null) {
                sender.sendMessage(Text.literal("[x] La zona ya no existe."), false);
                return false;
            }
            switch (p.type()) {
                case ADD_MEMBER -> handleAddMember(sender, claim, text, p.returnPage());
                case EDIT_WELCOME -> handleEditWelcome(sender, claim, text, p.returnPage());
            }
            return false;
        });
    }

    private static void handleAddMember(ServerPlayerEntity sender, Claim claim, String name, int page) {
        PlayerManager pm = sender.getServer().getPlayerManager();
        ServerPlayerEntity target = pm.getPlayer(name);
        if (target == null) {
            sender.sendMessage(Text.literal("[x] " + name + " no esta en linea."), false);
            return;
        }
        if (claim.isOwner(target.getUuid())) {
            sender.sendMessage(Text.literal("[x] Ese jugador ya es el dueno."), false);
            return;
        }
        claim.addMember(target.getUuid(), target.getName().getString());
        ClaimManager.getInstance().save();
        sender.sendMessage(Text.literal("[OK] Jugador agregado como miembro."), false);
        target.sendMessage(Text.literal(
            "[Claim] Eres miembro de la zona de " + sender.getName().getString()), false);
        open(sender, claim, page);
    }

    private static void handleEditWelcome(ServerPlayerEntity sender, Claim claim, String text, int page) {
        if (text.length() > 60) text = text.substring(0, 60);
        claim.getFlags().welcomeMessage = text;
        claim.getFlags().showWelcome = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.sendMessage(Text.literal("[OK] Bienvenida guardada."), false);
        open(sender, claim, page);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }
}
