/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks.gui;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.AdminClaimSubMenuHandler;
import com.claimblocks.gui.ClaimParticleMenuHandler;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.network.NetworkHooks;

public class ClaimMenuHandler
extends ChestMenu {
    public static final int SIZE = 54;
    private static final int[] FLAG_SLOTS_P0 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31};
    private static final int[] FLAG_SLOTS_P1 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 27};
    private static final ClaimFlags.FlagId[] PAGE_0 = new ClaimFlags.FlagId[]{ClaimFlags.FlagId.BUILDING, ClaimFlags.FlagId.BREAKING, ClaimFlags.FlagId.EXPLOSIONS, ClaimFlags.FlagId.FIRE, ClaimFlags.FlagId.MOB_SPAWN, ClaimFlags.FlagId.PVP, ClaimFlags.FlagId.MOB_DAMAGE, ClaimFlags.FlagId.ALERTS, ClaimFlags.FlagId.PUBLIC_MODE, ClaimFlags.FlagId.ANIMAL_KILLING, ClaimFlags.FlagId.CHEST_ACCESS, ClaimFlags.FlagId.CROP_HARVEST, ClaimFlags.FlagId.BURN_HOSTILES};
    private static final ClaimFlags.FlagId[] PAGE_1 = new ClaimFlags.FlagId[]{ClaimFlags.FlagId.ITEM_USE, ClaimFlags.FlagId.ENTITY_INTERACT, ClaimFlags.FlagId.TRAMPLING, ClaimFlags.FlagId.FLUIDS, ClaimFlags.FlagId.PVP_ALL, ClaimFlags.FlagId.TREE_CHOPPING, ClaimFlags.FlagId.SHOW_WELCOME, ClaimFlags.FlagId.ANVIL_USE, ClaimFlags.FlagId.ENDER_PEARL, ClaimFlags.FlagId.SIGN_EDITING, ClaimFlags.FlagId.DOORS_ACCESS, ClaimFlags.FlagId.EFFECT_REGEN, ClaimFlags.FlagId.EFFECT_RESIST, ClaimFlags.FlagId.EFFECT_SPEED, ClaimFlags.FlagId.ALLOW_FLIGHT, ClaimFlags.FlagId.SHOW_LEAVE, ClaimFlags.FlagId.SHOW_BORDER, ClaimFlags.FlagId.SHOW_PARTICLES};
    private static final Map<UUID, PendingChat> pending = new ConcurrentHashMap<UUID, PendingChat>();
    // Nombre elegido para el grupo durante el flujo de invitacion (paso nombre -> usuarios).
    private static final Map<UUID, String> pendingMergeName = new ConcurrentHashMap<UUID, String>();
    // Invitaciones de union pendientes, por codigo (usado por /claimmerge accept|reject).
    private static final Map<String, MergeInvite> invites = new ConcurrentHashMap<String, MergeInvite>();
    private final SimpleContainer chest;
    private final Claim claim;
    private final ServerPlayer viewer;
    private final int page;
    private boolean awaitingDeleteConfirm = false;

    public ClaimMenuHandler(int syncId, Inventory pInv, Claim claim, int page) {
        this(syncId, pInv, new SimpleContainer(54), claim, page);
    }

    private ClaimMenuHandler(int syncId, Inventory pInv, SimpleContainer chest, Claim claim, int page) {
        super(MenuType.GENERIC_9x6, syncId, pInv, (Container)chest, 6);
        this.chest = chest;
        this.claim = claim;
        this.viewer = (ServerPlayer)pInv.player;
        this.page = page;
        this.rebuild();
    }

    public Claim getClaim() {
        return this.claim;
    }

    public int getPage() {
        return this.page;
    }

    public boolean stillValid(Player player) {
        return true;
    }

    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void rebuild() {
        ItemStack bg = ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.GRAY_STAINED_GLASS_PANE), (Component)Component.literal((String)" "));
        for (int i = 0; i < 54; ++i) {
            this.chest.setItem(i, bg.copy());
        }
        com.claimblocks.data.ClaimGroup hdrGrp = ClaimManager.getInstance().getGroupOf(this.claim);
        String header = hdrGrp != null ? ("Grupo: " + hdrGrp.getName()) : ("Zona " + this.claim.sizeLabel() + " - " + this.claim.getOwnerName());
        this.chest.setItem(4, ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.PAPER), (Component)Component.literal((String)ClaimMenuHandler.truncate(header, 30)).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})));
        this.chest.setItem(11, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.COMPASS), (Component)Component.literal((String)"Coordenadas").withStyle(ChatFormatting.AQUA)), List.of(Component.literal((String)("X=" + this.claim.getX() + " Y=" + this.claim.getY() + " Z=" + this.claim.getZ())).withStyle(ChatFormatting.WHITE))));
        this.chest.setItem(13, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.PLAYER_HEAD), (Component)Component.literal((String)"Due\u00f1o").withStyle(ChatFormatting.AQUA)), List.of(Component.literal((String)ClaimMenuHandler.truncate(this.claim.getOwnerName(), 35)).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}))));
        this.chest.setItem(15, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.DIAMOND), (Component)Component.literal((String)("Zona " + this.claim.sizeLabel())).withStyle(ChatFormatting.YELLOW)), List.of(Component.literal((String)("Zona " + this.claim.sizeLabel() + " bloques")).withStyle(ChatFormatting.GRAY), Component.literal((String)("Altura: +/-" + this.claim.getHeight())).withStyle(ChatFormatting.GRAY))));
        this.chest.setItem(17, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.MAP), (Component)Component.literal((String)"Mundo").withStyle(ChatFormatting.AQUA)), List.of(Component.literal((String)ClaimMenuHandler.truncate(this.claim.getWorld(), 35)).withStyle(ChatFormatting.GRAY))));
        ClaimFlags f = this.claim.getFlags();
        ClaimFlags.FlagId[] ids = this.page == 0 ? PAGE_0 : PAGE_1;
        int[] slots = this.page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        int tierLevel = ClaimMenuHandler.paidLevelOf(this.claim.getTier());
        for (int i = 0; i < ids.length; ++i) {
            ClaimFlags.FlagId id = ids[i];
            int reqLevel = ClaimMenuHandler.requiredPaidLevel(id);
            if (reqLevel > 0 && tierLevel < reqLevel) {
                this.chest.setItem(slots[i], this.lockedEffectButton(id, reqLevel));
                continue;
            }
            this.chest.setItem(slots[i], this.flagButton(id, f.get(id)));
        }
        this.chest.setItem(38, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.WRITABLE_BOOK), (Component)Component.literal((String)("Miembros (" + this.claim.getMembers().size() + ")")).withStyle(ChatFormatting.YELLOW)), this.buildMemberLore()));
        this.chest.setItem(40, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.NAME_TAG), (Component)Component.literal((String)"Quitar miembro").withStyle(ChatFormatting.RED)), List.of(Component.literal((String)"Pide nombre por chat").withStyle(ChatFormatting.GRAY), Component.literal((String)"Clic para eliminar a un invitado").withStyle(ChatFormatting.GRAY))));
        this.chest.setItem(42, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.PLAYER_HEAD), (Component)Component.literal((String)"A\u00f1adir miembro").withStyle(ChatFormatting.GREEN)), List.of(Component.literal((String)"Pide nombre por chat").withStyle(ChatFormatting.GRAY), Component.literal((String)"Clic para a\u00f1adir").withStyle(ChatFormatting.GRAY))));
        this.chest.setItem(39, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.IRON_BARS), (Component)Component.literal((String)"Banear jugador").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})), this.buildBanLore()));
        this.chest.setItem(41, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.TRIPWIRE_HOOK), (Component)Component.literal((String)"Desbanear jugador").withStyle(ChatFormatting.GREEN)), List.of(Component.literal((String)"Pide nombre por chat").withStyle(ChatFormatting.GRAY), Component.literal((String)"Clic para quitar del baneo").withStyle(ChatFormatting.GRAY))));
        if (this.page > 0) {
            this.chest.setItem(45, ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.ARROW), (Component)Component.literal((String)"<< P\u00e1gina anterior").withStyle(ChatFormatting.AQUA)));
        }
        if (this.awaitingDeleteConfirm) {
            this.chest.setItem(46, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.TNT), (Component)Component.literal((String)"Confirmar eliminaci\u00f3n").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})), List.of(Component.literal((String)"Haz clic de nuevo para confirmar").withStyle(ChatFormatting.YELLOW))));
            this.chest.setItem(47, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.LIME_DYE), (Component)Component.literal((String)"Cancelar").withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD})), List.of(Component.literal((String)"Cancela la eliminaci\u00f3n").withStyle(ChatFormatting.GRAY))));
        } else {
            this.chest.setItem(46, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.BARRIER), (Component)Component.literal((String)"Eliminar zona").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})), List.of(Component.literal((String)"Clic para iniciar eliminaci\u00f3n").withStyle(ChatFormatting.YELLOW), Component.literal((String)"Devuelve la protecci\u00f3n al inv.").withStyle(ChatFormatting.GRAY))));
        }
        this.chest.setItem(49, ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.RED_DYE), (Component)Component.literal((String)"Cerrar").withStyle(ChatFormatting.WHITE)));
        this.chest.setItem(52, ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.BOOK), (Component)Component.literal((String)"Ver lista de zonas").withStyle(ChatFormatting.AQUA)));
        if (this.page == 0) {
            this.chest.setItem(53, ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.ARROW), (Component)Component.literal((String)"P\u00e1gina siguiente >>").withStyle(ChatFormatting.AQUA)));
        }
        // ---- Unir protecciones (grupo) ----
        com.claimblocks.data.ClaimGroup grp = ClaimManager.getInstance().getGroupOf(this.claim);
        if (grp == null) {
            this.chest.setItem(43, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.SLIME_BALL), (Component)Component.literal((String)"Unir protecci\u00f3n").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD})), List.of(Component.literal((String)"Crea un grupo y une zonas de tu equipo").withStyle(ChatFormatting.GRAY), Component.literal((String)"Clic: elegir nombre e invitar jugadores").withStyle(ChatFormatting.GRAY))));
        } else {
            this.chest.setItem(43, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.SLIME_BALL), (Component)Component.literal((String)ClaimMenuHandler.truncate("Grupo: " + grp.getName(), 30)).withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD})), List.of(Component.literal((String)("Miembros registrados: " + grp.getRegisteredPlayers().size())).withStyle(ChatFormatting.GRAY), Component.literal((String)"Clic: invitar mas jugadores").withStyle(ChatFormatting.GRAY))));
            this.chest.setItem(44, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new ItemStack((ItemLike)Items.SHEARS), (Component)Component.literal((String)"Disolver grupo").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})), List.of(Component.literal((String)"Separa todas las piedras del grupo").withStyle(ChatFormatting.GRAY), Component.literal((String)"Cada zona vuelve a ser independiente").withStyle(ChatFormatting.GRAY))));
        }
        this.broadcastChanges();
    }

    private List<Component> buildMemberLore() {
        ArrayList<Component> lore = new ArrayList<Component>();
        if (this.claim.getMembers().isEmpty()) {
            lore.add((Component)Component.literal((String)"(sin miembros)").withStyle(ChatFormatting.DARK_GRAY));
            return lore;
        }
        int max = Math.min(5, this.claim.getMembers().size());
        for (int i = 0; i < max; ++i) {
            String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
            lore.add((Component)Component.literal((String)ClaimMenuHandler.truncate(" - " + n, 35)).withStyle(ChatFormatting.WHITE));
        }
        if (this.claim.getMembers().size() > max) {
            lore.add((Component)Component.literal((String)(" - ... y " + (this.claim.getMembers().size() - max) + " m\u00e1s")).withStyle(ChatFormatting.GRAY));
        }
        return lore;
    }

    private List<Component> buildBanLore() {
        ArrayList<Component> lore = new ArrayList<Component>();
        lore.add((Component)Component.literal((String)"Escribe el nombre por chat para banear.").withStyle(ChatFormatting.GRAY));
        lore.add((Component)Component.literal((String)"Al entrar, la barrera los empuja y da\u00f1a.").withStyle(ChatFormatting.DARK_GRAY));
        Set<UUID> banned = this.claim.getBannedPlayers();
        lore.add((Component)Component.literal((String)("Baneados: " + banned.size())).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
        GameProfileCache cache = this.viewer.server.getProfileCache();
        int i = 0;
        for (UUID id : banned) {
            Optional p;
            if (i++ >= 8) {
                lore.add((Component)Component.literal((String)" - ...").withStyle(ChatFormatting.GRAY));
                break;
            }
            String name = id.toString().substring(0, 8);
            if (cache != null && (p = cache.get(id)).isPresent()) {
                name = ((GameProfile)p.get()).getName();
            }
            lore.add((Component)Component.literal((String)ClaimMenuHandler.truncate(" - " + name, 35)).withStyle(ChatFormatting.WHITE));
        }
        return lore;
    }

    public static void requestBanPlayer(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.BAN_PLAYER, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe el nombre del jugador a BANEAR (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestUnbanPlayer(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.UNBAN_PLAYER, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe el nombre del jugador a DESBANEAR (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    private static void handleBanPlayer(ServerPlayer sender, Claim claim, String name, int page) {
        UUID id = null;
        String resolved = name;
        ServerPlayer online = sender.server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            id = online.getUUID();
            resolved = online.getName().getString();
        } else {
            Optional p;
            GameProfileCache cache = sender.server.getProfileCache();
            Optional optional = p = cache == null ? Optional.empty() : cache.get(name);
            if (p.isPresent()) {
                id = ((GameProfile)p.get()).getId();
                resolved = ((GameProfile)p.get()).getName();
            }
        }
        if (id == null) {
            sender.displayClientMessage((Component)Component.literal((String)("[x] Jugador no encontrado: " + name)).withStyle(ChatFormatting.RED), false);
            ClaimMenuHandler.open(sender, claim, page);
        } else if (claim.isOwner(id)) {
            sender.displayClientMessage((Component)Component.literal((String)"[x] No puedes banear al due\u00f1o.").withStyle(ChatFormatting.RED), false);
            ClaimMenuHandler.open(sender, claim, page);
        } else {
            claim.banPlayer(id);
            claim.removeMember(id);
            ClaimManager.getInstance().save();
            sender.displayClientMessage((Component)Component.literal((String)("\u2714 " + resolved + " baneado de la zona.")).withStyle(ChatFormatting.GREEN), false);
            if (online != null) {
                online.displayClientMessage((Component)Component.literal((String)("[!] Has sido baneado de una zona de " + sender.getName().getString())).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), false);
            }
            ClaimMenuHandler.open(sender, claim, page);
        }
    }

    private static void handleUnbanPlayer(ServerPlayer sender, Claim claim, String name, int page) {
        UUID id = null;
        ServerPlayer online = sender.server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            id = online.getUUID();
        } else {
            Optional p;
            GameProfileCache cache = sender.server.getProfileCache();
            Optional optional = p = cache == null ? Optional.empty() : cache.get(name);
            if (p.isPresent()) {
                id = ((GameProfile)p.get()).getId();
            }
        }
        if (id == null || !claim.isBanned(id)) {
            sender.displayClientMessage((Component)Component.literal((String)"[x] Ese jugador no est\u00e1 baneado.").withStyle(ChatFormatting.RED), false);
            ClaimMenuHandler.open(sender, claim, page);
        } else {
            claim.unbanPlayer(id);
            ClaimManager.getInstance().save();
            sender.displayClientMessage((Component)Component.literal((String)("\u2714 " + name + " desbaneado.")).withStyle(ChatFormatting.GREEN), false);
            ClaimMenuHandler.open(sender, claim, page);
        }
    }

    private static int paidLevelOf(ClaimTier t) {
        String var1;
        if (t == null) {
            return 0;
        }
        return switch (var1 = t.id) {
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
        ItemStack stack = new ItemStack((ItemLike)Items.BLACK_STAINED_GLASS_PANE);
        return ClaimMenuHandler.withLore(ClaimMenuHandler.withName(stack, (Component)Component.literal((String)(ClaimMenuHandler.effectName(id) + " [LOCKED]")).withStyle(ChatFormatting.DARK_GRAY)), List.of(Component.literal((String)("Requiere zona " + ClaimMenuHandler.requiredTierLabel(reqLevel) + " o superior")).withStyle(ChatFormatting.GRAY), Component.literal((String)ClaimMenuHandler.effectShortDesc(id)).withStyle(ChatFormatting.DARK_GRAY)));
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
        ItemStack stack = new ItemStack((ItemLike)(enabled ? Items.LIME_DYE : Items.GRAY_DYE));
        MutableComponent name = Component.literal((String)ClaimMenuHandler.flagDisplayName(id, enabled)).withStyle(new ChatFormatting[]{enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD});
        String[] lore = ClaimMenuHandler.flagLore(id);
        return ClaimMenuHandler.withLore(ClaimMenuHandler.withName(stack, (Component)name), List.of(Component.literal((String)lore[0]).withStyle(ChatFormatting.GRAY), Component.literal((String)("Estado: " + (enabled ? "ACTIVO" : "INACTIVO") + " - " + lore[1])).withStyle(ChatFormatting.GRAY)));
    }

    private static String flagDisplayName(ClaimFlags.FlagId id, boolean on) {
        return switch (id) {
            case EFFECT_REGEN -> {
                if (on) {
                    yield "Regeneraci\u00f3n pasiva [ON]";
                }
                yield "Regeneraci\u00f3n pasiva [OFF]";
            }
            case EFFECT_RESIST -> {
                if (on) {
                    yield "Resistencia pasiva [ON]";
                }
                yield "Resistencia pasiva [OFF]";
            }
            case EFFECT_SPEED -> {
                if (on) {
                    yield "Velocidad pasiva [ON]";
                }
                yield "Velocidad pasiva [OFF]";
            }
            case ALLOW_FLIGHT -> {
                if (on) {
                    yield "Vuelo en zona: ACTIVO [ON]";
                }
                yield "Vuelo en zona: inactivo [OFF]";
            }
            case BUILDING -> {
                if (on) {
                    yield "Construir: BLOQUEADO [ON]";
                }
                yield "Construir: permitido [OFF]";
            }
            case BREAKING -> {
                if (on) {
                    yield "Romper: BLOQUEADO [ON]";
                }
                yield "Romper: permitido [OFF]";
            }
            case EXPLOSIONS -> {
                if (on) {
                    yield "Explosiones: BLOQUEADAS [ON]";
                }
                yield "Explosiones: permitidas [OFF]";
            }
            case FIRE -> {
                if (on) {
                    yield "Fuego: BLOQUEADO [ON]";
                }
                yield "Fuego: permitido [OFF]";
            }
            case MOB_SPAWN -> {
                if (on) {
                    yield "Mobs hostiles: BLOQUEADOS [ON]";
                }
                yield "Mobs hostiles: permit. [OFF]";
            }
            case PVP -> {
                if (on) {
                    yield "PVP: BLOQUEADO [ON]";
                }
                yield "PVP: permitido [OFF]";
            }
            case MOB_DAMAGE -> {
                if (on) {
                    yield "Da\u00f1o de mobs: BLOQUEADO [ON]";
                }
                yield "Da\u00f1o de mobs: permit. [OFF]";
            }
            case ALERTS -> {
                if (on) {
                    yield "Alertas intrusos: ON [ON]";
                }
                yield "Alertas intrusos: OFF [OFF]";
            }
            case ITEM_USE -> {
                if (on) {
                    yield "Usar items: BLOQUEADO [ON]";
                }
                yield "Usar items: permitido [OFF]";
            }
            case ENTITY_INTERACT -> {
                if (on) {
                    yield "Entidades: BLOQUEADAS [ON]";
                }
                yield "Entidades: libres [OFF]";
            }
            case TRAMPLING -> {
                if (on) {
                    yield "Cultivos: PROTEGIDOS [ON]";
                }
                yield "Cultivos: sin protec. [OFF]";
            }
            case FLUIDS -> {
                if (on) {
                    yield "Fluidos: BLOQUEADOS [ON]";
                }
                yield "Fluidos: permitidos [OFF]";
            }
            case PVP_ALL -> {
                if (on) {
                    yield "Zona PVP libre: ACTIVA [ON]";
                }
                yield "Zona PVP libre: inact. [OFF]";
            }
            case TREE_CHOPPING -> {
                if (on) {
                    yield "\u00c1rboles: PROTEGIDOS [ON]";
                }
                yield "\u00c1rboles: se talan [OFF]";
            }
            case PUBLIC_MODE -> {
                if (on) {
                    yield "Modo visita: ACTIVO [ON]";
                }
                yield "Modo visita: inactivo [OFF]";
            }
            case SHOW_WELCOME -> {
                if (on) {
                    yield "Bienvenida custom: ON [ON]";
                }
                yield "Bienvenida custom: OFF [OFF]";
            }
            case SHOW_LEAVE -> {
                if (on) {
                    yield "Mensaje de salida: ON [ON]";
                }
                yield "Mensaje de salida: OFF [OFF]";
            }
            case SHOW_BORDER -> {
                if (on) {
                    yield "Ver contorno: ON [ON]";
                }
                yield "Ver contorno: OFF [OFF]";
            }
            case SHOW_PARTICLES -> {
                if (on) {
                    yield "Ver part\u00edculas: ON [ON]";
                }
                yield "Ver part\u00edculas: OFF [OFF]";
            }
            case BURN_HOSTILES -> {
                if (on) {
                    yield "Repeler hostiles: ON [ON]";
                }
                yield "Repeler hostiles: OFF [OFF]";
            }
            case ANIMAL_KILLING -> {
                if (on) {
                    yield "Animales: PROTEGIDOS [ON]";
                }
                yield "Animales: se matan [OFF]";
            }
            case CHEST_ACCESS -> {
                if (on) {
                    yield "Cofres: BLOQUEADOS [ON]";
                }
                yield "Cofres: acceso libre [OFF]";
            }
            case CROP_HARVEST -> {
                if (on) {
                    yield "Cosecha: PROTEGIDA [ON]";
                }
                yield "Cosecha: libre [OFF]";
            }
            case ANVIL_USE -> {
                if (on) {
                    yield "Yunques: BLOQUEADOS [ON]";
                }
                yield "Yunques: uso libre [OFF]";
            }
            case ENDER_PEARL -> {
                if (on) {
                    yield "Ender pearl: BLOQUEADA [ON]";
                }
                yield "Ender pearl: permitida [OFF]";
            }
            case SIGN_EDITING -> {
                if (on) {
                    yield "Letreros: BLOQUEADOS [ON]";
                }
                yield "Letreros: editables [OFF]";
            }
            case DOORS_ACCESS -> {
                if (on) {
                    yield "Puertas/Botones: BLOQ [ON]";
                }
                yield "Puertas/Botones: libres [OFF]";
            }
            default -> throw new IncompatibleClassChangeError();
        };
    }

    private static String[] flagLore(ClaimFlags.FlagId id) {
        String desc = switch (id) {
            case EFFECT_REGEN -> "Regenera vida a due\u00f1o y miembros";
            case EFFECT_RESIST -> "Reduce da\u00f1o a due\u00f1o y miembros";
            case EFFECT_SPEED -> "Da velocidad a due\u00f1o y miembros";
            case ALLOW_FLIGHT -> "Due\u00f1o puede volar";
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
            case SHOW_LEAVE -> "Mensaje personalizado al salir";
            case SHOW_BORDER -> "Dibuja el contorno de tu protecci\u00f3n (l\u00edneas)";
            case SHOW_PARTICLES -> "Llena tu protecci\u00f3n con part\u00edculas";
            case BURN_HOSTILES -> "Quema a los mobs hostiles que entren (d\u00eda o noche)";
            case ANIMAL_KILLING -> "Intrusos no pueden matar animales";
            case CHEST_ACCESS -> "Intrusos no abren cofres ni barriles";
            case CROP_HARVEST -> "Intrusos no cosechan cultivos";
            case ANVIL_USE -> "Intrusos no pueden usar yunques";
            case ENDER_PEARL -> "Intrusos no se teletransportan";
            case SIGN_EDITING -> "Intrusos no editan letreros";
            case DOORS_ACCESS -> "Intrusos no usan puertas, botones ni placas";
            default -> throw new IncompatibleClassChangeError();
        };
        String action = id == ClaimFlags.FlagId.SHOW_WELCOME || id == ClaimFlags.FlagId.SHOW_LEAVE ? "Clic izq: editar | Clic der: on/off" : (id == ClaimFlags.FlagId.SHOW_PARTICLES ? "Clic para elegir part\u00edcula y densidad" : "Clic para cambiar");
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
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 54) {
            if (slotId == 45 && this.page > 0) {
                ClaimMenuHandler.open(this.viewer, this.claim, this.page - 1);
            } else if (slotId == 53 && this.page == 0) {
                ClaimMenuHandler.open(this.viewer, this.claim, this.page + 1);
            } else if (slotId == 46) {
                if (!this.awaitingDeleteConfirm) {
                    this.awaitingDeleteConfirm = true;
                    this.rebuild();
                    this.viewer.displayClientMessage((Component)Component.literal((String)"[!] Haz clic de nuevo para confirmar.").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    this.performDelete();
                }
            } else if (slotId == 47 && this.awaitingDeleteConfirm) {
                this.awaitingDeleteConfirm = false;
                this.rebuild();
                this.viewer.displayClientMessage((Component)Component.literal((String)"[i] Eliminaci\u00f3n cancelada.").withStyle(ChatFormatting.AQUA), true);
            } else {
                ClaimFlags.FlagId clicked;
                if (this.awaitingDeleteConfirm) {
                    this.awaitingDeleteConfirm = false;
                }
                if ((clicked = this.slotToFlag(slotId)) != null) {
                    int tierLevel;
                    int reqLevel = ClaimMenuHandler.requiredPaidLevel(clicked);
                    if (reqLevel > 0 && (tierLevel = ClaimMenuHandler.paidLevelOf(this.claim.getTier())) < reqLevel) {
                        this.viewer.displayClientMessage((Component)Component.literal((String)("[x] Requiere zona " + ClaimMenuHandler.requiredTierLabel(reqLevel) + " o superior.")).withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    if (clicked == ClaimFlags.FlagId.SHOW_WELCOME) {
                        if (button == 1) {
                            this.claim.getFlags().showWelcome = !this.claim.getFlags().showWelcome;
                            ClaimManager.getInstance().save();
                            this.rebuild();
                        } else {
                            ClaimMenuHandler.requestEditWelcome(this.viewer, this.claim, this.page);
                            this.viewer.closeContainer();
                        }
                    } else if (clicked == ClaimFlags.FlagId.SHOW_LEAVE) {
                        if (button == 1) {
                            this.claim.getFlags().showLeave = !this.claim.getFlags().showLeave;
                            ClaimManager.getInstance().save();
                            this.rebuild();
                        } else {
                            ClaimMenuHandler.requestEditLeave(this.viewer, this.claim, this.page);
                            this.viewer.closeContainer();
                        }
                    } else if (clicked == ClaimFlags.FlagId.SHOW_BORDER) {
                        this.claim.getFlags().showBorder = !this.claim.getFlags().showBorder;
                        ClaimManager.getInstance().save();
                        this.rebuild();
                    } else if (clicked == ClaimFlags.FlagId.SHOW_PARTICLES) {
                        ClaimParticleMenuHandler.open(this.viewer, this.claim, this.page);
                    } else {
                        this.claim.getFlags().toggle(clicked);
                        ClaimManager.getInstance().save();
                        this.rebuild();
                    }
                } else if (slotId == 38) {
                    this.viewer.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Miembros de la zona:").withStyle(ChatFormatting.GRAY), false);
                    if (this.claim.getMembers().isEmpty()) {
                        this.viewer.displayClientMessage((Component)Component.literal((String)"  (sin miembros)").withStyle(ChatFormatting.DARK_GRAY), false);
                    } else {
                        for (int i = 0; i < this.claim.getMembers().size(); ++i) {
                            String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
                            this.viewer.displayClientMessage((Component)Component.literal((String)("  - " + n)).withStyle(ChatFormatting.WHITE), false);
                        }
                    }
                } else if (slotId == 42) {
                    ClaimMenuHandler.requestAddMember(this.viewer, this.claim, this.page);
                    this.viewer.closeContainer();
                } else if (slotId == 40) {
                    if (this.claim.getMembers().isEmpty()) {
                        this.viewer.displayClientMessage((Component)Component.literal((String)"[i] Esta zona no tiene miembros que quitar.").withStyle(ChatFormatting.YELLOW), true);
                    } else {
                        ClaimMenuHandler.requestRemoveMember(this.viewer, this.claim, this.page);
                        this.viewer.closeContainer();
                    }
                } else if (slotId == 39) {
                    ClaimMenuHandler.requestBanPlayer(this.viewer, this.claim, this.page);
                    this.viewer.closeContainer();
                } else if (slotId == 41) {
                    if (this.claim.getBannedPlayers().isEmpty()) {
                        this.viewer.displayClientMessage((Component)Component.literal((String)"[i] No hay jugadores baneados.").withStyle(ChatFormatting.YELLOW), true);
                    } else {
                        ClaimMenuHandler.requestUnbanPlayer(this.viewer, this.claim, this.page);
                        this.viewer.closeContainer();
                    }
                } else if (slotId == 43) {
                    com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroupOf(this.claim);
                    if (g == null) {
                        ClaimMenuHandler.requestMergeName(this.viewer, this.claim, this.page);
                        this.viewer.closeContainer();
                    } else if (this.claim.isGroupMother()) {
                        ClaimMenuHandler.requestMergeUsers(this.viewer, this.claim, this.page);
                        this.viewer.closeContainer();
                    }
                } else if (slotId == 44) {
                    com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroupOf(this.claim);
                    if (g != null && this.claim.isGroupMother()) {
                        ClaimManager.getInstance().dissolveGroup(g.getGroupId());
                        this.viewer.displayClientMessage((Component)Component.literal((String)"\u2714 Grupo disuelto. Cada zona vuelve a ser independiente.").withStyle(ChatFormatting.GREEN), false);
                        this.rebuild();
                    }
                } else if (slotId == 49) {
                    this.viewer.closeContainer();
                } else if (slotId == 52) {
                    this.viewer.closeContainer();
                    this.viewer.server.getCommands().performPrefixedCommand(this.viewer.createCommandSourceStack(), "claim list");
                }
            }
        }
    }

    private void performDelete() {
        ClaimTier tier = this.claim.getTier();
        Level world = this.viewer.level();
        BlockPos centre = this.claim.getCenter();
        if (tier != null && ClaimBlocks.isClaimConcreteForTier(world.getBlockState(centre).getBlock(), tier)) {
            world.destroyBlock(centre, false);
        }
        world.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0f, 1.0f);
        ClaimManager.getInstance().removeClaim(world, centre);
        if (tier != null) {
            ItemStack stack = ClaimBlocks.createTierItem(tier, 1);
            if (!this.viewer.getInventory().add(stack)) {
                this.viewer.drop(stack, false);
            }
        }
        this.viewer.displayClientMessage((Component)Component.literal((String)"\u2714 Zona eliminada. Protecci\u00f3n devuelta a tu inventario.").withStyle(ChatFormatting.GREEN), false);
        this.viewer.closeContainer();
    }

    private ClaimFlags.FlagId slotToFlag(int slotIndex) {
        ClaimFlags.FlagId[] ids = this.page == 0 ? PAGE_0 : PAGE_1;
        int[] slots = this.page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        for (int i = 0; i < slots.length; ++i) {
            if (slots[i] != slotIndex) continue;
            return ids[i];
        }
        return null;
    }

    public static void open(ServerPlayer player, Claim claim, int page) {
        ClaimMenuHandler.open(player, claim, page, null);
    }

    public static void open(ServerPlayer player, final Claim claim, int page, String customTitle) {
        // Gate: las piedras miembro (no nodriza) no abren la GUI. Solo se pueden romper.
        if (claim.getGroupId() != null && !claim.isGroupMother()) {
            Claim mother = claim.getMother();
            String on = mother != null ? mother.getOwnerName() : "?";
            player.displayClientMessage((Component)Component.literal((String)("[!] Esta piedra pertenece al grupo de " + on + ". Solo la piedra nodriza gestiona el grupo. Puedes romperla para recuperarla.")).withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        final int p = Math.max(0, Math.min(1, page));
        com.claimblocks.data.ClaimGroup titleGrp = ClaimManager.getInstance().getGroupOf(claim);
        final String title = customTitle != null ? ClaimMenuHandler.truncate(customTitle, 40) : (titleGrp != null ? ClaimMenuHandler.truncate("Grupo: " + titleGrp.getName(), 40) : ClaimMenuHandler.truncate("Zona " + claim.sizeLabel() + " - " + claim.getOwnerName(), 40));
        NetworkHooks.openScreen((ServerPlayer)player, (MenuProvider)new MenuProvider(){

            public Component getDisplayName() {
                return Component.literal((String)title).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD});
            }

            public AbstractContainerMenu createMenu(int id, Inventory inv, Player pl) {
                return new ClaimMenuHandler(id, inv, claim, p);
            }
        });
    }

    public static void requestAddMember(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.ADD_MEMBER, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe el nombre del jugador (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestRemoveMember(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.REMOVE_MEMBER, claim.getClaimId(), returnPage));
        StringBuilder sb = new StringBuilder();
        List<String> names = claim.getMemberNames();
        for (int i = 0; i < names.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names.get(i));
        }
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Miembros: ").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)sb.toString()).withStyle(ChatFormatting.WHITE)), false);
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe el nombre del invitado a quitar (o 'cancelar'):").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestEditWelcome(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.EDIT_WELCOME, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe tu bienvenida (max 60 chars) o 'cancelar':").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void requestEditLeave(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.EDIT_LEAVE, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Escribe tu mensaje de salida (max 60 chars) o 'cancelar':").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void handleChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        UUID id = sender.getUUID();
        if (AdminClaimSubMenuHandler.hasPendingTransfer(id)) {
            event.setCanceled(true);
            String text = event.getMessage().getString().trim();
            UUID claimId = AdminClaimSubMenuHandler.popPendingTransfer(id);
            sender.server.execute(() -> {
                if (!(text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/"))) {
                    ClaimMenuHandler.handleAdminTransfer(sender, claimId, text);
                } else {
                    sender.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Cancelado.").withStyle(ChatFormatting.GRAY), false);
                }
            });
        } else {
            PendingChat p = pending.get(id);
            if (p != null) {
                event.setCanceled(true);
                String text = event.getMessage().getString().trim();
                pending.remove(id);
                sender.server.execute(() -> {
                    if (!(text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/"))) {
                        Claim claim = ClaimMenuHandler.findClaimById(p.claimId());
                        if (claim == null) {
                            sender.displayClientMessage((Component)Component.literal((String)"[x] La zona ya no existe.").withStyle(ChatFormatting.RED), false);
                        } else {
                            switch (p.type()) {
                                case ADD_MEMBER: {
                                    ClaimMenuHandler.handleAddMember(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case REMOVE_MEMBER: {
                                    ClaimMenuHandler.handleRemoveMember(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case EDIT_WELCOME: {
                                    ClaimMenuHandler.handleEditWelcome(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case EDIT_LEAVE: {
                                    ClaimMenuHandler.handleEditLeave(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case BAN_PLAYER: {
                                    ClaimMenuHandler.handleBanPlayer(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case UNBAN_PLAYER: {
                                    ClaimMenuHandler.handleUnbanPlayer(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case MERGE_NAME: {
                                    ClaimMenuHandler.handleMergeName(sender, claim, text, p.returnPage());
                                    break;
                                }
                                case MERGE_USERS: {
                                    ClaimMenuHandler.handleMergeUsers(sender, claim, text, p.returnPage());
                                }
                            }
                        }
                    } else {
                        sender.displayClientMessage((Component)Component.literal((String)"[Protecci\u00f3n] Cancelado.").withStyle(ChatFormatting.GRAY), false);
                    }
                });
            }
        }
    }

    private static void handleAdminTransfer(ServerPlayer op, UUID claimId, String name) {
        Claim claim = ClaimMenuHandler.findClaimById(claimId);
        if (claim == null) {
            op.displayClientMessage((Component)Component.literal((String)"[x] La zona ya no existe.").withStyle(ChatFormatting.RED), false);
        } else {
            String newOwnerName;
            UUID newOwnerId;
            ServerPlayer online = op.server.getPlayerList().getPlayerByName(name);
            if (online != null) {
                newOwnerId = online.getUUID();
                newOwnerName = online.getName().getString();
            } else {
                Optional profile;
                GameProfileCache profileCache = op.server.getProfileCache();
                Optional optional = profile = profileCache == null ? Optional.empty() : profileCache.get(name);
                if (profile.isEmpty()) {
                    op.displayClientMessage((Component)Component.literal((String)"[x] Jugador no encontrado.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                newOwnerId = ((GameProfile)profile.get()).getId();
                newOwnerName = ((GameProfile)profile.get()).getName();
            }
            claim.setOwner(newOwnerId, newOwnerName);
            claim.getMembers().clear();
            claim.getMemberNames().clear();
            ClaimManager.getInstance().save();
            op.displayClientMessage((Component)Component.literal((String)("\u2714 Zona transferida a " + newOwnerName + ".")).withStyle(ChatFormatting.GREEN), false);
            MutableComponent msg = Component.literal((String)"[!] Un administrador te transfiri\u00f3 una zona ").withStyle(ChatFormatting.YELLOW).append((Component)Component.literal((String)claim.sizeLabel()).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD})).append((Component)Component.literal((String)(" en X:" + claim.getX() + " Z:" + claim.getZ())).withStyle(ChatFormatting.YELLOW));
            if (online != null) {
                online.displayClientMessage((Component)msg, false);
            } else {
                ClaimManager.getInstance().queueMessage(newOwnerId, (Component)msg);
            }
        }
    }

    private static void handleAddMember(ServerPlayer sender, Claim claim, String name, int page) {
        ServerPlayer target = sender.server.getPlayerList().getPlayerByName(name);
        if (target == null) {
            sender.displayClientMessage((Component)Component.literal((String)("[x] " + name + " no est\u00e1 en l\u00ednea.")).withStyle(ChatFormatting.RED), false);
        } else if (claim.isOwner(target.getUUID())) {
            sender.displayClientMessage((Component)Component.literal((String)"[x] Ese jugador ya es el due\u00f1o.").withStyle(ChatFormatting.RED), false);
        } else {
            claim.addMember(target.getUUID(), target.getName().getString());
            ClaimManager.getInstance().save();
            sender.displayClientMessage((Component)Component.literal((String)"\u2714 Jugador agregado como miembro.").withStyle(ChatFormatting.GREEN), false);
            target.displayClientMessage((Component)Component.literal((String)("[Protecci\u00f3n] Eres miembro de la zona de " + sender.getName().getString())).withStyle(ChatFormatting.AQUA), false);
            ClaimMenuHandler.open(sender, claim, page);
        }
    }

    private static void handleRemoveMember(ServerPlayer sender, Claim claim, String name, int page) {
        int idx = -1;
        for (int i = 0; i < claim.getMemberNames().size(); ++i) {
            if (!claim.getMemberNames().get(i).equalsIgnoreCase(name)) continue;
            idx = i;
            break;
        }
        UUID targetId = null;
        if (idx >= 0 && idx < claim.getMembers().size()) {
            targetId = claim.getMembers().get(idx);
        } else {
            ServerPlayer online = sender.server.getPlayerList().getPlayerByName(name);
            if (online != null && claim.isMember(online.getUUID())) {
                targetId = online.getUUID();
            }
        }
        if (targetId == null) {
            sender.displayClientMessage((Component)Component.literal((String)("[x] " + name + " no es miembro de esta zona.")).withStyle(ChatFormatting.RED), false);
            ClaimMenuHandler.open(sender, claim, page);
        } else {
            claim.removeMember(targetId);
            ClaimManager.getInstance().save();
            sender.displayClientMessage((Component)Component.literal((String)("\u2714 " + name + " fue eliminado de la zona.")).withStyle(ChatFormatting.GREEN), false);
            ServerPlayer removed = sender.server.getPlayerList().getPlayer(targetId);
            if (removed != null) {
                removed.displayClientMessage((Component)Component.literal((String)("[Protecci\u00f3n] Ya no eres miembro de la zona de " + sender.getName().getString())).withStyle(ChatFormatting.YELLOW), false);
            }
            ClaimMenuHandler.open(sender, claim, page);
        }
    }

    private static void handleEditWelcome(ServerPlayer sender, Claim claim, String text, int page) {
        if (text.length() > 60) {
            text = text.substring(0, 60);
        }
        claim.getFlags().welcomeMessage = text;
        claim.getFlags().showWelcome = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.displayClientMessage((Component)Component.literal((String)"\u2714 Bienvenida guardada.").withStyle(ChatFormatting.GREEN), false);
        ClaimMenuHandler.open(sender, claim, page);
    }

    private static void handleEditLeave(ServerPlayer sender, Claim claim, String text, int page) {
        if (text.length() > 60) {
            text = text.substring(0, 60);
        }
        claim.getFlags().leaveMessage = text;
        claim.getFlags().showLeave = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.displayClientMessage((Component)Component.literal((String)"\u2714 Mensaje de salida guardado.").withStyle(ChatFormatting.GREEN), false);
        ClaimMenuHandler.open(sender, claim, page);
    }

    // ==================== UNIR PROTECCIONES (grupo) ====================
    public static void requestMergeName(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.MERGE_NAME, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Grupo] Escribe el NOMBRE de la zona unida (o 'cancelar'):").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    public static void requestMergeUsers(ServerPlayer player, Claim claim, int returnPage) {
        pending.put(player.getUUID(), new PendingChat(PendingType.MERGE_USERS, claim.getClaimId(), returnPage));
        player.displayClientMessage((Component)Component.literal((String)"[Grupo] Escribe el/los jugadores a invitar (separados por espacio) o 'cancelar':").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static void handleMergeName(ServerPlayer sender, Claim claim, String text, int page) {
        String name = text.length() > 32 ? text.substring(0, 32) : text;
        pendingMergeName.put(sender.getUUID(), name);
        pending.put(sender.getUUID(), new PendingChat(PendingType.MERGE_USERS, claim.getClaimId(), page));
        sender.displayClientMessage((Component)Component.literal((String)("[Grupo] Nombre: \"" + name + "\". Ahora escribe el/los jugadores a invitar (separados por espacio):")).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static void handleMergeUsers(ServerPlayer sender, Claim claim, String text, int page) {
        ClaimManager mgr = ClaimManager.getInstance();
        com.claimblocks.data.ClaimGroup g = mgr.getGroupOf(claim);
        if (g == null) {
            String name = pendingMergeName.getOrDefault(sender.getUUID(), "Grupo");
            g = mgr.createGroup(claim, name);
        }
        pendingMergeName.remove(sender.getUUID());
        String[] parts = text.split("[ ,]+");
        int sent = 0;
        for (String raw : parts) {
            String pname = raw.trim();
            if (pname.isEmpty()) {
                continue;
            }
            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(pname);
            if (target == null) {
                sender.displayClientMessage((Component)Component.literal((String)("[x] " + pname + " no esta en linea (debe estar conectado para invitarlo).")).withStyle(ChatFormatting.RED), false);
                continue;
            }
            if (target.getUUID().equals(sender.getUUID())) {
                continue;
            }
            if (g.isRegistered(target.getUUID())) {
                sender.displayClientMessage((Component)Component.literal((String)("[i] " + target.getName().getString() + " ya esta en el grupo.")).withStyle(ChatFormatting.GRAY), false);
                continue;
            }
            String code = ClaimMenuHandler.genCode();
            invites.put(code, new MergeInvite(code, g.getGroupId(), target.getUUID(), sender.getName().getString(), g.getName()));
            ClaimMenuHandler.sendInvite(target, sender.getName().getString(), g.getName(), code);
            ++sent;
        }
        if (sent > 0) {
            sender.displayClientMessage((Component)Component.literal((String)("\u2714 Invitacion enviada a " + sent + " jugador(es). Grupo: \"" + g.getName() + "\".")).withStyle(ChatFormatting.GREEN), false);
        }
        ClaimMenuHandler.open(sender, claim, page);
    }

    private static void sendInvite(ServerPlayer target, String inviterName, String groupName, String code) {
        target.displayClientMessage((Component)Component.literal((String)("[Grupo] " + inviterName + " te invita a unir tu proteccion al grupo \"" + groupName + "\".")).withStyle(ChatFormatting.AQUA), false);
        Component accept = (Component)Component.literal((String)" [\u2714 ACEPTAR] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(Boolean.valueOf(true)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claimmerge accept " + code)));
        Component reject = (Component)Component.literal((String)"[\u2718 RECHAZAR]").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(Boolean.valueOf(true)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claimmerge reject " + code)));
        target.displayClientMessage((Component)Component.literal((String)"").append(accept).append(reject), false);
    }

    public static void acceptMerge(ServerPlayer target, String code) {
        MergeInvite inv = invites.remove(code);
        if (inv == null || !target.getUUID().equals(inv.targetId())) {
            target.displayClientMessage((Component)Component.literal((String)"[x] Invitacion no valida o expirada.").withStyle(ChatFormatting.RED), false);
            return;
        }
        ClaimManager mgr = ClaimManager.getInstance();
        com.claimblocks.data.ClaimGroup g = mgr.getGroup(inv.groupId());
        if (g == null) {
            target.displayClientMessage((Component)Component.literal((String)"[x] El grupo ya no existe.").withStyle(ChatFormatting.RED), false);
            return;
        }
        mgr.registerPlayer(g.getGroupId(), target.getUUID());
        target.displayClientMessage((Component)Component.literal((String)("\u2714 Te uniste al grupo \"" + g.getName() + "\". Ahora tus piedras colocadas dentro de esa zona se uniran.")).withStyle(ChatFormatting.GREEN), false);
        Component note = (Component)Component.literal((String)(target.getName().getString() + " acepto unirse al grupo \"" + g.getName() + "\".")).withStyle(ChatFormatting.GREEN);
        ServerPlayer inviter = g.getMotherOwnerId() == null ? null : target.server.getPlayerList().getPlayer(g.getMotherOwnerId());
        if (inviter != null) {
            inviter.displayClientMessage(note, false);
        } else if (g.getMotherOwnerId() != null) {
            mgr.queueMessage(g.getMotherOwnerId(), note);
        }
    }

    public static void rejectMerge(ServerPlayer target, String code) {
        MergeInvite inv = invites.remove(code);
        if (inv == null || !target.getUUID().equals(inv.targetId())) {
            target.displayClientMessage((Component)Component.literal((String)"[x] Invitacion no valida o expirada.").withStyle(ChatFormatting.RED), false);
            return;
        }
        target.displayClientMessage((Component)Component.literal((String)"[i] Rechazaste la invitacion de union.").withStyle(ChatFormatting.GRAY), false);
        com.claimblocks.data.ClaimGroup g = ClaimManager.getInstance().getGroup(inv.groupId());
        if (g != null && g.getMotherOwnerId() != null) {
            Component note = (Component)Component.literal((String)(target.getName().getString() + " rechazo unirse al grupo \"" + g.getName() + "\".")).withStyle(ChatFormatting.YELLOW);
            ServerPlayer inviter = target.server.getPlayerList().getPlayer(g.getMotherOwnerId());
            if (inviter != null) {
                inviter.displayClientMessage(note, false);
            } else {
                ClaimManager.getInstance().queueMessage(g.getMotherOwnerId(), note);
            }
        }
    }

    public static void leaveMerge(ServerPlayer player) {
        ClaimManager mgr = ClaimManager.getInstance();
        com.claimblocks.data.ClaimGroup g = mgr.getGroupByRegistered(player.getUUID());
        if (g == null) {
            player.displayClientMessage((Component)Component.literal((String)"[!] No estas en ningun grupo.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        boolean wasMother = player.getUUID().equals(g.getMotherOwnerId());
        String name = g.getName();
        mgr.removePlayerFromGroup(g.getGroupId(), player.getUUID());
        player.displayClientMessage((Component)Component.literal((String)(wasMother ? ("\u2714 Disolviste el grupo \"" + name + "\".") : ("\u2714 Saliste del grupo \"" + name + "\". Tus piedras vuelven a ser independientes."))).withStyle(ChatFormatting.GREEN), false);
    }

    private static String genCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (!c.getClaimId().equals(id)) continue;
            return c;
        }
        return null;
    }

    public record PendingChat(PendingType type, UUID claimId, int returnPage) {
    }

    public static enum PendingType {
        ADD_MEMBER,
        EDIT_WELCOME,
        EDIT_LEAVE,
        BAN_PLAYER,
        UNBAN_PLAYER,
        REMOVE_MEMBER,
        MERGE_NAME,
        MERGE_USERS;

    }

    public record MergeInvite(String code, UUID groupId, UUID targetId, String inviterName, String groupName) {
    }
}

