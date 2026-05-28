/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.fabric.api.message.v1.ServerMessageEvents
 *  net.minecraft.class_124
 *  net.minecraft.class_1263
 *  net.minecraft.class_1277
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_3324
 *  net.minecraft.class_3908
 *  net.minecraft.class_3917
 *  net.minecraft.class_5250
 *  net.minecraft.class_747
 *  net.minecraft.class_9290
 *  net.minecraft.class_9334
 */
package com.claimblocks.gui;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.AdminClaimSubMenuHandler;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.class_124;
import net.minecraft.class_1263;
import net.minecraft.class_1277;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3324;
import net.minecraft.class_3908;
import net.minecraft.class_3917;
import net.minecraft.class_5250;
import net.minecraft.class_747;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

public class ClaimMenuHandler
extends class_1703 {
    public static final int SIZE = 54;
    private static final int SLOT_TITLE = 4;
    private static final int SLOT_COORDS = 11;
    private static final int SLOT_OWNER = 13;
    private static final int SLOT_TIER = 15;
    private static final int SLOT_WORLD = 17;
    private static final int SLOT_VIEW_MEMBERS = 38;
    private static final int SLOT_ADD_MEMBER = 42;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_DELETE = 46;
    private static final int SLOT_CANCEL_DEL = 47;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_LIST = 52;
    private static final int SLOT_NEXT = 53;
    private static final int[] FLAG_SLOTS_P0 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30};
    private static final int[] FLAG_SLOTS_P1 = new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32};
    private static final ClaimFlags.FlagId[] PAGE_0 = new ClaimFlags.FlagId[]{ClaimFlags.FlagId.BUILDING, ClaimFlags.FlagId.BREAKING, ClaimFlags.FlagId.EXPLOSIONS, ClaimFlags.FlagId.FIRE, ClaimFlags.FlagId.MOB_SPAWN, ClaimFlags.FlagId.PVP, ClaimFlags.FlagId.MOB_DAMAGE, ClaimFlags.FlagId.ALERTS, ClaimFlags.FlagId.PUBLIC_MODE, ClaimFlags.FlagId.ANIMAL_KILLING, ClaimFlags.FlagId.CHEST_ACCESS, ClaimFlags.FlagId.CROP_HARVEST};
    private static final ClaimFlags.FlagId[] PAGE_1 = new ClaimFlags.FlagId[]{ClaimFlags.FlagId.ITEM_USE, ClaimFlags.FlagId.ENTITY_INTERACT, ClaimFlags.FlagId.TRAMPLING, ClaimFlags.FlagId.FLUIDS, ClaimFlags.FlagId.PVP_ALL, ClaimFlags.FlagId.TREE_CHOPPING, ClaimFlags.FlagId.SHOW_WELCOME, ClaimFlags.FlagId.ANVIL_USE, ClaimFlags.FlagId.ENDER_PEARL, ClaimFlags.FlagId.SIGN_EDITING, ClaimFlags.FlagId.EFFECT_REGEN, ClaimFlags.FlagId.EFFECT_RESIST, ClaimFlags.FlagId.EFFECT_SPEED, ClaimFlags.FlagId.ALLOW_FLIGHT};
    private static final Map<UUID, PendingChat> pending = new HashMap<UUID, PendingChat>();
    private final class_1277 chest = new class_1277(54){

        public boolean method_5443(class_1657 p) {
            return true;
        }
    };
    private final Claim claim;
    private final class_3222 viewer;
    private final int page;
    private boolean awaitingDeleteConfirm = false;

    public ClaimMenuHandler(int syncId, class_1661 pInv, Claim claim, int page) {
        super(class_3917.field_17327, syncId);
        int col;
        int row;
        this.claim = claim;
        this.viewer = (class_3222)pInv.field_7546;
        this.page = page;
        for (row = 0; row < 6; ++row) {
            for (col = 0; col < 9; ++col) {
                int idx = col + row * 9;
                this.method_7621(new class_1735((class_1263)this.chest, idx, 8 + col * 18, 18 + row * 18){

                    public boolean method_7674(class_1657 p) {
                        return false;
                    }

                    public boolean method_7680(class_1799 s) {
                        return false;
                    }
                });
            }
        }
        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.method_7621(new class_1735((class_1263)pInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col2 = 0; col2 < 9; ++col2) {
            this.method_7621(new class_1735((class_1263)pInv, col2, 8 + col2 * 18, 198));
        }
        this.rebuild();
    }

    public Claim getClaim() {
        return this.claim;
    }

    public int getPage() {
        return this.page;
    }

    private void rebuild() {
        this.chest.method_5448();
        class_1799 bg = ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8157), (class_2561)class_2561.method_43470((String)" "));
        for (int i = 0; i < 54; ++i) {
            this.chest.method_5447(i, bg.method_7972());
        }
        this.chest.method_5447(4, ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8407), (class_2561)class_2561.method_43470((String)ClaimMenuHandler.truncate("Zona " + this.claim.sizeLabel() + " - " + this.claim.getOwnerName(), 30)).method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})));
        this.chest.method_5447(11, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8895), (class_2561)class_2561.method_43470((String)"Coordenadas").method_27692(class_124.field_1075)), List.of(class_2561.method_43470((String)("X=" + this.claim.getX() + " Y=" + this.claim.getY() + " Z=" + this.claim.getZ())).method_27692(class_124.field_1068))));
        this.chest.method_5447(13, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8360), (class_2561)class_2561.method_43470((String)"Due\u00f1o").method_27692(class_124.field_1075)), List.of(class_2561.method_43470((String)ClaimMenuHandler.truncate(this.claim.getOwnerName(), 35)).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}))));
        this.chest.method_5447(15, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8137), (class_2561)class_2561.method_43470((String)("Zona " + this.claim.sizeLabel())).method_27692(class_124.field_1054)), List.of(class_2561.method_43470((String)ClaimMenuHandler.truncate("Zona " + this.claim.sizeLabel() + " bloques", 35)).method_27692(class_124.field_1080), class_2561.method_43470((String)ClaimMenuHandler.truncate("Altura: +/-" + this.claim.getHeight(), 35)).method_27692(class_124.field_1080))));
        this.chest.method_5447(17, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8557), (class_2561)class_2561.method_43470((String)"Mundo").method_27692(class_124.field_1075)), List.of(class_2561.method_43470((String)ClaimMenuHandler.truncate(this.claim.getWorld(), 35)).method_27692(class_124.field_1080))));
        ClaimFlags f = this.claim.getFlags();
        ClaimFlags.FlagId[] ids = this.page == 0 ? PAGE_0 : PAGE_1;
        int[] slots = this.page == 0 ? FLAG_SLOTS_P0 : FLAG_SLOTS_P1;
        ClaimTier tier = this.claim.getTier();
        boolean isPaid = tier != null && tier.isPaid();
        for (int i = 0; i < ids.length; ++i) {
            ClaimFlags.FlagId id = ids[i];
            if (ClaimMenuHandler.isEffectFlag(id) && !isPaid) {
                this.chest.method_5447(slots[i], this.lockedEffectButton(id));
                continue;
            }
            this.chest.method_5447(slots[i], this.flagButton(id, f.get(id)));
        }
        this.chest.method_5447(38, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8575), (class_2561)class_2561.method_43470((String)("Miembros (" + this.claim.getMembers().size() + ")")).method_27692(class_124.field_1054)), this.buildMemberLore()));
        this.chest.method_5447(42, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8674), (class_2561)class_2561.method_43470((String)"A\u00f1adir miembro").method_27692(class_124.field_1060)), List.of(class_2561.method_43470((String)"Pide nombre por chat").method_27692(class_124.field_1080), class_2561.method_43470((String)"Clic para a\u00f1adir").method_27692(class_124.field_1080))));
        if (this.page > 0) {
            this.chest.method_5447(45, ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"<< P\u00e1gina anterior").method_27692(class_124.field_1075)));
        }
        if (this.awaitingDeleteConfirm) {
            this.chest.method_5447(46, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8626), (class_2561)class_2561.method_43470((String)"Confirmar eliminaci\u00f3n").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067})), List.of(class_2561.method_43470((String)"Haz clic de nuevo para confirmar").method_27692(class_124.field_1054), class_2561.method_43470((String)"O mueve el cursor fuera para cancelar").method_27692(class_124.field_1080))));
            this.chest.method_5447(47, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_19057), (class_2561)class_2561.method_43470((String)"Cancelar").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067})), List.of(class_2561.method_43470((String)"Cancela la eliminaci\u00f3n de la zona").method_27692(class_124.field_1080))));
        } else {
            this.chest.method_5447(46, ClaimMenuHandler.withLore(ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8077), (class_2561)class_2561.method_43470((String)"Eliminar zona").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067})), List.of(class_2561.method_43470((String)"Clic para iniciar eliminaci\u00f3n").method_27692(class_124.field_1054), class_2561.method_43470((String)"Devuelve la piedra al inv.").method_27692(class_124.field_1080))));
        }
        this.chest.method_5447(49, ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8691), (class_2561)class_2561.method_43470((String)"Cerrar").method_27692(class_124.field_1068)));
        this.chest.method_5447(52, ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8895), (class_2561)class_2561.method_43470((String)"Ver lista de zonas").method_27692(class_124.field_1075)));
        if (this.page == 0) {
            this.chest.method_5447(53, ClaimMenuHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"P\u00e1gina siguiente >>").method_27692(class_124.field_1075)));
        }
        this.method_7623();
    }

    private List<class_2561> buildMemberLore() {
        ArrayList<class_2561> lore = new ArrayList<class_2561>();
        if (this.claim.getMembers().isEmpty()) {
            lore.add((class_2561)class_2561.method_43470((String)"(sin miembros)").method_27692(class_124.field_1063));
            return lore;
        }
        int max = Math.min(5, this.claim.getMembers().size());
        for (int i = 0; i < max; ++i) {
            String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
            lore.add((class_2561)class_2561.method_43470((String)ClaimMenuHandler.truncate(" - " + n, 35)).method_27692(class_124.field_1068));
        }
        if (this.claim.getMembers().size() > max) {
            lore.add((class_2561)class_2561.method_43470((String)(" - ... y " + (this.claim.getMembers().size() - max) + " m\u00e1s")).method_27692(class_124.field_1080));
        }
        return lore;
    }

    private static boolean isEffectFlag(ClaimFlags.FlagId id) {
        return ClaimFlags.isPaidOnly(id);
    }

    private class_1799 lockedEffectButton(ClaimFlags.FlagId id) {
        class_1799 stack = new class_1799((class_1935)class_1802.field_8507);
        return ClaimMenuHandler.withLore(ClaimMenuHandler.withName(stack, (class_2561)class_2561.method_43470((String)(ClaimMenuHandler.effectName(id) + " [LOCKED]")).method_27692(class_124.field_1063)), List.of(class_2561.method_43470((String)"Solo disponible en zonas de pago").method_27692(class_124.field_1080), class_2561.method_43470((String)"Tiers 250x250, 300x300 o 500x500").method_27692(class_124.field_1063)));
    }

    private static String effectName(ClaimFlags.FlagId id) {
        return switch (id) {
            case ClaimFlags.FlagId.EFFECT_REGEN -> "Regeneraci\u00f3n pasiva";
            case ClaimFlags.FlagId.EFFECT_RESIST -> "Resistencia pasiva";
            case ClaimFlags.FlagId.EFFECT_SPEED -> "Velocidad pasiva";
            case ClaimFlags.FlagId.ALLOW_FLIGHT -> "Vuelo en zona";
            default -> "Perk pasivo";
        };
    }

    private class_1799 flagButton(ClaimFlags.FlagId id, boolean enabled) {
        class_1799 stack = new class_1799((class_1935)(enabled ? class_1802.field_8581 : class_1802.field_8879));
        class_5250 name = class_2561.method_43470((String)ClaimMenuHandler.flagDisplayName(id, enabled)).method_27695(new class_124[]{enabled ? class_124.field_1060 : class_124.field_1061, class_124.field_1067});
        String[] lore = ClaimMenuHandler.flagLore(id);
        return ClaimMenuHandler.withLore(ClaimMenuHandler.withName(stack, (class_2561)name), List.of(class_2561.method_43470((String)lore[0]).method_27692(class_124.field_1080), class_2561.method_43470((String)("Estado: " + (enabled ? "ACTIVO" : "INACTIVO") + " - " + lore[1])).method_27692(class_124.field_1080)));
    }

    private static String flagDisplayName(ClaimFlags.FlagId id, boolean on) {
        return switch (id) {
            default -> throw new MatchException(null, null);
            case ClaimFlags.FlagId.BUILDING -> {
                if (on) {
                    yield "Construir: BLOQUEADO [ON]";
                }
                yield "Construir: permitido [OFF]";
            }
            case ClaimFlags.FlagId.BREAKING -> {
                if (on) {
                    yield "Romper: BLOQUEADO [ON]";
                }
                yield "Romper: permitido [OFF]";
            }
            case ClaimFlags.FlagId.EXPLOSIONS -> {
                if (on) {
                    yield "Explosiones: BLOQUEADAS [ON]";
                }
                yield "Explosiones: permitidas [OFF]";
            }
            case ClaimFlags.FlagId.FIRE -> {
                if (on) {
                    yield "Fuego: BLOQUEADO [ON]";
                }
                yield "Fuego: permitido [OFF]";
            }
            case ClaimFlags.FlagId.MOB_SPAWN -> {
                if (on) {
                    yield "Mobs hostiles: BLOQUEADOS [ON]";
                }
                yield "Mobs hostiles: permit. [OFF]";
            }
            case ClaimFlags.FlagId.PVP -> {
                if (on) {
                    yield "PVP: BLOQUEADO [ON]";
                }
                yield "PVP: permitido [OFF]";
            }
            case ClaimFlags.FlagId.MOB_DAMAGE -> {
                if (on) {
                    yield "Da\u00f1o de mobs: BLOQUEADO [ON]";
                }
                yield "Da\u00f1o de mobs: permit. [OFF]";
            }
            case ClaimFlags.FlagId.ALERTS -> {
                if (on) {
                    yield "Alertas intrusos: ON [ON]";
                }
                yield "Alertas intrusos: OFF [OFF]";
            }
            case ClaimFlags.FlagId.ITEM_USE -> {
                if (on) {
                    yield "Usar items: BLOQUEADO [ON]";
                }
                yield "Usar items: permitido [OFF]";
            }
            case ClaimFlags.FlagId.ENTITY_INTERACT -> {
                if (on) {
                    yield "Entidades: BLOQUEADAS [ON]";
                }
                yield "Entidades: libres [OFF]";
            }
            case ClaimFlags.FlagId.TRAMPLING -> {
                if (on) {
                    yield "Cultivos: PROTEGIDOS [ON]";
                }
                yield "Cultivos: sin protec. [OFF]";
            }
            case ClaimFlags.FlagId.FLUIDS -> {
                if (on) {
                    yield "Fluidos: BLOQUEADOS [ON]";
                }
                yield "Fluidos: permitidos [OFF]";
            }
            case ClaimFlags.FlagId.PVP_ALL -> {
                if (on) {
                    yield "Zona PVP libre: ACTIVA [ON]";
                }
                yield "Zona PVP libre: inact. [OFF]";
            }
            case ClaimFlags.FlagId.TREE_CHOPPING -> {
                if (on) {
                    yield "\u00c1rboles: PROTEGIDOS [ON]";
                }
                yield "\u00c1rboles: se talan [OFF]";
            }
            case ClaimFlags.FlagId.PUBLIC_MODE -> {
                if (on) {
                    yield "Modo visita: ACTIVO [ON]";
                }
                yield "Modo visita: inactivo [OFF]";
            }
            case ClaimFlags.FlagId.SHOW_WELCOME -> {
                if (on) {
                    yield "Bienvenida custom: ON [ON]";
                }
                yield "Bienvenida custom: OFF [OFF]";
            }
            case ClaimFlags.FlagId.EFFECT_REGEN -> {
                if (on) {
                    yield "Regeneraci\u00f3n pasiva [ON]";
                }
                yield "Regeneraci\u00f3n pasiva [OFF]";
            }
            case ClaimFlags.FlagId.EFFECT_RESIST -> {
                if (on) {
                    yield "Resistencia pasiva [ON]";
                }
                yield "Resistencia pasiva [OFF]";
            }
            case ClaimFlags.FlagId.EFFECT_SPEED -> {
                if (on) {
                    yield "Velocidad pasiva [ON]";
                }
                yield "Velocidad pasiva [OFF]";
            }
            case ClaimFlags.FlagId.ALLOW_FLIGHT -> {
                if (on) {
                    yield "Vuelo en zona: ACTIVO [ON]";
                }
                yield "Vuelo en zona: inactivo [OFF]";
            }
            case ClaimFlags.FlagId.ANIMAL_KILLING -> {
                if (on) {
                    yield "Animales: PROTEGIDOS [ON]";
                }
                yield "Animales: se matan [OFF]";
            }
            case ClaimFlags.FlagId.CHEST_ACCESS -> {
                if (on) {
                    yield "Cofres: BLOQUEADOS [ON]";
                }
                yield "Cofres: acceso libre [OFF]";
            }
            case ClaimFlags.FlagId.CROP_HARVEST -> {
                if (on) {
                    yield "Cosecha: PROTEGIDA [ON]";
                }
                yield "Cosecha: libre [OFF]";
            }
            case ClaimFlags.FlagId.ANVIL_USE -> {
                if (on) {
                    yield "Yunques: BLOQUEADOS [ON]";
                }
                yield "Yunques: uso libre [OFF]";
            }
            case ClaimFlags.FlagId.ENDER_PEARL -> {
                if (on) {
                    yield "Ender pearl: BLOQUEADA [ON]";
                }
                yield "Ender pearl: permitida [OFF]";
            }
            case ClaimFlags.FlagId.SIGN_EDITING -> on ? "Letreros: BLOQUEADOS [ON]" : "Letreros: editables [OFF]";
        };
    }

    private static String[] flagLore(ClaimFlags.FlagId id) {
        String[] stringArray;
        switch (id) {
            default: {
                throw new MatchException(null, null);
            }
            case BUILDING: {
                String[] stringArray2 = new String[2];
                stringArray2[0] = "Intrusos no pueden colocar bloques";
                stringArray = stringArray2;
                stringArray2[1] = "Clic para cambiar";
                break;
            }
            case BREAKING: {
                String[] stringArray3 = new String[2];
                stringArray3[0] = "Intrusos no pueden romper nada";
                stringArray = stringArray3;
                stringArray3[1] = "Clic para cambiar";
                break;
            }
            case EXPLOSIONS: {
                String[] stringArray4 = new String[2];
                stringArray4[0] = "TNT y creepers no destruyen";
                stringArray = stringArray4;
                stringArray4[1] = "Clic para cambiar";
                break;
            }
            case FIRE: {
                String[] stringArray5 = new String[2];
                stringArray5[0] = "El fuego no se propaga aqu\u00ed";
                stringArray = stringArray5;
                stringArray5[1] = "Clic para cambiar";
                break;
            }
            case MOB_SPAWN: {
                String[] stringArray6 = new String[2];
                stringArray6[0] = "Zombies, skeletons no spawnean";
                stringArray = stringArray6;
                stringArray6[1] = "Clic para cambiar";
                break;
            }
            case PVP: {
                String[] stringArray7 = new String[2];
                stringArray7[0] = "Jugadores no pueden atacarse";
                stringArray = stringArray7;
                stringArray7[1] = "Clic para cambiar";
                break;
            }
            case MOB_DAMAGE: {
                String[] stringArray8 = new String[2];
                stringArray8[0] = "Los mobs no da\u00f1an a jugadores";
                stringArray = stringArray8;
                stringArray8[1] = "Clic para cambiar";
                break;
            }
            case ALERTS: {
                String[] stringArray9 = new String[2];
                stringArray9[0] = "Avisa al due\u00f1o cuando entran";
                stringArray = stringArray9;
                stringArray9[1] = "Clic para cambiar";
                break;
            }
            case ITEM_USE: {
                String[] stringArray10 = new String[2];
                stringArray10[0] = "Intrusos no pueden usar items";
                stringArray = stringArray10;
                stringArray10[1] = "Clic para cambiar";
                break;
            }
            case ENTITY_INTERACT: {
                String[] stringArray11 = new String[2];
                stringArray11[0] = "Intrusos no usan mobs/aldeanos";
                stringArray = stringArray11;
                stringArray11[1] = "Clic para cambiar";
                break;
            }
            case TRAMPLING: {
                String[] stringArray12 = new String[2];
                stringArray12[0] = "Intrusos no destruyen la tierra";
                stringArray = stringArray12;
                stringArray12[1] = "Clic para cambiar";
                break;
            }
            case FLUIDS: {
                String[] stringArray13 = new String[2];
                stringArray13[0] = "Nadie coloca agua ni lava aqu\u00ed";
                stringArray = stringArray13;
                stringArray13[1] = "Clic para cambiar";
                break;
            }
            case PVP_ALL: {
                String[] stringArray14 = new String[2];
                stringArray14[0] = "Todos se pueden atacar aqu\u00ed";
                stringArray = stringArray14;
                stringArray14[1] = "Clic para cambiar";
                break;
            }
            case TREE_CHOPPING: {
                String[] stringArray15 = new String[2];
                stringArray15[0] = "Intrusos no pueden talar \u00e1rboles";
                stringArray = stringArray15;
                stringArray15[1] = "Clic para cambiar";
                break;
            }
            case PUBLIC_MODE: {
                String[] stringArray16 = new String[2];
                stringArray16[0] = "Todos entran pero no modifican";
                stringArray = stringArray16;
                stringArray16[1] = "Clic para cambiar";
                break;
            }
            case SHOW_WELCOME: {
                String[] stringArray17 = new String[2];
                stringArray17[0] = "Mensaje personalizado al entrar";
                stringArray = stringArray17;
                stringArray17[1] = "Clic para editar";
                break;
            }
            case EFFECT_REGEN: {
                String[] stringArray18 = new String[2];
                stringArray18[0] = "Regenera vida a due\u00f1o y miembros";
                stringArray = stringArray18;
                stringArray18[1] = "Clic para cambiar";
                break;
            }
            case EFFECT_RESIST: {
                String[] stringArray19 = new String[2];
                stringArray19[0] = "Reduce da\u00f1o a due\u00f1o y miembros";
                stringArray = stringArray19;
                stringArray19[1] = "Clic para cambiar";
                break;
            }
            case EFFECT_SPEED: {
                String[] stringArray20 = new String[2];
                stringArray20[0] = "Da velocidad a due\u00f1o y miembros";
                stringArray = stringArray20;
                stringArray20[1] = "Clic para cambiar";
                break;
            }
            case ALLOW_FLIGHT: {
                String[] stringArray21 = new String[2];
                stringArray21[0] = "Due\u00f1o y miembros pueden volar";
                stringArray = stringArray21;
                stringArray21[1] = "Clic para cambiar";
                break;
            }
            case ANIMAL_KILLING: {
                String[] stringArray22 = new String[2];
                stringArray22[0] = "Intrusos no pueden matar animales";
                stringArray = stringArray22;
                stringArray22[1] = "Clic para cambiar";
                break;
            }
            case CHEST_ACCESS: {
                String[] stringArray23 = new String[2];
                stringArray23[0] = "Intrusos no abren cofres ni barriles";
                stringArray = stringArray23;
                stringArray23[1] = "Clic para cambiar";
                break;
            }
            case CROP_HARVEST: {
                String[] stringArray24 = new String[2];
                stringArray24[0] = "Intrusos no cosechan cultivos";
                stringArray = stringArray24;
                stringArray24[1] = "Clic para cambiar";
                break;
            }
            case ANVIL_USE: {
                String[] stringArray25 = new String[2];
                stringArray25[0] = "Intrusos no pueden usar yunques";
                stringArray = stringArray25;
                stringArray25[1] = "Clic para cambiar";
                break;
            }
            case ENDER_PEARL: {
                String[] stringArray26 = new String[2];
                stringArray26[0] = "Intrusos no se teletransportan";
                stringArray = stringArray26;
                stringArray26[1] = "Clic para cambiar";
                break;
            }
            case SIGN_EDITING: {
                String[] stringArray27 = new String[2];
                stringArray27[0] = "Intrusos no editan letreros";
                stringArray = stringArray27;
                stringArray27[1] = "Clic para cambiar";
            }
        }
        return stringArray;
    }

    private static class_1799 withName(class_1799 stack, class_2561 name) {
        stack.method_57379(class_9334.field_49631, name);
        return stack;
    }

    private static class_1799 withLore(class_1799 stack, List<class_2561> lore) {
        stack.method_57379(class_9334.field_49632, new class_9290(lore));
        return stack;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    public void method_7593(int slotIndex, int button, class_1713 action, class_1657 player) {
        ClaimFlags.FlagId clicked;
        if (slotIndex < 0 || slotIndex >= 54) {
            if (action == class_1713.field_7794) {
                return;
            }
            super.method_7593(slotIndex, button, action, player);
            return;
        }
        if (slotIndex == 45 && this.page > 0) {
            ClaimMenuHandler.open(this.viewer, this.claim, this.page - 1);
            return;
        }
        if (slotIndex == 53 && this.page == 0) {
            ClaimMenuHandler.open(this.viewer, this.claim, this.page + 1);
            return;
        }
        if (slotIndex == 46) {
            if (!this.awaitingDeleteConfirm) {
                this.awaitingDeleteConfirm = true;
                this.rebuild();
                this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[!] Haz clic de nuevo para confirmar.").method_27692(class_124.field_1054), true);
                return;
            }
            this.performDelete();
            return;
        }
        if (slotIndex == 47 && this.awaitingDeleteConfirm) {
            this.awaitingDeleteConfirm = false;
            this.rebuild();
            this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[i] Eliminaci\u00f3n cancelada.").method_27692(class_124.field_1075), true);
            return;
        }
        if (this.awaitingDeleteConfirm) {
            this.awaitingDeleteConfirm = false;
        }
        if ((clicked = this.slotToFlag(slotIndex)) != null) {
            ClaimTier tier;
            if (ClaimMenuHandler.isEffectFlag(clicked) && ((tier = this.claim.getTier()) == null || !tier.isPaid())) {
                this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[x] Solo disponible en zonas de pago.").method_27692(class_124.field_1061), true);
                return;
            }
            if (clicked == ClaimFlags.FlagId.SHOW_WELCOME) {
                if (button == 1) {
                    this.claim.getFlags().showWelcome = !this.claim.getFlags().showWelcome;
                    ClaimManager.getInstance().save();
                    this.rebuild();
                } else {
                    ClaimMenuHandler.requestEditWelcome(this.viewer, this.claim, this.page);
                    this.viewer.method_7346();
                }
                return;
            }
            this.claim.getFlags().toggle(clicked);
            ClaimManager.getInstance().save();
            this.rebuild();
            return;
        }
        if (slotIndex == 38) {
            this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[Claim] Miembros de la zona:").method_27692(class_124.field_1080), false);
            if (this.claim.getMembers().isEmpty()) {
                this.viewer.method_7353((class_2561)class_2561.method_43470((String)"  (sin miembros)").method_27692(class_124.field_1063), false);
            } else {
                for (int i = 0; i < this.claim.getMembers().size(); ++i) {
                    String n = i < this.claim.getMemberNames().size() ? this.claim.getMemberNames().get(i) : this.claim.getMembers().get(i).toString();
                    this.viewer.method_7353((class_2561)class_2561.method_43470((String)("  - " + n)).method_27692(class_124.field_1068), false);
                }
            }
            return;
        }
        if (slotIndex == 42) {
            ClaimMenuHandler.requestAddMember(this.viewer, this.claim, this.page);
            this.viewer.method_7346();
            return;
        }
        if (slotIndex == 49) {
            this.viewer.method_7346();
            return;
        }
        if (slotIndex == 52) {
            this.viewer.method_7346();
            this.viewer.method_5682().method_3734().method_44252(this.viewer.method_5671(), "claim list");
            return;
        }
    }

    private void performDelete() {
        class_2248 b;
        class_2338 centre;
        class_1937 world = this.viewer.method_37908();
        if (world.method_8320(centre = this.claim.getCenter()).method_26204() instanceof ClaimStoneBlock) {
            world.method_8651(centre, false, (class_1297)this.viewer);
        }
        ClaimManager.getInstance().removeClaim(world, centre);
        if (this.claim.getTierId() != null && (b = ModBlocks.byId(this.claim.getTierId())) != null) {
            class_1799 stack = new class_1799((class_1935)b);
            if (!this.viewer.method_31548().method_7394(stack)) {
                this.viewer.method_7328(stack, false);
            }
        }
        this.viewer.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Zona eliminada. Piedra devuelta a tu inventario.").method_27692(class_124.field_1060), false);
        this.viewer.method_7346();
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

    public class_1799 method_7601(class_1657 p, int slot) {
        return class_1799.field_8037;
    }

    public boolean method_7597(class_1657 p) {
        return true;
    }

    public static void open(class_3222 player, Claim claim, int page) {
        ClaimMenuHandler.open(player, claim, page, null);
    }

    public static void open(class_3222 player, Claim claim, int page, String customTitle) {
        int p = Math.max(0, Math.min(1, page));
        String title = customTitle != null ? ClaimMenuHandler.truncate(customTitle, 40) : ClaimMenuHandler.truncate("Zona " + claim.sizeLabel() + " - " + claim.getOwnerName(), 40);
        player.method_17355((class_3908)new class_747((syncId, pInv, plr) -> new ClaimMenuHandler(syncId, pInv, claim, p), (class_2561)class_2561.method_43470((String)title).method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})));
    }

    public static void requestAddMember(class_3222 player, Claim claim, int returnPage) {
        pending.put(player.method_5667(), new PendingChat(PendingType.ADD_MEMBER, claim.getClaimId(), returnPage));
        player.method_7353((class_2561)class_2561.method_43470((String)"[Claim] Escribe el nombre del jugador (o 'cancelar'):").method_27692(class_124.field_1054), false);
    }

    public static void requestEditWelcome(class_3222 player, Claim claim, int returnPage) {
        pending.put(player.method_5667(), new PendingChat(PendingType.EDIT_WELCOME, claim.getClaimId(), returnPage));
        player.method_7353((class_2561)class_2561.method_43470((String)"[Claim] Escribe tu bienvenida (max 60 chars) o 'cancelar':").method_27692(class_124.field_1054), false);
    }

    public static void registerChatListener() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID id = sender.method_5667();
            if (AdminClaimSubMenuHandler.hasPendingTransfer(id)) {
                String text = message.method_46291().getString().trim();
                UUID claimId = AdminClaimSubMenuHandler.popPendingTransfer(id);
                if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/")) {
                    sender.method_7353((class_2561)class_2561.method_43470((String)"[Claim] Cancelado.").method_27692(class_124.field_1080), false);
                    return false;
                }
                ClaimMenuHandler.handleAdminTransfer(sender, claimId, text);
                return false;
            }
            PendingChat p = pending.get(id);
            if (p == null) {
                return true;
            }
            String text = message.method_46291().getString().trim();
            pending.remove(id);
            if (text.equalsIgnoreCase("cancelar") || text.equalsIgnoreCase("cancel") || text.startsWith("/")) {
                sender.method_7353((class_2561)class_2561.method_43470((String)"[Claim] Cancelado.").method_27692(class_124.field_1080), false);
                return false;
            }
            Claim claim = ClaimMenuHandler.findClaimById(p.claimId());
            if (claim == null) {
                sender.method_7353((class_2561)class_2561.method_43470((String)"[x] La zona ya no existe.").method_27692(class_124.field_1061), false);
                return false;
            }
            switch (p.type().ordinal()) {
                case 0: {
                    ClaimMenuHandler.handleAddMember(sender, claim, text, p.returnPage());
                    break;
                }
                case 1: {
                    ClaimMenuHandler.handleEditWelcome(sender, claim, text, p.returnPage());
                }
            }
            return false;
        });
    }

    private static void handleAdminTransfer(class_3222 op, UUID claimId, String name) {
        String newOwnerName;
        UUID newOwnerId;
        Claim claim = ClaimMenuHandler.findClaimById(claimId);
        if (claim == null) {
            op.method_7353((class_2561)class_2561.method_43470((String)"[x] La zona ya no existe.").method_27692(class_124.field_1061), false);
            return;
        }
        class_3222 online = op.method_5682().method_3760().method_14566(name);
        if (online != null) {
            newOwnerId = online.method_5667();
            newOwnerName = online.method_5477().getString();
        } else {
            Optional profile = op.method_5682().method_3793().method_14515(name);
            if (profile.isEmpty()) {
                op.method_7353((class_2561)class_2561.method_43470((String)"[x] Jugador no encontrado.").method_27692(class_124.field_1061), false);
                return;
            }
            newOwnerId = ((GameProfile)profile.get()).getId();
            newOwnerName = ((GameProfile)profile.get()).getName();
        }
        claim.setOwner(newOwnerId, newOwnerName);
        claim.getMembers().clear();
        claim.getMemberNames().clear();
        ClaimManager.getInstance().save();
        op.method_7353((class_2561)class_2561.method_43470((String)("\u2714 Zona transferida a " + newOwnerName + ".")).method_27692(class_124.field_1060), false);
        class_5250 msg = class_2561.method_43470((String)"[!] Un administrador te transfiri\u00f3 una zona ").method_27692(class_124.field_1054).method_10852((class_2561)class_2561.method_43470((String)claim.sizeLabel()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)(" en X:" + claim.getX() + " Z:" + claim.getZ())).method_27692(class_124.field_1054));
        if (online != null) {
            online.method_7353((class_2561)msg, false);
        } else {
            ClaimManager.getInstance().queueMessage(newOwnerId, (class_2561)msg);
        }
    }

    private static void handleAddMember(class_3222 sender, Claim claim, String name, int page) {
        class_3324 pm = sender.method_5682().method_3760();
        class_3222 target = pm.method_14566(name);
        if (target == null) {
            sender.method_7353((class_2561)class_2561.method_43470((String)("[x] " + name + " no est\u00e1 en l\u00ednea.")).method_27692(class_124.field_1061), false);
            return;
        }
        if (claim.isOwner(target.method_5667())) {
            sender.method_7353((class_2561)class_2561.method_43470((String)"[x] Ese jugador ya es el due\u00f1o.").method_27692(class_124.field_1061), false);
            return;
        }
        claim.addMember(target.method_5667(), target.method_5477().getString());
        ClaimManager.getInstance().save();
        sender.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Jugador agregado como miembro.").method_27692(class_124.field_1060), false);
        target.method_7353((class_2561)class_2561.method_43470((String)("[Claim] Eres miembro de la zona de " + sender.method_5477().getString())).method_27692(class_124.field_1075), false);
        ClaimMenuHandler.open(sender, claim, page);
    }

    private static void handleEditWelcome(class_3222 sender, Claim claim, String text, int page) {
        if (text.length() > 60) {
            text = text.substring(0, 60);
        }
        claim.getFlags().welcomeMessage = text;
        claim.getFlags().showWelcome = !text.isBlank();
        ClaimManager.getInstance().save();
        sender.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Bienvenida guardada.").method_27692(class_124.field_1060), false);
        ClaimMenuHandler.open(sender, claim, page);
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
        EDIT_WELCOME;

    }
}

