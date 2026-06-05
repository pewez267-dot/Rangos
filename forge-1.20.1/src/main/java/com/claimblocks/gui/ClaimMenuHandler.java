package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.network.NetworkHooks;

public class ClaimMenuHandler extends ChestMenu {
    public static final int SIZE = 54;
    private static final int[] FLAG_SLOTS_P0 = {18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30};
    private static final int[] FLAG_SLOTS_P1 = {18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33};
    private static final ClaimFlags.FlagId[] PAGE_0 = {
        ClaimFlags.FlagId.BUILDING, ClaimFlags.FlagId.BREAKING, ClaimFlags.FlagId.EXPLOSIONS, ClaimFlags.FlagId.FIRE,
        ClaimFlags.FlagId.MOB_SPAWN, ClaimFlags.FlagId.PVP, ClaimFlags.FlagId.MOB_DAMAGE, ClaimFlags.FlagId.ALERTS,
        ClaimFlags.FlagId.PUBLIC_MODE, ClaimFlags.FlagId.ANIMAL_KILLING, ClaimFlags.FlagId.CHEST_ACCESS, ClaimFlags.FlagId.CROP_HARVEST
    };
    private static final ClaimFlags.FlagId[] PAGE_1 = {
        ClaimFlags.FlagId.ITEM_USE, ClaimFlags.FlagId.ENTITY_INTERACT, ClaimFlags.FlagId.TRAMPLING, ClaimFlags.FlagId.FLUIDS,
        ClaimFlags.FlagId.PVP_ALL, ClaimFlags.FlagId.TREE_CHOPPING, ClaimFlags.FlagId.SHOW_WELCOME, ClaimFlags.FlagId.ANVIL_USE,
        ClaimFlags.FlagId.ENDER_PEARL, ClaimFlags.FlagId.SIGN_EDITING, ClaimFlags.FlagId.DOORS_ACCESS,
        ClaimFlags.FlagId.EFFECT_REGEN, ClaimFlags.FlagId.EFFECT_RESIST, ClaimFlags.FlagId.EFFECT_SPEED, ClaimFlags.FlagId.ALLOW_FLIGHT
    };
    private static final Map<UUID, PendingChat> pending = new ConcurrentHashMap<>();

    private final SimpleContainer chest;
    private final Claim claim;
    private final ServerPlayer viewer;
    private final int page;
    private boolean awaitingDeleteConfirm = false;

    public ClaimMenuHandler(int syncId, Inventory pInv, Claim claim, int page) {
        this(syncId, pInv, new SimpleContainer(54), claim, page);
    }

    private ClaimMenuHandler(int syncId, Inventory pInv, SimpleContainer chest, Claim claim, int page) {
        super(MenuType.GENERIC_9x6, syncId, pInv, chest, 6);
        this.chest = chest;
        this.claim = claim;
        this.viewer = (ServerPlayer) pInv.player;
        this.page = page;
        this.rebuild();
    }

