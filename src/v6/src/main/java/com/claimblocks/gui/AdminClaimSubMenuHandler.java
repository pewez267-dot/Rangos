/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1263
 *  net.minecraft.class_1277
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_1713
 *  net.minecraft.class_1735
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_1937
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_2709
 *  net.minecraft.class_2902$class_2903
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.class_3908
 *  net.minecraft.class_3917
 *  net.minecraft.class_5250
 *  net.minecraft.class_747
 *  net.minecraft.class_9290
 *  net.minecraft.class_9334
 */
package com.claimblocks.gui;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.gui.AdminPanelHandler;
import com.claimblocks.gui.ClaimMenuHandler;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.class_124;
import net.minecraft.class_1263;
import net.minecraft.class_1277;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2709;
import net.minecraft.class_2902;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3908;
import net.minecraft.class_3917;
import net.minecraft.class_5250;
import net.minecraft.class_747;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

public class AdminClaimSubMenuHandler
extends class_1703 {
    public static final int SIZE = 54;
    private static final int SLOT_TELEPORT = 11;
    private static final int SLOT_FLAGS = 12;
    private static final int SLOT_DELETE = 13;
    private static final int SLOT_TRANSFER = 15;
    private static final int SLOT_BACK = 22;
    private static final Map<UUID, UUID> pendingTransfers = new HashMap<UUID, UUID>();
    private final class_1277 inv = new class_1277(54){

        public boolean method_5443(class_1657 p) {
            return true;
        }
    };
    private final class_3222 viewer;
    private final UUID claimId;
    private boolean awaitingDeleteConfirm = false;

    public AdminClaimSubMenuHandler(int syncId, class_1661 pInv, UUID claimId) {
        super(class_3917.field_17327, syncId);
        int col;
        int row;
        this.viewer = (class_3222)pInv.field_7546;
        this.claimId = claimId;
        for (row = 0; row < 6; ++row) {
            for (col = 0; col < 9; ++col) {
                int idx = col + row * 9;
                this.method_7621(new class_1735((class_1263)this.inv, idx, 8 + col * 18, 18 + row * 18){

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

    private Claim claim() {
        return AdminPanelHandler.findClaim(this.claimId);
    }

    private void rebuild() {
        this.inv.method_5448();
        class_1799 bg = AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8871), (class_2561)class_2561.method_43470((String)" "));
        for (int i = 0; i < 54; ++i) {
            this.inv.method_5447(i, bg.method_7972());
        }
        Claim c = this.claim();
        if (c == null) {
            return;
        }
        String owner = c.getOwnerName();
        this.inv.method_5447(11, AdminClaimSubMenuHandler.withLore(AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8251), (class_2561)class_2561.method_43470((String)"Teleportar al claim").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067})), List.of(class_2561.method_43470((String)("Te lleva al centro del claim de " + owner)).method_27692(class_124.field_1080))));
        this.inv.method_5447(12, AdminClaimSubMenuHandler.withLore(AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8865), (class_2561)class_2561.method_43470((String)"Ver y editar flags").method_27695(new class_124[]{class_124.field_1054, class_124.field_1067})), List.of(class_2561.method_43470((String)"Abre el men\u00fa de flags de este claim").method_27692(class_124.field_1080))));
        if (this.awaitingDeleteConfirm) {
            this.inv.method_5447(13, AdminClaimSubMenuHandler.withLore(AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8626), (class_2561)class_2561.method_43470((String)"\u00bfConfirmar eliminaci\u00f3n?").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067})), List.of(class_2561.method_43470((String)("Esto eliminar\u00e1 la zona de " + owner)).method_27692(class_124.field_1054), class_2561.method_43470((String)"El bloque NO se devuelve al due\u00f1o").method_27692(class_124.field_1061), class_2561.method_43470((String)"Clic de nuevo para confirmar").method_27692(class_124.field_1080))));
        } else {
            this.inv.method_5447(13, AdminClaimSubMenuHandler.withLore(AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8077), (class_2561)class_2561.method_43470((String)"Eliminar este claim").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067})), List.of(class_2561.method_43470((String)("Elimina la zona de " + owner)).method_27692(class_124.field_1054), class_2561.method_43470((String)"Clic para pedir confirmaci\u00f3n").method_27692(class_124.field_1080))));
        }
        this.inv.method_5447(15, AdminClaimSubMenuHandler.withLore(AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8407), (class_2561)class_2561.method_43470((String)"Transferir claim").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067})), List.of(class_2561.method_43470((String)"Cambia el due\u00f1o de esta zona").method_27692(class_124.field_1080))));
        this.inv.method_5447(22, AdminClaimSubMenuHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"Volver al panel").method_27692(class_124.field_1075)));
        this.method_7623();
    }

    public void method_7593(int slot, int button, class_1713 action, class_1657 player) {
        if (slot < 0 || slot >= 54) {
            if (action == class_1713.field_7794) {
                return;
            }
            super.method_7593(slot, button, action, player);
            return;
        }
        Claim c = this.claim();
        if (c == null) {
            this.viewer.method_7346();
            return;
        }
        if (slot != 13 && this.awaitingDeleteConfirm) {
            this.awaitingDeleteConfirm = false;
        }
        if (slot == 22) {
            AdminPanelHandler.open(this.viewer, 0);
            return;
        }
        if (slot == 11) {
            this.teleportToClaim(c);
            return;
        }
        if (slot == 12) {
            String title = "[Admin] Flags de " + c.getOwnerName() + " - " + c.sizeLabel();
            ClaimMenuHandler.open(this.viewer, c, 0, title);
            return;
        }
        if (slot == 13) {
            if (!this.awaitingDeleteConfirm) {
                this.awaitingDeleteConfirm = true;
                this.rebuild();
                return;
            }
            this.adminDelete(c);
            return;
        }
        if (slot == 15) {
            this.startTransfer(c);
            return;
        }
        this.rebuild();
    }

    private void teleportToClaim(Claim c) {
        class_3218 world = null;
        for (class_3218 w : this.viewer.method_5682().method_3738()) {
            if (!w.method_27983().method_29177().toString().equals(c.getWorld())) continue;
            world = w;
            break;
        }
        if (world == null) {
            this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[x] No se pudo encontrar la dimensi\u00f3n.").method_27692(class_124.field_1061), false);
            this.viewer.method_7346();
            return;
        }
        int topY = world.method_8624(class_2902.class_2903.field_13203, c.getX(), c.getZ());
        class_2338 target = new class_2338(c.getX(), topY, c.getZ());
        this.viewer.method_48105(world, (double)target.method_10263() + 0.5, (double)target.method_10264(), (double)target.method_10260() + 0.5, EnumSet.noneOf(class_2709.class), this.viewer.method_36454(), this.viewer.method_36455());
        this.viewer.method_7353((class_2561)class_2561.method_43470((String)("\u2714 Teletransportado a la zona de " + c.getOwnerName() + ".")).method_27692(class_124.field_1060), false);
        this.viewer.method_7346();
    }

    private void adminDelete(Claim c) {
        class_2338 pos;
        String ownerName = c.getOwnerName();
        UUID ownerId = c.getOwnerUUID();
        class_3218 world = null;
        for (class_3218 w : this.viewer.method_5682().method_3738()) {
            if (!w.method_27983().method_29177().toString().equals(c.getWorld())) continue;
            world = w;
            break;
        }
        if (world != null && world.method_8320(pos = c.getCenter()).method_26204() instanceof ClaimStoneBlock) {
            world.method_8652(pos, class_2246.field_10124.method_9564(), 3);
        }
        ClaimManager.getInstance().removeClaim((class_1937)world, c.getCenter());
        this.viewer.method_7353((class_2561)class_2561.method_43470((String)("\u2714 Zona de " + ownerName + " eliminada por admin.")).method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}), false);
        class_3222 owner = this.viewer.method_5682().method_3760().method_14602(ownerId);
        class_5250 msg = class_2561.method_43470((String)"[!] Un administrador elimin\u00f3 tu zona ").method_27692(class_124.field_1054).method_10852((class_2561)class_2561.method_43470((String)c.sizeLabel()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)(" en X:" + c.getX() + " Z:" + c.getZ())).method_27692(class_124.field_1054));
        if (owner != null) {
            owner.method_7353((class_2561)msg, false);
        } else {
            ClaimManager.getInstance().queueMessage(ownerId, (class_2561)msg);
        }
        this.viewer.method_7346();
    }

    private void startTransfer(Claim c) {
        pendingTransfers.put(this.viewer.method_5667(), c.getClaimId());
        this.viewer.method_7353((class_2561)class_2561.method_43470((String)"[i] Escribe el nombre del nuevo due\u00f1o en el chat.").method_27692(class_124.field_1075), false);
        this.viewer.method_7353((class_2561)class_2561.method_43470((String)"    Escribe 'cancelar' para abortar.").method_27692(class_124.field_1080), false);
        this.viewer.method_7346();
    }

    public static UUID popPendingTransfer(UUID opId) {
        return pendingTransfers.remove(opId);
    }

    public static boolean hasPendingTransfer(UUID opId) {
        return pendingTransfers.containsKey(opId);
    }

    public class_1799 method_7601(class_1657 p, int s) {
        return class_1799.field_8037;
    }

    public boolean method_7597(class_1657 p) {
        return true;
    }

    private static class_1799 withName(class_1799 s, class_2561 t) {
        s.method_57379(class_9334.field_49631, t);
        return s;
    }

    private static class_1799 withLore(class_1799 s, List<class_2561> lore) {
        s.method_57379(class_9334.field_49632, new class_9290(lore));
        return s;
    }

    public static void open(class_3222 player, UUID claimId) {
        Claim c = AdminPanelHandler.findClaim(claimId);
        if (c == null) {
            player.method_7353((class_2561)class_2561.method_43470((String)"[x] La zona ya no existe.").method_27692(class_124.field_1061), false);
            return;
        }
        String title = "Admin - " + c.getOwnerName() + " " + c.sizeLabel();
        if (title.length() > 40) {
            title = title.substring(0, 37) + "...";
        }
        String t = title;
        player.method_17355((class_3908)new class_747((syncId, pInv, plr) -> new AdminClaimSubMenuHandler(syncId, pInv, claimId), (class_2561)class_2561.method_43470((String)t).method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})));
    }
}

