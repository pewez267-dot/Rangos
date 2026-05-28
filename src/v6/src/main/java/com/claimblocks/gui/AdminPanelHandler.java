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
 *  net.minecraft.class_2248
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_3908
 *  net.minecraft.class_3917
 *  net.minecraft.class_5250
 *  net.minecraft.class_747
 *  net.minecraft.class_9290
 *  net.minecraft.class_9334
 */
package com.claimblocks.gui;

import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.AdminClaimSubMenuHandler;
import com.claimblocks.gui.AdminGlobalFlagsHandler;
import java.util.List;
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
import net.minecraft.class_2248;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3908;
import net.minecraft.class_3917;
import net.minecraft.class_5250;
import net.minecraft.class_747;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

public class AdminPanelHandler
extends class_1703 {
    public static final int SIZE = 54;
    private static final int CLAIMS_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_STATS = 46;
    private static final int SLOT_GFLAG = 47;
    private static final int SLOT_BYPASS = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;
    private final class_1277 inv = new class_1277(54){

        public boolean method_5443(class_1657 p) {
            return true;
        }
    };
    private final class_3222 viewer;
    private final int page;
    private final List<Claim> claims;

    public AdminPanelHandler(int syncId, class_1661 pInv, int page) {
        super(class_3917.field_17327, syncId);
        int col;
        int row;
        this.viewer = (class_3222)pInv.field_7546;
        this.page = page;
        this.claims = ClaimManager.getInstance().getAllClaims();
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

    private void rebuild() {
        this.inv.method_5448();
        class_1799 bg = AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8157), (class_2561)class_2561.method_43470((String)" "));
        for (int i = 0; i < 54; ++i) {
            this.inv.method_5447(i, bg.method_7972());
        }
        int start = this.page * 45;
        int end = Math.min(start + 45, this.claims.size());
        for (int i = start; i < end; ++i) {
            Claim c = this.claims.get(i);
            int slot = i - start;
            this.inv.method_5447(slot, AdminPanelHandler.claimItem(c));
        }
        if (this.page > 0) {
            this.inv.method_5447(45, AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"<< P\u00e1gina anterior").method_27692(class_124.field_1075)));
        }
        this.inv.method_5447(46, AdminPanelHandler.withLore(AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8529), (class_2561)class_2561.method_43470((String)"Estad\u00edsticas").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})), List.of(class_2561.method_43470((String)"Resumen del servidor").method_27692(class_124.field_1080))));
        this.inv.method_5447(47, AdminPanelHandler.withLore(AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8865), (class_2561)class_2561.method_43470((String)"Flags Globales").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})), List.of(class_2561.method_43470((String)"PVP / Mob griefing / Fire").method_27692(class_124.field_1080))));
        boolean bypassing = ClaimManager.getInstance().isBypassing(this.viewer.method_5667());
        this.inv.method_5447(48, AdminPanelHandler.withLore(AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8845), (class_2561)class_2561.method_43470((String)("Modo Bypass: " + (bypassing ? "ON" : "OFF"))).method_27695(new class_124[]{bypassing ? class_124.field_1060 : class_124.field_1061, class_124.field_1067})), List.of(class_2561.method_43470((String)"Ignorar protecciones de zonas").method_27692(class_124.field_1080))));
        this.inv.method_5447(49, AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8077), (class_2561)class_2561.method_43470((String)"Cerrar panel").method_27692(class_124.field_1068)));
        if (end < this.claims.size()) {
            this.inv.method_5447(53, AdminPanelHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"P\u00e1gina siguiente >>").method_27692(class_124.field_1075)));
        }
        this.method_7623();
    }

    private static class_1799 claimItem(Claim c) {
        ClaimTier tier = c.getTier();
        class_2248 block = tier != null ? ModBlocks.byId(tier.id) : null;
        class_1799 stack = block != null ? new class_1799((class_1935)block.method_8389()) : new class_1799((class_1935)class_1802.field_8407);
        class_5250 name = class_2561.method_43470((String)(c.getOwnerName() + " - " + c.sizeLabel())).method_27695(new class_124[]{class_124.field_1065, class_124.field_1067});
        return AdminPanelHandler.withLore(AdminPanelHandler.withName(stack, (class_2561)name), List.of(class_2561.method_43470((String)("Posici\u00f3n: X:" + c.getX() + " Z:" + c.getZ())).method_27692(class_124.field_1080), class_2561.method_43470((String)("Dimensi\u00f3n: " + c.getWorld())).method_27692(class_124.field_1062), class_2561.method_43470((String)"Clic para gestionar este claim").method_27692(class_124.field_1054)));
    }

    private static class_1799 withName(class_1799 s, class_2561 t) {
        s.method_57379(class_9334.field_49631, t);
        return s;
    }

    private static class_1799 withLore(class_1799 s, List<class_2561> lore) {
        s.method_57379(class_9334.field_49632, new class_9290(lore));
        return s;
    }

    public void method_7593(int slot, int button, class_1713 action, class_1657 player) {
        if (slot < 0 || slot >= 54) {
            if (action == class_1713.field_7794) {
                return;
            }
            super.method_7593(slot, button, action, player);
            return;
        }
        if (slot == 45 && this.page > 0) {
            AdminPanelHandler.open(this.viewer, this.page - 1);
            return;
        }
        if (slot == 53) {
            int max = (this.claims.size() - 1) / 45;
            if (this.page < max) {
                AdminPanelHandler.open(this.viewer, this.page + 1);
            }
            return;
        }
        if (slot == 49) {
            this.viewer.method_7346();
            return;
        }
        if (slot == 46) {
            this.viewer.method_7346();
            this.viewer.method_5682().method_3734().method_44252(this.viewer.method_5671(), "claimadmin stats");
            return;
        }
        if (slot == 47) {
            AdminGlobalFlagsHandler.open(this.viewer);
            return;
        }
        if (slot == 48) {
            ClaimManager.getInstance().toggleBypass(this.viewer.method_5667());
            this.rebuild();
            return;
        }
        int idx = this.page * 45 + slot;
        if (idx < this.claims.size()) {
            AdminClaimSubMenuHandler.open(this.viewer, this.claims.get(idx).getClaimId());
        }
    }

    public class_1799 method_7601(class_1657 p, int s) {
        return class_1799.field_8037;
    }

    public boolean method_7597(class_1657 p) {
        return true;
    }

    public static void open(class_3222 player, int page) {
        int p = Math.max(0, page);
        player.method_17355((class_3908)new class_747((syncId, pInv, plr) -> new AdminPanelHandler(syncId, pInv, p), (class_2561)class_2561.method_43470((String)"Panel de Administraci\u00f3n").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})));
    }

    public static Claim findClaim(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (!c.getClaimId().equals(id)) continue;
            return c;
        }
        return null;
    }
}

