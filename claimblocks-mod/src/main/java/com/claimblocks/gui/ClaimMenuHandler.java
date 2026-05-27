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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owner-only management GUI rendered as a vanilla 9x6 chest. Hosts up to 19
 * flags split across 2 pages (9 on page 0, 10 on page 1 - the last 3 of which
 * are passive-effect flags only available on paid tiers).
 *
 * Pure-server side handler; the client renders it as a regular chest GUI.
 *
 * Slot map per page:
 *   row 0: title at slot 4
 *   row 1: info at 11, 13, 15, 17
 *   row 2-3: flag buttons (page-specific layout)
 *   row 4: members button (38), add-member (42)
 *   row 5: 45=prev, 46=delete OR confirm-delete, 47=cancel-delete (only when confirming),
 *          49=close, 52=list, 53=next
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
    private static final int SLOT_CANCEL_DEL = 47;
    private static final int SLOT_CLOSE      = 49;
    private static final int SLOT_LIST       = 52;
    private static final int SLOT_NEXT       = 53;

    /** Flag layout per page. Page 0 = 12 flags, page 1 = 14 flags. */
    private static final int[] FLAG_SLOTS_P0 = {18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30};
    private static final int[] FLAG_SLOTS_P1 = {18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32};

    private static final FlagId[] PAGE_0 = {
        FlagId.BUILDING, FlagId.BREAKING, FlagId.EXPLOSIONS, FlagId.FIRE,
        FlagId.MOB_SPAWN, FlagId.PVP, FlagId.MOB_DAMAGE, FlagId.ALERTS,
        FlagId.PUBLIC_MODE,
        FlagId.ANIMAL_KILLING, FlagId.CHEST_ACCESS, FlagId.CROP_HARVEST
    };
    private static final FlagId[] PAGE_1 = {
        FlagId.ITEM_USE, FlagId.ENTITY_INTERACT, FlagId.TRAMPLING, FlagId.FLUIDS,
        FlagId.PVP_ALL, FlagId.TREE_CHOPPING, FlagId.SHOW_WELCOME,
        FlagId.ANVIL_USE, FlagId.ENDER_PEARL, FlagId.SIGN_EDITING,
        FlagId.EFFECT_REGEN, FlagId.EFFECT_RESIST, FlagId.EFFECT_SPEED, FlagId.ALLOW_FLIGHT
    };

    /** Players awaiting a chat reply for "add member" or "edit welcome". */
    public enum PendingType { ADD_MEMBER, EDIT_WELCOME }
    public record PendingChat(PendingType type, UUID claimId, int returnPage) {}
    private static final Map<UUID, PendingChat> pending = new HashMap<>();

    private final SimpleInventory chest = new SimpleInventory(SIZE) {
        @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
    };
    private final Claim claim;
    private final ServerPlayerEntity viewer;
    private final int page;
    /** True between first-click and second-click (or cancel) on the delete button. */
    private boolean awaitingDeleteConfirm = false;

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

    /* -------- rendering ------------------------------------------------- */

    private void rebuild() {
        chest.clear();
        ItemStack bg = withName(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), Text.literal(" "));
        for (int i = 0; i < SIZE; i++) chest.setStack(i, bg.copy());

        chest.setStack(SLOT_TITLE, withName(
            new ItemStack(Items.PAPER),
            Text.literal(truncate("Zona " + claim.sizeLabel()
                + " - " + claim.getOwnerName(), 30))
                .formatted(Formatting.GOLD, Formatting.BOLD)
        ));

        // Info row
        chest.setStack(SLOT_COORDS, withLore(
            withName(new ItemStack(Items.MAP),
                Text.literal("Coordenadas").formatted(Formatting.AQUA)),
            List.of(Text.literal("X=" + claim.getX()
                    + " Y=" + claim.getY() + " Z=" + claim.getZ())
                .formatted(Formatting.WHITE))
        ));
        chest.setStack(SLOT_OWNER, withLore(
            withName(new ItemStack(Items.WRITTEN_BOOK),
                Text.literal("Dueño").formatted(Formatting.AQUA)),
            List.of(Text.literal(truncate(claim.getOwnerName(), 35))
                .formatted(Formatting.WHITE, Formatting.BOLD))
        ));
        chest.setStack(SLOT_TIER, withLore(
            withName(new ItemStack(Items.NETHER_STAR),
                Text.literal("Zona " + claim.sizeLabel()).formatted(Formatting.YELLOW)),
            List.of(
                Text.literal(truncate("Zona " + claim.sizeLabel() + " bloques", 35))
                    .formatted(Formatting.GRAY),
                Text.literal(truncate("Altura: +/-" + claim.getHeight(), 35))
                    .formatted(Formatting.GRAY)
            )
        ));
        chest.setStack(SLOT_WORLD, withLore(
            withName(new ItemStack(Items.CLOCK),
                Text.literal("Mundo").formatted(Formatting.AQUA)),
            List.of(Text.literal(truncate(claim.getWorld(), 35)).formatted(Formatting.GRAY))
        ));

        // Flags page
        ClaimFlags f = claim.getFlags();
        FlagId[] ids = page == 0 ? PAGE_0 : PAGE_1;
        int[] slots  = page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        ClaimTier tier = claim.getTier();
        boolean isPaid = tier != null && tier.isPaid();
        for (int i = 0; i < ids.length; i++) {
            FlagId id = ids[i];
            if (isEffectFlag(id) && !isPaid) {
                chest.setStack(slots[i], lockedEffectButton(id));
            } else {
                chest.setStack(slots[i], flagButton(id, f.get(id)));
            }
        }

        // Members
        chest.setStack(SLOT_VIEW_MEMBERS, withLore(
            withName(new ItemStack(Items.PLAYER_HEAD),
                Text.literal("Miembros (" + claim.getMembers().size() + ")")
                    .formatted(Formatting.YELLOW)),
            buildMemberLore()
        ));
        chest.setStack(SLOT_ADD_MEMBER, withLore(
            withName(new ItemStack(Items.WRITABLE_BOOK),
                Text.literal("Añadir miembro").formatted(Formatting.GREEN)),
            List.of(Text.literal("Pide nombre por chat").formatted(Formatting.GRAY),
                    Text.literal("Clic para añadir").formatted(Formatting.GRAY))
        ));

        // Bottom row
        if (page > 0) {
            chest.setStack(SLOT_PREV, withName(new ItemStack(Items.ARROW),
                Text.literal("<< Página anterior").formatted(Formatting.AQUA)));
        }
        if (awaitingDeleteConfirm) {
            chest.setStack(SLOT_DELETE, withLore(
                withName(new ItemStack(Items.TNT),
                    Text.literal("Confirmar eliminación").formatted(Formatting.RED, Formatting.BOLD)),
                List.of(Text.literal("Haz clic de nuevo para confirmar")
                            .formatted(Formatting.YELLOW),
                        Text.literal("O mueve el cursor fuera para cancelar")
                            .formatted(Formatting.GRAY))
            ));
            chest.setStack(SLOT_CANCEL_DEL, withLore(
                withName(new ItemStack(Items.GREEN_WOOL),
                    Text.literal("Cancelar").formatted(Formatting.GREEN, Formatting.BOLD)),
                List.of(Text.literal("Cancela la eliminación de la zona")
                            .formatted(Formatting.GRAY))
            ));
        } else {
            chest.setStack(SLOT_DELETE, withLore(
                withName(new ItemStack(Items.BARRIER),
                    Text.literal("Eliminar zona").formatted(Formatting.RED, Formatting.BOLD)),
                List.of(Text.literal("Clic para iniciar eliminación")
                            .formatted(Formatting.YELLOW),
                        Text.literal("Devuelve la piedra al inv.")
                            .formatted(Formatting.GRAY))
            ));
        }
        chest.setStack(SLOT_CLOSE, withName(new ItemStack(Items.OAK_DOOR),
            Text.literal("Cerrar").formatted(Formatting.WHITE)));
        chest.setStack(SLOT_LIST, withName(new ItemStack(Items.MAP),
            Text.literal("Ver lista de zonas").formatted(Formatting.AQUA)));
        if (page == 0) {
            chest.setStack(SLOT_NEXT, withName(new ItemStack(Items.ARROW),
                Text.literal("Página siguiente >>").formatted(Formatting.AQUA)));
        }

        sendContentUpdates();
    }

    private List<Text> buildMemberLore() {
        List<Text> lore = new ArrayList<>();
        if (claim.getMembers().isEmpty()) {
            lore.add(Text.literal("(sin miembros)").formatted(Formatting.DARK_GRAY));
            return lore;
        }
        int max = Math.min(5, claim.getMembers().size());
        for (int i = 0; i < max; i++) {
            String n = i < claim.getMemberNames().size()
                ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
            lore.add(Text.literal(truncate(" - " + n, 35)).formatted(Formatting.WHITE));
        }
        if (claim.getMembers().size() > max) {
            lore.add(Text.literal(" - ... y " + (claim.getMembers().size() - max) + " más")
                .formatted(Formatting.GRAY));
        }
        return lore;
    }

    private static boolean isEffectFlag(FlagId id) {
        return ClaimFlags.isPaidOnly(id);
    }

    private ItemStack lockedEffectButton(FlagId id) {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS);
        return withLore(
            withName(stack, Text.literal(effectName(id) + " [LOCKED]")
                .formatted(Formatting.DARK_GRAY)),
            List.of(
                Text.literal("Solo disponible en zonas de pago").formatted(Formatting.GRAY),
                Text.literal("Tiers 250x250, 300x300 o 500x500").formatted(Formatting.DARK_GRAY)
            )
        );
    }

    private static String effectName(FlagId id) {
        return switch (id) {
            case EFFECT_REGEN  -> "Regeneración pasiva";
            case EFFECT_RESIST -> "Resistencia pasiva";
            case EFFECT_SPEED  -> "Velocidad pasiva";
            case ALLOW_FLIGHT  -> "Vuelo en zona";
            default -> "Perk pasivo";
        };
    }

    /** Builds the visual button for a normal flag with restored colors. */
    private ItemStack flagButton(FlagId id, boolean enabled) {
        ItemStack stack = new ItemStack(enabled
            ? Items.LIME_STAINED_GLASS_PANE
            : Items.RED_STAINED_GLASS_PANE);
        Text name = Text.literal(flagDisplayName(id, enabled))
            .formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD);
        String[] lore = flagLore(id);
        return withLore(
            withName(stack, name),
            List.of(
                Text.literal(lore[0]).formatted(Formatting.GRAY),
                Text.literal("Estado: " + (enabled ? "ACTIVO" : "INACTIVO") + " - " + lore[1])
                    .formatted(Formatting.GRAY)
            )
        );
    }

    /** Per-flag display name (varies by ON/OFF state per spec). */
    private static String flagDisplayName(FlagId id, boolean on) {
        return switch (id) {
            case BUILDING        -> on ? "Construir: BLOQUEADO [ON]"     : "Construir: permitido [OFF]";
            case BREAKING        -> on ? "Romper: BLOQUEADO [ON]"        : "Romper: permitido [OFF]";
            case EXPLOSIONS      -> on ? "Explosiones: BLOQUEADAS [ON]"  : "Explosiones: permitidas [OFF]";
            case FIRE            -> on ? "Fuego: BLOQUEADO [ON]"         : "Fuego: permitido [OFF]";
            case MOB_SPAWN       -> on ? "Mobs hostiles: BLOQUEADOS [ON]": "Mobs hostiles: permit. [OFF]";
            case PVP             -> on ? "PVP: BLOQUEADO [ON]"           : "PVP: permitido [OFF]";
            case MOB_DAMAGE      -> on ? "Daño de mobs: BLOQUEADO [ON]"  : "Daño de mobs: permit. [OFF]";
            case ALERTS          -> on ? "Alertas intrusos: ON [ON]"     : "Alertas intrusos: OFF [OFF]";
            case ITEM_USE        -> on ? "Usar items: BLOQUEADO [ON]"    : "Usar items: permitido [OFF]";
            case ENTITY_INTERACT -> on ? "Entidades: BLOQUEADAS [ON]"    : "Entidades: libres [OFF]";
            case TRAMPLING       -> on ? "Cultivos: PROTEGIDOS [ON]"     : "Cultivos: sin protec. [OFF]";
            case FLUIDS          -> on ? "Fluidos: BLOQUEADOS [ON]"      : "Fluidos: permitidos [OFF]";
            case PVP_ALL         -> on ? "Zona PVP libre: ACTIVA [ON]"   : "Zona PVP libre: inact. [OFF]";
            case TREE_CHOPPING   -> on ? "Árboles: PROTEGIDOS [ON]"      : "Árboles: se talan [OFF]";
            case PUBLIC_MODE     -> on ? "Modo visita: ACTIVO [ON]"      : "Modo visita: inactivo [OFF]";
            case SHOW_WELCOME    -> on ? "Bienvenida custom: ON [ON]"    : "Bienvenida custom: OFF [OFF]";
            case EFFECT_REGEN    -> on ? "Regeneración pasiva [ON]"      : "Regeneración pasiva [OFF]";
            case EFFECT_RESIST   -> on ? "Resistencia pasiva [ON]"       : "Resistencia pasiva [OFF]";
            case EFFECT_SPEED    -> on ? "Velocidad pasiva [ON]"         : "Velocidad pasiva [OFF]";
            case ALLOW_FLIGHT    -> on ? "Vuelo en zona: ACTIVO [ON]"     : "Vuelo en zona: inactivo [OFF]";
            case ANIMAL_KILLING  -> on ? "Animales: PROTEGIDOS [ON]"      : "Animales: se matan [OFF]";
            case CHEST_ACCESS    -> on ? "Cofres: BLOQUEADOS [ON]"        : "Cofres: acceso libre [OFF]";
            case CROP_HARVEST    -> on ? "Cosecha: PROTEGIDA [ON]"        : "Cosecha: libre [OFF]";
            case ANVIL_USE       -> on ? "Yunques: BLOQUEADOS [ON]"       : "Yunques: uso libre [OFF]";
            case ENDER_PEARL     -> on ? "Ender pearl: BLOQUEADA [ON]"    : "Ender pearl: permitida [OFF]";
            case SIGN_EDITING    -> on ? "Letreros: BLOQUEADOS [ON]"      : "Letreros: editables [OFF]";
        };
    }

    /** Returns {description, action-hint}. */
    private static String[] flagLore(FlagId id) {
        return switch (id) {
            case BUILDING        -> new String[]{ "Intrusos no pueden colocar bloques", "Clic para cambiar" };
            case BREAKING        -> new String[]{ "Intrusos no pueden romper nada",     "Clic para cambiar" };
            case EXPLOSIONS      -> new String[]{ "TNT y creepers no destruyen",        "Clic para cambiar" };
            case FIRE            -> new String[]{ "El fuego no se propaga aquí",        "Clic para cambiar" };
            case MOB_SPAWN       -> new String[]{ "Zombies, skeletons no spawnean",     "Clic para cambiar" };
            case PVP             -> new String[]{ "Jugadores no pueden atacarse",       "Clic para cambiar" };
            case MOB_DAMAGE      -> new String[]{ "Los mobs no dañan a jugadores",      "Clic para cambiar" };
            case ALERTS          -> new String[]{ "Avisa al dueño cuando entran",       "Clic para cambiar" };
            case ITEM_USE        -> new String[]{ "Intrusos no pueden usar items",      "Clic para cambiar" };
            case ENTITY_INTERACT -> new String[]{ "Intrusos no usan mobs/aldeanos",     "Clic para cambiar" };
            case TRAMPLING       -> new String[]{ "Intrusos no destruyen la tierra",    "Clic para cambiar" };
            case FLUIDS          -> new String[]{ "Nadie coloca agua ni lava aquí",     "Clic para cambiar" };
            case PVP_ALL         -> new String[]{ "Todos se pueden atacar aquí",        "Clic para cambiar" };
            case TREE_CHOPPING   -> new String[]{ "Intrusos no pueden talar árboles",   "Clic para cambiar" };
            case PUBLIC_MODE     -> new String[]{ "Todos entran pero no modifican",     "Clic para cambiar" };
            case SHOW_WELCOME    -> new String[]{ "Mensaje personalizado al entrar",    "Clic para editar"  };
            case EFFECT_REGEN    -> new String[]{ "Regenera vida a dueño y miembros",   "Clic para cambiar" };
            case EFFECT_RESIST   -> new String[]{ "Reduce daño a dueño y miembros",     "Clic para cambiar" };
            case EFFECT_SPEED    -> new String[]{ "Da velocidad a dueño y miembros",    "Clic para cambiar" };
            case ALLOW_FLIGHT    -> new String[]{ "Dueño y miembros pueden volar",      "Clic para cambiar" };
            case ANIMAL_KILLING  -> new String[]{ "Intrusos no pueden matar animales",  "Clic para cambiar" };
            case CHEST_ACCESS    -> new String[]{ "Intrusos no abren cofres ni barriles","Clic para cambiar" };
            case CROP_HARVEST    -> new String[]{ "Intrusos no cosechan cultivos",      "Clic para cambiar" };
            case ANVIL_USE       -> new String[]{ "Intrusos no pueden usar yunques",    "Clic para cambiar" };
            case ENDER_PEARL     -> new String[]{ "Intrusos no se teletransportan",     "Clic para cambiar" };
            case SIGN_EDITING    -> new String[]{ "Intrusos no editan letreros",        "Clic para cambiar" };
        };
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

        // Pagination
        if (slotIndex == SLOT_PREV && page > 0) {
            open(viewer, claim, page - 1);
            return;
        }
        if (slotIndex == SLOT_NEXT && page == 0) {
            open(viewer, claim, page + 1);
            return;
        }

        // Delete flow
        if (slotIndex == SLOT_DELETE) {
            if (!awaitingDeleteConfirm) {
                awaitingDeleteConfirm = true;
                rebuild();
                viewer.sendMessage(Text.literal("[!] Haz clic de nuevo para confirmar.")
                    .formatted(Formatting.YELLOW), true);
                return;
            }
            // Confirmed
            performDelete();
            return;
        }
        if (slotIndex == SLOT_CANCEL_DEL && awaitingDeleteConfirm) {
            awaitingDeleteConfirm = false;
            rebuild();
            viewer.sendMessage(Text.literal("[i] Eliminación cancelada.")
                .formatted(Formatting.AQUA), true);
            return;
        }

        // Reset delete flow if we click anything else
        if (awaitingDeleteConfirm) {
            awaitingDeleteConfirm = false;
            // fall through to handle the actual click
        }

        // Flags
        FlagId clicked = slotToFlag(slotIndex);
        if (clicked != null) {
            // Locked effect flag for non-paid claims
            if (isEffectFlag(clicked)) {
                ClaimTier tier = claim.getTier();
                if (tier == null || !tier.isPaid()) {
                    viewer.sendMessage(Text.literal("[x] Solo disponible en zonas de pago.")
                        .formatted(Formatting.RED), true);
                    return;
                }
            }
            if (clicked == FlagId.SHOW_WELCOME) {
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
            viewer.sendMessage(Text.literal("[Claim] Miembros de la zona:")
                .formatted(Formatting.GRAY), false);
            if (claim.getMembers().isEmpty()) {
                viewer.sendMessage(Text.literal("  (sin miembros)").formatted(Formatting.DARK_GRAY), false);
            } else {
                for (int i = 0; i < claim.getMembers().size(); i++) {
                    String n = i < claim.getMemberNames().size()
                        ? claim.getMemberNames().get(i) : claim.getMembers().get(i).toString();
                    viewer.sendMessage(Text.literal("  - " + n).formatted(Formatting.WHITE), false);
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
    }

    private void performDelete() {
        World world = viewer.getWorld();
        BlockPos centre = claim.getCenter();
        if (world.getBlockState(centre).getBlock() instanceof ClaimStoneBlock) {
            world.breakBlock(centre, false, viewer);
        }
        ClaimManager.getInstance().removeClaim(world, centre);
        if (claim.getTierId() != null) {
            Block b = ModBlocks.byId(claim.getTierId());
            if (b != null) {
                ItemStack stack = new ItemStack(b);
                if (!viewer.getInventory().insertStack(stack)) viewer.dropItem(stack, false);
            }
        }
        viewer.sendMessage(Text.literal("✔ Zona eliminada. Piedra devuelta a tu inventario.")
            .formatted(Formatting.GREEN), false);
        viewer.closeHandledScreen();
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
        open(player, claim, page, null);
    }

    /** Opens the menu with a custom title (used by /claimadmin to show "[Admin]..."). */
    public static void open(ServerPlayerEntity player, Claim claim, int page, String customTitle) {
        final int p = Math.max(0, Math.min(1, page));
        String title = customTitle != null
            ? truncate(customTitle, 40)
            : truncate("Zona " + claim.sizeLabel() + " - " + claim.getOwnerName(), 40);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, pInv, plr) -> new ClaimMenuHandler(syncId, pInv, claim, p),
            Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD)
        ));
    }

    public static void requestAddMember(ServerPlayerEntity player, Claim claim, int returnPage) {
        pending.put(player.getUuid(), new PendingChat(PendingType.ADD_MEMBER,
            claim.getClaimId(), returnPage));
        player.sendMessage(Text.literal("[Claim] Escribe el nombre del jugador (o 'cancelar'):")
            .formatted(Formatting.YELLOW), false);
    }

    public static void requestEditWelcome(ServerPlayerEntity player, Claim claim, int returnPage) {
        pending.put(player.getUuid(), new PendingChat(PendingType.EDIT_WELCOME,
            claim.getClaimId(), returnPage));
        player.sendMessage(Text.literal("[Claim] Escribe tu bienvenida (max 60 chars) o 'cancelar':")
            .formatted(Formatting.YELLOW), false);
    }

    public static void registerChatListener() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID id = sender.getUuid();

            // Admin transfer takes priority
            if (com.claimblocks.gui.AdminClaimSubMenuHandler.hasPendingTransfer(id)) {
                String text = message.getContent().getString().trim();
                UUID claimId = com.claimblocks.gui.AdminClaimSubMenuHandler.popPendingTransfer(id);
                if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")
                    || text.startsWith("/")) {
                    sender.sendMessage(Text.literal("[Claim] Cancelado.").formatted(Formatting.GRAY), false);
                    return false;
                }
                handleAdminTransfer(sender, claimId, text);
                return false;
            }

            PendingChat p = pending.get(id);
            if (p == null) return true;
            String text = message.getContent().getString().trim();
            pending.remove(id);
            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel")
                || text.startsWith("/")) {
                sender.sendMessage(Text.literal("[Claim] Cancelado.").formatted(Formatting.GRAY), false);
                return false;
            }
            Claim claim = findClaimById(p.claimId());
            if (claim == null) {
                sender.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
                return false;
            }
            switch (p.type()) {
                case ADD_MEMBER   -> handleAddMember(sender, claim, text, p.returnPage());
                case EDIT_WELCOME -> handleEditWelcome(sender, claim, text, p.returnPage());
            }
            return false;
        });
    }

    private static void handleAdminTransfer(ServerPlayerEntity op, UUID claimId, String name) {
        Claim claim = findClaimById(claimId);
        if (claim == null) {
            op.sendMessage(Text.literal("[x] La zona ya no existe.").formatted(Formatting.RED), false);
            return;
        }
        // Find the new owner (online or via UserCache for offline)
        ServerPlayerEntity online = op.getServer().getPlayerManager().getPlayer(name);
        UUID newOwnerId;
        String newOwnerName;
        if (online != null) {
            newOwnerId = online.getUuid();
            newOwnerName = online.getName().getString();
        } else {
            var profile = op.getServer().getUserCache().findByName(name);
            if (profile.isEmpty()) {
                op.sendMessage(Text.literal("[x] Jugador no encontrado.").formatted(Formatting.RED), false);
                return;
            }
            newOwnerId = profile.get().getId();
            newOwnerName = profile.get().getName();
        }

        // Transfer
        claim.setOwner(newOwnerId, newOwnerName);
        claim.getMembers().clear();
        claim.getMemberNames().clear();
        ClaimManager.getInstance().save();

        op.sendMessage(Text.literal("✔ Zona transferida a " + newOwnerName + ".")
            .formatted(Formatting.GREEN), false);
        Text msg = Text.literal("[!] Un administrador te transfirió una zona ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(claim.sizeLabel()).formatted(Formatting.WHITE, Formatting.BOLD))
            .append(Text.literal(" en X:" + claim.getX() + " Z:" + claim.getZ()).formatted(Formatting.YELLOW));
        if (online != null) {
            online.sendMessage(msg, false);
        } else {
            ClaimManager.getInstance().queueMessage(newOwnerId, msg);
        }
    }

    private static void handleAddMember(ServerPlayerEntity sender, Claim claim, String name, int page) {
        PlayerManager pm = sender.getServer().getPlayerManager();
        ServerPlayerEntity target = pm.getPlayer(name);
        if (target == null) {
            sender.sendMessage(Text.literal("[x] " + name + " no está en línea.")
                .formatted(Formatting.RED), false);
            return;
        }
        if (claim.isOwner(target.getUuid())) {
            sender.sendMessage(Text.literal("[x] Ese jugador ya es el dueño.")
                .formatted(Formatting.RED), false);
            return;
        }
        claim.addMember(target.getUuid(), target.getName().getString());
        ClaimManager.getInstance().save();
        sender.sendMessage(Text.literal("✔ Jugador agregado como miembro.")
            .formatted(Formatting.GREEN), false);
        target.sendMessage(Text.literal(
            "[Claim] Eres miembro de la zona de " + sender.getName().getString())
            .formatted(Formatting.AQUA), false);
        open(sender, claim, page);
    }

    private static void handleEditWelcome(ServerPlayerEntity sender, Claim claim, String text, int page) {
        if (text.length() > 60) text = text.substring(0, 60);
        claim.getFlags().welcomeMessage = text;
        claim.getFlags().showWelcome = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.sendMessage(Text.literal("✔ Bienvenida guardada.").formatted(Formatting.GREEN), false);
        open(sender, claim, page);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }
}
