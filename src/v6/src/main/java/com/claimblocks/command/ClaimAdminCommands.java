/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.minecraft.class_124
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_5250
 */
package com.claimblocks.command;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.gui.AdminPanelHandler;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.class_124;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_5250;

public final class ClaimAdminCommands {
    private static final SuggestionProvider<class_2168> GLOBAL_FLAG_NAMES = (ctx, builder) -> {
        String s = builder.getRemaining().toLowerCase();
        for (String n : new String[]{"globalPVP", "globalMobGriefing", "globalFireSpread"}) {
            if (!n.toLowerCase().startsWith(s)) continue;
            builder.suggest(n);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<class_2168> ON_OFF = (ctx, builder) -> {
        String s = builder.getRemaining().toLowerCase();
        for (String n : new String[]{"on", "off"}) {
            if (!n.startsWith(s)) continue;
            builder.suggest(n);
        }
        return builder.buildFuture();
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)"claimadmin").requires(s -> s.method_9259(2))).executes(ClaimAdminCommands::openPanel)).then(class_2170.method_9247((String)"list").executes(ClaimAdminCommands::list))).then(class_2170.method_9247((String)"bypass").executes(ClaimAdminCommands::toggleBypass))).then(class_2170.method_9247((String)"stats").executes(ClaimAdminCommands::stats))).then(class_2170.method_9247((String)"globalflag").then(class_2170.method_9244((String)"flag", (ArgumentType)StringArgumentType.word()).suggests(GLOBAL_FLAG_NAMES).then(class_2170.method_9244((String)"value", (ArgumentType)StringArgumentType.word()).suggests(ON_OFF).executes(ClaimAdminCommands::globalFlag))))));
    }

    private static int openPanel(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        AdminPanelHandler.open(p, 0);
        return 1;
    }

    private static int list(CommandContext<class_2168> ctx) {
        List<Claim> all = ClaimManager.getInstance().getAllClaims();
        if (all.isEmpty()) {
            ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"[i] No hay zonas activas en el servidor.").method_27692(class_124.field_1075), false);
            return 0;
        }
        for (Claim c : all) {
            class_5250 line = class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)c.getOwnerName()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)" | ").method_27692(class_124.field_1080)).method_10852((class_2561)class_2561.method_43470((String)c.sizeLabel()).method_27692(class_124.field_1054)).method_10852((class_2561)class_2561.method_43470((String)" | ").method_27692(class_124.field_1080)).method_10852((class_2561)class_2561.method_43470((String)("X:" + c.getX() + " Z:" + c.getZ())).method_27692(class_124.field_1068)).method_10852((class_2561)class_2561.method_43470((String)" | ").method_27692(class_124.field_1080)).method_10852((class_2561)class_2561.method_43470((String)("Dim:" + c.getWorld())).method_27692(class_124.field_1062));
            ((class_2168)ctx.getSource()).method_9226(() -> ClaimAdminCommands.lambda$list$5((class_2561)line), false);
        }
        return all.size();
    }

    private static int toggleBypass(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        boolean now = ClaimManager.getInstance().toggleBypass(p.method_5667());
        if (now) {
            p.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Modo bypass activado. Las zonas no te afectan.").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067}), false);
        } else {
            p.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Modo bypass desactivado.").method_27692(class_124.field_1060), false);
        }
        return 1;
    }

    private static int stats(CommandContext<class_2168> ctx) {
        List<Claim> all = ClaimManager.getInstance().getAllClaims();
        HashSet<UUID> uniqueOwners = new HashSet<UUID>();
        Claim biggest = null;
        Claim oldest = null;
        int paid = 0;
        for (Claim c : all) {
            ClaimTier t;
            uniqueOwners.add(c.getOwnerUUID());
            if (biggest == null || c.getRadius() > biggest.getRadius()) {
                biggest = c;
            }
            if (oldest == null || c.getCreatedAt() < oldest.getCreatedAt()) {
                oldest = c;
            }
            if ((t = c.getTier()) == null || !t.isPaid()) continue;
            ++paid;
        }
        class_2168 src = (class_2168)ctx.getSource();
        src.method_9226(() -> class_2561.method_43470((String)"--- Estad\u00edsticas de ClaimBlocks ---").method_27692(class_124.field_1065), false);
        src.method_9226(() -> ClaimAdminCommands.infoLine("Total de zonas activas: " + all.size()), false);
        src.method_9226(() -> ClaimAdminCommands.infoLine("Jugadores con zona: " + uniqueOwners.size()), false);
        if (biggest != null) {
            Claim b = biggest;
            src.method_9226(() -> ClaimAdminCommands.infoLine("Zona m\u00e1s grande: " + b.sizeLabel() + " de " + b.getOwnerName()), false);
        }
        if (oldest != null) {
            Claim o = oldest;
            String when = o.getCreatedAt() == 0L ? "(legacy)" : new Date(o.getCreatedAt()).toString();
            src.method_9226(() -> class_2561.method_43470((String)"\u2714 Zona m\u00e1s antigua: ").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)(o.sizeLabel() + " de " + o.getOwnerName())).method_27692(class_124.field_1068)).method_10852((class_2561)class_2561.method_43470((String)(" (" + when + ")")).method_27692(class_124.field_1063)), false);
        }
        int paidCount = paid;
        src.method_9226(() -> class_2561.method_43470((String)("\u2714 Zonas de pago activas: " + paidCount)).method_27695(new class_124[]{class_124.field_1065, class_124.field_1067}), false);
        src.method_9226(() -> class_2561.method_43470((String)"-----------------------------------").method_27692(class_124.field_1065), false);
        return 1;
    }

    private static class_2561 infoLine(String text) {
        return class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)text).method_27692(class_124.field_1075));
    }

    private static int globalFlag(CommandContext<class_2168> ctx) {
        String flag = StringArgumentType.getString(ctx, (String)"flag");
        String value = StringArgumentType.getString(ctx, (String)"value").toLowerCase();
        if (!(flag.equals("globalPVP") || flag.equals("globalMobGriefing") || flag.equals("globalFireSpread"))) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)("[x] Flag global desconocida: " + flag)).method_27692(class_124.field_1061));
            return 0;
        }
        boolean on = value.equals("on") || value.equals("true");
        GlobalFlags.getInstance().set(flag, on, ((class_2168)ctx.getSource()).method_9211());
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 Flag global ").method_27695(new class_124[]{class_124.field_1065, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)flag).method_27692(class_124.field_1054)).method_10852((class_2561)class_2561.method_43470((String)" establecida a ").method_27692(class_124.field_1065)).method_10852((class_2561)class_2561.method_43470((String)(on ? "[ON]" : "[OFF]")).method_27695(new class_124[]{on ? class_124.field_1060 : class_124.field_1061, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)".").method_27692(class_124.field_1065)), true);
        class_5250 bcast = class_2561.method_43470((String)"[!] Un administrador cambi\u00f3 una configuraci\u00f3n global del servidor.").method_27692(class_124.field_1054);
        ((class_2168)ctx.getSource()).method_9211().method_3760().method_14571().forEach(arg_0 -> ClaimAdminCommands.lambda$globalFlag$14((class_2561)bcast, arg_0));
        return 1;
    }

    private static /* synthetic */ void lambda$globalFlag$14(class_2561 bcast, class_3222 p) {
        p.method_7353(bcast, false);
    }

    private static /* synthetic */ class_2561 lambda$list$5(class_2561 line) {
        return line;
    }
}

