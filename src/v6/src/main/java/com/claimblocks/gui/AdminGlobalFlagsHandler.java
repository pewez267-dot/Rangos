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

import com.claimblocks.data.GlobalFlags;
import com.claimblocks.gui.AdminPanelHandler;
import java.util.List;
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
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3908;
import net.minecraft.class_3917;
import net.minecraft.class_5250;
import net.minecraft.class_747;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

public class AdminGlobalFlagsHandler
extends class_1703 {
    public static final int SIZE = 54;
    private static final int SLOT_PVP = 11;
    private static final int SLOT_GRIEF = 13;
    private static final int SLOT_FIRE = 15;
    private static final int SLOT_BACK = 22;
    private final class_1277 inv = new class_1277(54){

        public boolean method_5443(class_1657 p) {
            return true;
        }
    };
    private final class_3222 viewer;

    public AdminGlobalFlagsHandler(int syncId, class_1661 pInv) {
        super(class_3917.field_17327, syncId);
        int col;
        int row;
        this.viewer = (class_3222)pInv.field_7546;
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
        class_1799 bg = AdminGlobalFlagsHandler.withName(new class_1799((class_1935)class_1802.field_8871), (class_2561)class_2561.method_43470((String)" "));
        for (int i = 0; i < 54; ++i) {
            this.inv.method_5447(i, bg.method_7972());
        }
        GlobalFlags g = GlobalFlags.getInstance();
        this.inv.method_5447(11, AdminGlobalFlagsHandler.flagButton("PVP global", g.globalPVP, "Permite PVP fuera de claims"));
        this.inv.method_5447(13, AdminGlobalFlagsHandler.flagButton("Mob griefing global", g.globalMobGriefing, "Mobs destruyen bloques fuera de claims"));
        this.inv.method_5447(15, AdminGlobalFlagsHandler.flagButton("Propagaci\u00f3n de fuego", g.globalFireSpread, "Fire spread global gamerule"));
        this.inv.method_5447(22, AdminGlobalFlagsHandler.withName(new class_1799((class_1935)class_1802.field_8107), (class_2561)class_2561.method_43470((String)"Volver al panel").method_27692(class_124.field_1075)));
        this.method_7623();
    }

    private static class_1799 flagButton(String name, boolean on, String desc) {
        class_1799 stack = new class_1799((class_1935)(on ? class_1802.field_8581 : class_1802.field_8879));
        class_5250 title = class_2561.method_43470((String)(name + " " + (on ? "[ON]" : "[OFF]"))).method_27695(new class_124[]{on ? class_124.field_1060 : class_124.field_1061, class_124.field_1067});
        return AdminGlobalFlagsHandler.withLore(AdminGlobalFlagsHandler.withName(stack, (class_2561)title), List.of(class_2561.method_43470((String)desc).method_27692(class_124.field_1080), class_2561.method_43470((String)("Estado: " + (on ? "ACTIVO" : "INACTIVO") + " - Clic para cambiar")).method_27692(class_124.field_1080)));
    }

    public void method_7593(int slot, int button, class_1713 action, class_1657 player) {
        if (slot < 0 || slot >= 54) {
            if (action == class_1713.field_7794) {
                return;
            }
            super.method_7593(slot, button, action, player);
            return;
        }
        if (slot == 22) {
            AdminPanelHandler.open(this.viewer, 0);
            return;
        }
        GlobalFlags g = GlobalFlags.getInstance();
        String name = null;
        boolean newVal = false;
        if (slot == 11) {
            name = "globalPVP";
            boolean bl = newVal = !g.globalPVP;
        }
        if (slot == 13) {
            name = "globalMobGriefing";
            boolean bl = newVal = !g.globalMobGriefing;
        }
        if (slot == 15) {
            name = "globalFireSpread";
            boolean bl = newVal = !g.globalFireSpread;
        }
        if (name != null) {
            g.set(name, newVal, this.viewer.method_5682());
            class_5250 bcast = class_2561.method_43470((String)"[!] Un administrador cambi\u00f3 una configuraci\u00f3n global del servidor.").method_27692(class_124.field_1054);
            this.viewer.method_5682().method_3760().method_14571().forEach(arg_0 -> AdminGlobalFlagsHandler.lambda$onSlotClick$0((class_2561)bcast, arg_0));
            this.rebuild();
        }
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

    public static void open(class_3222 player) {
        player.method_17355((class_3908)new class_747((syncId, pInv, plr) -> new AdminGlobalFlagsHandler(syncId, pInv), (class_2561)class_2561.method_43470((String)"Flags Globales").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067})));
    }

    private static /* synthetic */ void lambda$onSlotClick$0(class_2561 bcast, class_3222 p) {
        p.method_7353(bcast, false);
    }
}