    public Claim getClaim() { return this.claim; }
    public int getPage() { return this.page; }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    private void rebuild() {
        ItemStack bg = withName(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Component.literal(" "));
        for (int i = 0; i < 54; ++i) this.chest.setItem(i, bg.copy());

        this.chest.setItem(4, withName(new ItemStack(Items.PAPER),
            Component.literal(truncate("Zona " + this.claim.sizeLabel() + " - " + this.claim.getOwnerName(), 30)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        this.chest.setItem(11, withLore(withName(new ItemStack(Items.COMPASS), Component.literal("Coordenadas").withStyle(ChatFormatting.AQUA)),
            List.of(Component.literal("X=" + this.claim.getX() + " Y=" + this.claim.getY() + " Z=" + this.claim.getZ()).withStyle(ChatFormatting.WHITE))));
        this.chest.setItem(13, withLore(withName(new ItemStack(Items.PLAYER_HEAD), Component.literal("Due\u00f1o").withStyle(ChatFormatting.AQUA)),
            List.of(Component.literal(truncate(this.claim.getOwnerName(), 35)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))));
        this.chest.setItem(15, withLore(withName(new ItemStack(Items.DIAMOND), Component.literal("Zona " + this.claim.sizeLabel()).withStyle(ChatFormatting.YELLOW)),
            List.of(Component.literal("Zona " + this.claim.sizeLabel() + " bloques").withStyle(ChatFormatting.GRAY),
                    Component.literal("Altura: +/-" + this.claim.getHeight()).withStyle(ChatFormatting.GRAY))));
        this.chest.setItem(17, withLore(withName(new ItemStack(Items.MAP), Component.literal("Mundo").withStyle(ChatFormatting.AQUA)),
            List.of(Component.literal(truncate(this.claim.getWorld(), 35)).withStyle(ChatFormatting.GRAY))));

        ClaimFlags f = this.claim.getFlags();
        ClaimFlags.FlagId[] ids = this.page == 0 ? PAGE_0 : PAGE_1;
        int[] slots = this.page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        int tierLevel = paidLevelOf(this.claim.getTier());
        for (int i = 0; i < ids.length; ++i) {
            ClaimFlags.FlagId id = ids[i];
            int reqLevel = requiredPaidLevel(id);
            if (reqLevel > 0 && tierLevel < reqLevel) {
                this.chest.setItem(slots[i], lockedEffectButton(id, reqLevel));
            } else {
                this.chest.setItem(slots[i], flagButton(id, f.get(id)));
            }
        }

        this.chest.setItem(38, withLore(withName(new ItemStack(Items.WRITABLE_BOOK), Component.literal("Miembros (" + this.claim.getMembers().size() + ")").withStyle(ChatFormatting.YELLOW)), buildMemberLore()));
        this.chest.setItem(40, withLore(withName(new ItemStack(Items.NAME_TAG), Component.literal("Quitar miembro").withStyle(ChatFormatting.RED)),
            List.of(Component.literal("Pide nombre por chat").withStyle(ChatFormatting.GRAY), Component.literal("Clic para eliminar a un invitado").withStyle(ChatFormatting.GRAY))));
        this.chest.setItem(42, withLore(withName(new ItemStack(Items.PLAYER_HEAD), Component.literal("A\u00f1adir miembro").withStyle(ChatFormatting.GREEN)),
            List.of(Component.literal("Pide nombre por chat").withStyle(ChatFormatting.GRAY), Component.literal("Clic para a\u00f1adir").withStyle(ChatFormatting.GRAY))));

        if (this.page > 0) {
            this.chest.setItem(45, withName(new ItemStack(Items.ARROW), Component.literal("<< P\u00e1gina anterior").withStyle(ChatFormatting.AQUA)));
        }
        if (this.awaitingDeleteConfirm) {
            this.chest.setItem(46, withLore(withName(new ItemStack(Items.TNT), Component.literal("Confirmar eliminaci\u00f3n").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                List.of(Component.literal("Haz clic de nuevo para confirmar").withStyle(ChatFormatting.YELLOW))));
            this.chest.setItem(47, withLore(withName(new ItemStack(Items.LIME_DYE), Component.literal("Cancelar").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)),
                List.of(Component.literal("Cancela la eliminaci\u00f3n").withStyle(ChatFormatting.GRAY))));
        } else {
            this.chest.setItem(46, withLore(withName(new ItemStack(Items.BARRIER), Component.literal("Eliminar zona").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                List.of(Component.literal("Clic para iniciar eliminaci\u00f3n").withStyle(ChatFormatting.YELLOW), Component.literal("Devuelve la piedra al inv.").withStyle(ChatFormatting.GRAY))));
        }
        this.chest.setItem(49, withName(new ItemStack(Items.RED_DYE), Component.literal("Cerrar").withStyle(ChatFormatting.WHITE)));
        this.chest.setItem(52, withName(new ItemStack(Items.BOOK), Component.literal("Ver lista de zonas").withStyle(ChatFormatting.AQUA)));
        if (this.page == 0) {
            this.chest.setItem(53, withName(new ItemStack(Items.ARROW), Component.literal("P\u00e1gina siguiente >>").withStyle(ChatFormatting.AQUA)));
        }
        this.broadcastChanges();
    }

    private List<Component> buildMemberLore() {
        List<Component> lore = new ArrayList<>();
        if (this.claim.getMembers().isEmpty()) {
            lore.add(Component.literal("(sin miembros)").withStyle(ChatFormatting.DARK_GRAY));
            return lore;
        }
        int max = Math.min(5, this.claim.getMembers().size());
        for (int i = 0; i < max; ++i) {
            String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
            lore.add(Component.literal(truncate(" - " + n, 35)).withStyle(ChatFormatting.WHITE));
        }
        if (this.claim.getMembers().size() > max) {
            lore.add(Component.literal(" - ... y " + (this.claim.getMembers().size() - max) + " m\u00e1s").withStyle(ChatFormatting.GRAY));
        }
        return lore;
    }

    private static int paidLevelOf(ClaimTier t) {
        if (t == null) return 0;
        return switch (t.id) {
            case "claimstone_250x250" -> 1;
            case "claimstone_300x300" -> 2;
            case "claimstone_500x500" -> 3;
            default -> 0;
        };
    }

    private static int requiredPaidLevel(ClaimFlags.FlagId id) {
        return switch (id) {
            case EFFECT_REGEN -> 1;
            case EFFECT_RESIST -> 2;
            case EFFECT_SPEED -> 2;
            case ALLOW_FLIGHT -> 3;
            default -> 0;
        };
    }

    private static String requiredTierLabel(int reqLevel) {
        return switch (reqLevel) {
            case 1 -> "250x250";
            case 2 -> "300x300";
            case 3 -> "500x500";
            default -> "?";
        };
    }

    private ItemStack lockedEffectButton(ClaimFlags.FlagId id, int reqLevel) {
        ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        return withLore(withName(stack, Component.literal(effectName(id) + " [LOCKED]").withStyle(ChatFormatting.DARK_GRAY)),
            List.of(Component.literal("Requiere zona " + requiredTierLabel(reqLevel) + " o superior").withStyle(ChatFormatting.GRAY),
                    Component.literal(effectShortDesc(id)).withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static String effectShortDesc(ClaimFlags.FlagId id) {
        return switch (id) {
            case EFFECT_REGEN -> "Regenera vida a duenio y miembros";
            case EFFECT_RESIST -> "Reduce dano a duenio y miembros";
            case EFFECT_SPEED -> "Da velocidad a duenio y miembros";
            case ALLOW_FLIGHT -> "Solo el duenio puede volar en su zona";
            default -> "Perk pasivo";
        };
    }

    private static String effectName(ClaimFlags.FlagId id) {
        return switch (id) {
            case EFFECT_REGEN -> "Regeneraci\u00f3n pasiva";
            case EFFECT_RESIST -> "Resistencia pasiva";
            case EFFECT_SPEED -> "Velocidad pasiva";
            case ALLOW_FLIGHT -> "Vuelo en zona";
            default -> "Perk pasivo";
        };
    }

    private ItemStack flagButton(ClaimFlags.FlagId id, boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? Items.LIME_DYE : Items.GRAY_DYE);
        MutableComponent name = Component.literal(flagDisplayName(id, enabled)).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD);
        String[] lore = flagLore(id);
        return withLore(withName(stack, name),
            List.of(Component.literal(lore[0]).withStyle(ChatFormatting.GRAY),
                    Component.literal("Estado: " + (enabled ? "ACTIVO" : "INACTIVO") + " - " + lore[1]).withStyle(ChatFormatting.GRAY)));
    }

    private static String flagDisplayName(ClaimFlags.FlagId id, boolean on) {
        return switch (id) {
            case BUILDING -> on ? "Construir: BLOQUEADO [ON]" : "Construir: permitido [OFF]";
            case BREAKING -> on ? "Romper: BLOQUEADO [ON]" : "Romper: permitido [OFF]";
            case EXPLOSIONS -> on ? "Explosiones: BLOQUEADAS [ON]" : "Explosiones: permitidas [OFF]";
            case FIRE -> on ? "Fuego: BLOQUEADO [ON]" : "Fuego: permitido [OFF]";
            case MOB_SPAWN -> on ? "Mobs hostiles: BLOQUEADOS [ON]" : "Mobs hostiles: permit. [OFF]";
            case PVP -> on ? "PVP: BLOQUEADO [ON]" : "PVP: permitido [OFF]";
            case MOB_DAMAGE -> on ? "Da\u00f1o de mobs: BLOQUEADO [ON]" : "Da\u00f1o de mobs: permit. [OFF]";
            case ALERTS -> on ? "Alertas intrusos: ON [ON]" : "Alertas intrusos: OFF [OFF]";
            case ITEM_USE -> on ? "Usar items: BLOQUEADO [ON]" : "Usar items: permitido [OFF]";
            case ENTITY_INTERACT -> on ? "Entidades: BLOQUEADAS [ON]" : "Entidades: libres [OFF]";
            case TRAMPLING -> on ? "Cultivos: PROTEGIDOS [ON]" : "Cultivos: sin protec. [OFF]";
            case FLUIDS -> on ? "Fluidos: BLOQUEADOS [ON]" : "Fluidos: permitidos [OFF]";
            case PVP_ALL -> on ? "Zona PVP libre: ACTIVA [ON]" : "Zona PVP libre: inact. [OFF]";
            case TREE_CHOPPING -> on ? "\u00c1rboles: PROTEGIDOS [ON]" : "\u00c1rboles: se talan [OFF]";
            case PUBLIC_MODE -> on ? "Modo visita: ACTIVO [ON]" : "Modo visita: inactivo [OFF]";
            case SHOW_WELCOME -> on ? "Bienvenida custom: ON [ON]" : "Bienvenida custom: OFF [OFF]";
            case EFFECT_REGEN -> on ? "Regeneraci\u00f3n pasiva [ON]" : "Regeneraci\u00f3n pasiva [OFF]";
            case EFFECT_RESIST -> on ? "Resistencia pasiva [ON]" : "Resistencia pasiva [OFF]";
            case EFFECT_SPEED -> on ? "Velocidad pasiva [ON]" : "Velocidad pasiva [OFF]";
            case ALLOW_FLIGHT -> on ? "Vuelo en zona: ACTIVO [ON]" : "Vuelo en zona: inactivo [OFF]";
            case ANIMAL_KILLING -> on ? "Animales: PROTEGIDOS [ON]" : "Animales: se matan [OFF]";
            case CHEST_ACCESS -> on ? "Cofres: BLOQUEADOS [ON]" : "Cofres: acceso libre [OFF]";
            case CROP_HARVEST -> on ? "Cosecha: PROTEGIDA [ON]" : "Cosecha: libre [OFF]";
            case ANVIL_USE -> on ? "Yunques: BLOQUEADOS [ON]" : "Yunques: uso libre [OFF]";
            case ENDER_PEARL -> on ? "Ender pearl: BLOQUEADA [ON]" : "Ender pearl: permitida [OFF]";
            case SIGN_EDITING -> on ? "Letreros: BLOQUEADOS [ON]" : "Letreros: editables [OFF]";
            case DOORS_ACCESS -> on ? "Puertas/Botones: BLOQ [ON]" : "Puertas/Botones: libres [OFF]";
        };
    }

    private static String[] flagLore(ClaimFlags.FlagId id) {
        String desc = switch (id) {
            case BUILDING -> "Intrusos no pueden colocar bloques";
            case BREAKING -> "Intrusos no pueden romper nada";
            case EXPLOSIONS -> "TNT y creepers no destruyen";
            case FIRE -> "El fuego no se propaga aqu\u00ed";
            case MOB_SPAWN -> "Zombies, skeletons no spawnean";
            case PVP -> "Jugadores no pueden atacarse";
            case MOB_DAMAGE -> "Los mobs no da\u00f1an a jugadores";
            case ALERTS -> "Avisa al due\u00f1o cuando entran";
            case ITEM_USE -> "Intrusos no pueden usar items";
            case ENTITY_INTERACT -> "Intrusos no usan mobs/aldeanos";
            case TRAMPLING -> "Intrusos no destruyen la tierra";
            case FLUIDS -> "Nadie coloca agua ni lava aqu\u00ed";
            case PVP_ALL -> "Todos se pueden atacar aqu\u00ed";
            case TREE_CHOPPING -> "Intrusos no pueden talar \u00e1rboles";
            case PUBLIC_MODE -> "Todos entran pero no modifican";
            case SHOW_WELCOME -> "Mensaje personalizado al entrar";
            case EFFECT_REGEN -> "Regenera vida a due\u00f1o y miembros";
            case EFFECT_RESIST -> "Reduce da\u00f1o a due\u00f1o y miembros";
            case EFFECT_SPEED -> "Da velocidad a due\u00f1o y miembros";
            case ALLOW_FLIGHT -> "Due\u00f1o puede volar";
            case ANIMAL_KILLING -> "Intrusos no pueden matar animales";
            case CHEST_ACCESS -> "Intrusos no abren cofres ni barriles";
            case CROP_HARVEST -> "Intrusos no cosechan cultivos";
            case ANVIL_USE -> "Intrusos no pueden usar yunques";
            case ENDER_PEARL -> "Intrusos no se teletransportan";
            case SIGN_EDITING -> "Intrusos no editan letreros";
            case DOORS_ACCESS -> "Intrusos no usan puertas, botones ni placas";
        };
        String action = id == ClaimFlags.FlagId.SHOW_WELCOME ? "Clic para editar" : "Clic para cambiar";
        return new String[]{desc, action};
    }

    private static ItemStack withName(ItemStack stack, Component name) {
        stack.setHoverName(name);
        return stack;
    }

    private static ItemStack withLore(ItemStack stack, List<Component> lore) {
        ClaimBlocks.setLore(stack, lore);
        return stack;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= 54) return; // bloquea mover items / inventario propio

        if (slotId == 45 && this.page > 0) { open(this.viewer, this.claim, this.page - 1); return; }
        if (slotId == 53 && this.page == 0) { open(this.viewer, this.claim, this.page + 1); return; }
        if (slotId == 46) {
            if (!this.awaitingDeleteConfirm) {
                this.awaitingDeleteConfirm = true;
                this.rebuild();
                this.viewer.displayClientMessage(Component.literal("[!] Haz clic de nuevo para confirmar.").withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            this.performDelete();
            return;
        }
        if (slotId == 47 && this.awaitingDeleteConfirm) {
            this.awaitingDeleteConfirm = false;
            this.rebuild();
            this.viewer.displayClientMessage(Component.literal("[i] Eliminaci\u00f3n cancelada.").withStyle(ChatFormatting.AQUA), true);
            return;
        }
        if (this.awaitingDeleteConfirm) this.awaitingDeleteConfirm = false;

        ClaimFlags.FlagId clicked = slotToFlag(slotId);
        if (clicked != null) {
            int reqLevel = requiredPaidLevel(clicked);
            if (reqLevel > 0) {
                int tierLevel = paidLevelOf(this.claim.getTier());
                if (tierLevel < reqLevel) {
                    this.viewer.displayClientMessage(Component.literal("[x] Requiere zona " + requiredTierLabel(reqLevel) + " o superior.").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
            if (clicked == ClaimFlags.FlagId.SHOW_WELCOME) {
                if (button == 1) {
                    this.claim.getFlags().showWelcome = !this.claim.getFlags().showWelcome;
                    ClaimManager.getInstance().save();
                    this.rebuild();
                } else {
                    requestEditWelcome(this.viewer, this.claim, this.page);
                    this.viewer.closeContainer();
                }
                return;
            }
            this.claim.getFlags().toggle(clicked);
            ClaimManager.getInstance().save();
            this.rebuild();
            return;
        }
        if (slotId == 38) {
            this.viewer.displayClientMessage(Component.literal("[Claim] Miembros de la zona:").withStyle(ChatFormatting.GRAY), false);
            if (this.claim.getMembers().isEmpty()) {
                this.viewer.displayClientMessage(Component.literal("  (sin miembros)").withStyle(ChatFormatting.DARK_GRAY), false);
            } else {
                for (int i = 0; i < this.claim.getMembers().size(); ++i) {
                    String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
                    this.viewer.displayClientMessage(Component.literal("  - " + n).withStyle(ChatFormatting.WHITE), false);
                }
            }
            return;
        }
        if (slotId == 42) {
            requestAddMember(this.viewer, this.claim, this.page);
            this.viewer.closeContainer();
            return;
        }
        if (slotId == 40) {
            if (this.claim.getMembers().isEmpty()) {
                this.viewer.displayClientMessage(Component.literal("[i] Esta zona no tiene miembros que quitar.").withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            requestRemoveMember(this.viewer, this.claim, this.page);
            this.viewer.closeContainer();
            return;
        }
        if (slotId == 49) {
            this.viewer.closeContainer();
            return;
        }
        if (slotId == 52) {
            this.viewer.closeContainer();
            this.viewer.server.getCommands().performPrefixedCommand(this.viewer.createCommandSourceStack(), "claim list");
            return;
        }
    }

    private void performDelete() {
        ClaimTier tier = this.claim.getTier();
        var world = this.viewer.level();
        var centre = this.claim.getCenter();
        if (tier != null && ClaimBlocks.isClaimConcreteForTier(world.getBlockState(centre).getBlock(), tier)) {
            world.destroyBlock(centre, false);
        }
        world.playSound(null, centre, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 2.0f, 1.0f);
        ClaimManager.getInstance().removeClaim(world, centre);
        if (tier != null) {
            ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
            if (!this.viewer.getInventory().add(stack)) this.viewer.drop(stack, false);
        }
        this.viewer.displayClientMessage(Component.literal("\u2714 Zona eliminada. Piedra devuelta a tu inventario.").withStyle(ChatFormatting.GREEN), false);
        this.viewer.closeContainer();
    }

    private ClaimFlags.FlagId slotToFlag(int slotIndex) {
        ClaimFlags.FlagId[] ids = this.page == 0 ? PAGE_0 : PAGE_1;
        int[] slots = this.page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        for (int i = 0; i < slots.length; ++i) {
            if (slots[i] == slotIndex) return ids[i];
        }
        return null;
    }

    // ====================== OPEN ======================
    public static void open(ServerPlayer player, Claim claim, int page) {
        open(player, claim, page, null);
    }

    public static void open(ServerPlayer player, Claim claim, int page, String customTitle) {
        int p = Math.max(0, Math.min(1, page));
        String title = customTitle != null ? truncate(customTitle, 40)
                : truncate("Zona " + claim.sizeLabel() + " - " + claim.getOwnerName(), 40);
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
                return new ClaimMenuHandler(id, inv, claim, p);
            }
        });
    }

    // ====================== CHAT PENDING ======================
    public static void requestAddMember(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.ADD_MEMBER, claim.getClaimId(), returnPage));
        player.displayClientMessage(Component.literal("[Claim] Escribe el nombre del jugador (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestRemoveMember(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.REMOVE_MEMBER, claim.getClaimId(), returnPage));
        StringBuilder sb = new StringBuilder();
        List<String> names = claim.getMemberNames();
        for (int i = 0; i < names.size(); ++i) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        player.displayClientMessage(Component.literal("[Claim] Miembros: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sb.toString()).withStyle(ChatFormatting.WHITE)), false);
        player.displayClientMessage(Component.literal("[Claim] Escribe el nombre del invitado a quitar (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestEditWelcome(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.EDIT_WELCOME, claim.getClaimId(), returnPage));
        player.displayClientMessage(Component.literal("[Claim] Escribe tu bienvenida (max 60 chars) o 'cancelar':").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void handleChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        UUID id = sender.getUUID();

        // Transferencia admin pendiente (desde AdminClaimSubMenuHandler)
        if (AdminClaimSubMenuHandler.hasPendingTransfer(id)) {
            event.setCanceled(true);
            String text = event.getMessage().getString().trim();
            UUID claimId = AdminClaimSubMenuHandler.popPendingTransfer(id);
            sender.server.execute(() -> {
                if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/")) {
                    sender.displayClientMessage(Component.literal("[Claim] Cancelado.").withStyle(ChatFormatting.GRAY), false);
                    return;
                }
                handleAdminTransfer(sender, claimId, text);
            });
            return;
        }

        PendingChat p = pending.get(id);
        if (p == null) return;
        event.setCanceled(true);
        String text = event.getMessage().getString().trim();
        pending.remove(id);
        sender.server.execute(() -> {
            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/")) {
                sender.displayClientMessage(Component.literal("[Claim] Cancelado.").withStyle(ChatFormatting.GRAY), false);
                return;
            }
            Claim claim = findClaimById(p.claimId());
            if (claim == null) {
                sender.displayClientMessage(Component.literal("[x] La zona ya no existe.").withStyle(ChatFormatting.RED), false);
                return;
            }
            switch (p.type()) {
                case ADD_MEMBER -> handleAddMember(sender, claim, text, p.returnPage());
                case REMOVE_MEMBER -> handleRemoveMember(sender, claim, text, p.returnPage());
                case EDIT_WELCOME -> handleEditWelcome(sender, claim, text, p.returnPage());
            }
        });
    }

    private static void handleAdminTransfer(ServerPlayer op, UUID claimId, String name) {
        Claim claim = findClaimById(claimId);
        if (claim == null) {
            op.displayClientMessage(Component.literal("[x] La zona ya no existe.").withStyle(ChatFormatting.RED), false);
            return;
        }
        ServerPlayer online = op.server.getPlayerList().getPlayerByName(name);
        UUID newOwnerId;
        String newOwnerName;
        if (online != null) {
            newOwnerId = online.getUUID();
            newOwnerName = online.getName().getString();
        } else {
            var profileCache = op.server.getProfileCache();
            var profile = profileCache == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty() : profileCache.get(name);
            if (profile.isEmpty()) {
                op.displayClientMessage(Component.literal("[x] Jugador no encontrado.").withStyle(ChatFormatting.RED), false);
                return;
            }
            newOwnerId = profile.get().getId();
            newOwnerName = profile.get().getName();
        }
        claim.setOwner(newOwnerId, newOwnerName);
        claim.getMembers().clear();
        claim.getMemberNames().clear();
        ClaimManager.getInstance().save();
        op.displayClientMessage(Component.literal("\u2714 Zona transferida a " + newOwnerName + ".").withStyle(ChatFormatting.GREEN), false);
        MutableComponent msg = Component.literal("[!] Un administrador te transfiri\u00f3 una zona ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(claim.sizeLabel()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal(" en X:" + claim.getX() + " Z:" + claim.getZ()).withStyle(ChatFormatting.YELLOW));
        if (online != null) online.displayClientMessage(msg, false);
        else ClaimManager.getInstance().queueMessage(newOwnerId, msg);
    }

    private static void handleAddMember(ServerPlayer sender, Claim claim, String name, int page) {
        ServerPlayer target = sender.server.getPlayerList().getPlayerByName(name);
        if (target == null) {
            sender.displayClientMessage(Component.literal("[x] " + name + " no est\u00e1 en l\u00ednea.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (claim.isOwner(target.getUUID())) {
            sender.displayClientMessage(Component.literal("[x] Ese jugador ya es el due\u00f1o.").withStyle(ChatFormatting.RED), false);
            return;
        }
        claim.addMember(target.getUUID(), target.getName().getString());
        ClaimManager.getInstance().save();
        sender.displayClientMessage(Component.literal("\u2714 Jugador agregado como miembro.").withStyle(ChatFormatting.GREEN), false);
        target.displayClientMessage(Component.literal("[Claim] Eres miembro de la zona de " + sender.getName().getString()).withStyle(ChatFormatting.AQUA), false);
        open(sender, claim, page);
    }

    private static void handleRemoveMember(ServerPlayer sender, Claim claim, String name, int page) {
        int idx = -1;
        for (int i = 0; i < claim.getMemberNames().size(); ++i) {
            if (claim.getMemberNames().get(i).equalsIgnoreCase(name)) { idx = i; break; }
        }
        UUID targetId = null;
        if (idx >= 0 && idx < claim.getMembers().size()) {
            targetId = claim.getMembers().get(idx);
        } else {
            ServerPlayer online = sender.server.getPlayerList().getPlayerByName(name);
            if (online != null && claim.isMember(online.getUUID())) targetId = online.getUUID();
        }
        if (targetId == null) {
            sender.displayClientMessage(Component.literal("[x] " + name + " no es miembro de esta zona.").withStyle(ChatFormatting.RED), false);
            open(sender, claim, page);
            return;
        }
        claim.removeMember(targetId);
        ClaimManager.getInstance().save();
        sender.displayClientMessage(Component.literal("\u2714 " + name + " fue eliminado de la zona.").withStyle(ChatFormatting.GREEN), false);
        ServerPlayer removed = sender.server.getPlayerList().getPlayer(targetId);
        if (removed != null) {
            removed.displayClientMessage(Component.literal("[Claim] Ya no eres miembro de la zona de " + sender.getName().getString()).withStyle(ChatFormatting.YELLOW), false);
        }
        open(sender, claim, page);
    }

    private static void handleEditWelcome(ServerPlayer sender, Claim claim, String text, int page) {
        if (text.length() > 60) text = text.substring(0, 60);
        claim.getFlags().welcomeMessage = text;
        claim.getFlags().showWelcome = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.displayClientMessage(Component.literal("\u2714 Bienvenida guardada.").withStyle(ChatFormatting.GREEN), false);
        open(sender, claim, page);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }

    public record PendingChat(PendingType type, UUID claimId, int returnPage) {}

    public static enum PendingType {
        ADD_MEMBER,
        EDIT_WELCOME,
        REMOVE_MEMBER;
    }
}
