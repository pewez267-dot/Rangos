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
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1799
 *  net.minecraft.class_1935
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2186
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 */
package com.claimblocks.command;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.class_124;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2186;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_5250;

public final class ClaimCommands {
    private static final SuggestionProvider<class_2168> CLAIMSTONE_IDS = (context, builder) -> {
        String start = builder.getRemaining().toLowerCase();
        for (ClaimTier t : ClaimTier.VALUES) {
            if (!t.id.startsWith(start)) continue;
            builder.suggest(t.id);
        }
        return builder.buildFuture();
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> dispatcher.register(
            class_2170.method_9247("claim")
                // /claim sin sub-comando: imprime ayuda.
                .executes(ClaimCommands::help)
                // ----- Comandos para CUALQUIER JUGADOR -----
                .then(class_2170.method_9247("remove").executes(ClaimCommands::remove))
                .then(class_2170.method_9247("menu").executes(ClaimCommands::menu))
                .then(class_2170.method_9247("list").executes(ClaimCommands::list))
                .then(class_2170.method_9247("info").executes(ClaimCommands::info))
                .then(class_2170.method_9247("help").executes(ClaimCommands::help))
                .then(class_2170.method_9247("ban")
                    .then(class_2170.method_9244("jugador", class_2186.method_9305())
                        .executes(ClaimCommands::ban)))
                .then(class_2170.method_9247("unban")
                    .then(class_2170.method_9244("jugador", class_2186.method_9305())
                        .executes(ClaimCommands::unban)))
                .then(class_2170.method_9247("transfer")
                    .then(class_2170.method_9244("jugador", class_2186.method_9305())
                        .executes(ClaimCommands::transfer)))
                .then(class_2170.method_9247("removemember")
                    .then(class_2170.method_9244("jugador", class_2186.method_9305())
                        .executes(ClaimCommands::removeMember)))
                // ----- Comandos SOLO para OPERADORES (level 2+) -----
                .then(class_2170.method_9247("give")
                    .requires(s -> s.method_9259(2))
                    .then(class_2170.method_9244("jugador", class_2186.method_9308())
                        .then(class_2170.method_9244("id", StringArgumentType.word())
                            .suggests(CLAIMSTONE_IDS)
                            .executes(ClaimCommands::give))))
                .then(class_2170.method_9247("clear")
                    .requires(s -> s.method_9259(2))
                    .then(class_2170.method_9244("jugador", class_2186.method_9305())
                        .executes(ClaimCommands::clear)))
        ));
    }

    private static int help(CommandContext<class_2168> ctx) {
        boolean isOp = ((class_2168) ctx.getSource()).method_9259(2);
        ((class_2168) ctx.getSource()).method_9226(() -> {
            class_5250 t = class_2561.method_43470("=== ClaimBlocks Comandos ===\n").method_27695(new class_124[]{class_124.field_1054, class_124.field_1067})
                .method_10852(class_2561.method_43470("/claim menu  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- abre el menu de la zona donde estas\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim info  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- info de la zona donde estas\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim list  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- lista tus zonas\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim remove  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- borra tu zona actual\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim ban|unban <jugador>  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- gestiona baneados de tu zona\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim transfer <jugador>  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- transfiere tu zona\n").method_27692(class_124.field_1080))
                .method_10852(class_2561.method_43470("/claim removemember <jugador>  ").method_27692(class_124.field_1075))
                .method_10852(class_2561.method_43470("- quita un miembro\n").method_27692(class_124.field_1080));
            if (isOp) {
                t.method_10852(class_2561.method_43470("\n--- Solo Operadores ---\n").method_27692(class_124.field_1061))
                 .method_10852(class_2561.method_43470("/claim give <jugador> <tier>\n").method_27692(class_124.field_1054))
                 .method_10852(class_2561.method_43470("/claim clear <jugador>\n").method_27692(class_124.field_1054))
                 .method_10852(class_2561.method_43470("/claimadmin").method_27692(class_124.field_1054));
            }
            return t;
        }, false);
        return 1;
    }

    private static int give(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        Collection<class_3222> targets = class_2186.method_9312(ctx, (String)"jugador");
        String id = StringArgumentType.getString(ctx, (String)"id");
        ClaimTier tier = ClaimTier.byId(id);
        if (tier == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)("[x] ID no v\u00e1lido: " + id)).method_27692(class_124.field_1061));
            return 0;
        }
        class_2248 block = ClaimBlocks.blockForTier(tier);
        for (class_3222 p : targets) {
            class_1799 stack = ClaimBlocks.createTierItem(tier, 1);
            if (!p.method_31548().method_7394(stack)) {
                p.method_7328(stack, false);
            }
            p.method_7353((class_2561)class_2561.method_43470((String)"[+] ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)"Recibiste Piedra de Claim ").method_27692(class_124.field_1060)).method_10852((class_2561)class_2561.method_43470((String)tier.label()).method_27695(new class_124[]{class_124.field_1054, class_124.field_1067})), false);
            ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)"Le diste Piedra ").method_27692(class_124.field_1060)).method_10852((class_2561)class_2561.method_43470((String)tier.label()).method_27692(class_124.field_1054)).method_10852((class_2561)class_2561.method_43470((String)" a ").method_27692(class_124.field_1060)).method_10852((class_2561)class_2561.method_43470((String)p.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})), true);
        }
        return targets.size();
    }

    private static int clear(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 target = class_2186.method_9315(ctx, (String)"jugador");
        int n = ClaimManager.getInstance().clearClaimsOf(target.method_5667());
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)("Eliminadas " + n + " zona(s) de ")).method_27692(class_124.field_1060)).method_10852((class_2561)class_2561.method_43470((String)target.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})), true);
        return n;
    }

    private static int remove(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        Claim c = ClaimManager.getInstance().getClaimAt(p.method_37908(), p.method_24515());
        if (c == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No est\u00e1s en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657)p) && !p.method_5687(2)) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] Solo el due\u00f1o puede eliminar esta zona.").method_27692(class_124.field_1061));
            return 0;
        }
        class_2338 centre = c.getCenter();
        com.claimblocks.data.ClaimTier tier = c.getTier();
        if (tier != null && ClaimBlocks.isClaimConcreteForTier(p.method_37908().method_8320(centre).method_26204(), tier)) {
            p.method_37908().method_8651(centre, false, (class_1297)p);
        }
        ClaimManager.getInstance().removeClaim(p.method_37908(), centre);
        if (tier != null) {
            class_1799 stack = ClaimBlocks.createTierItem(tier, 1);
            if (!p.method_31548().method_7394(stack)) {
                p.method_7328(stack, false);
            }
        }
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 Zona eliminada. Piedra devuelta a tu inventario.").method_27692(class_124.field_1060), false);
        return 1;
    }

    private static int menu(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        Claim c = ClaimManager.getInstance().getClaimAt(p.method_37908(), p.method_24515());
        if (c == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No est\u00e1s en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657)p) && !p.method_5687(2)) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] Solo el due\u00f1o puede administrar esta zona.").method_27692(class_124.field_1061));
            return 0;
        }
        ClaimMenuHandler.open(p, c, 0);
        return 1;
    }

    private static int list(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        List<Claim> claims = ClaimManager.getInstance().getClaimsOf(p.method_5667());
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"[Claim] ").method_27692(class_124.field_1080).method_10852((class_2561)class_2561.method_43470((String)("Tus zonas (" + claims.size() + "):")).method_27692(class_124.field_1075)), false);
        for (Claim c : claims) {
            ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"  >> ").method_27692(class_124.field_1080).method_10852((class_2561)class_2561.method_43470((String)c.sizeLabel()).method_27692(class_124.field_1054)).method_10852((class_2561)class_2561.method_43470((String)(" en X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ())).method_27692(class_124.field_1068)).method_10852((class_2561)class_2561.method_43470((String)(" - " + c.getWorld())).method_27692(class_124.field_1063)), false);
        }
        if (claims.isEmpty()) {
            ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"  (no tienes ninguna)").method_27692(class_124.field_1063), false);
        }
        return claims.size();
    }

    private static int info(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 p = ((class_2168)ctx.getSource()).method_9207();
        Claim c = ClaimManager.getInstance().getClaimAt(p.method_37908(), p.method_24515());
        if (c == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No est\u00e1s en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"[Claim] ").method_27692(class_124.field_1080).method_10852((class_2561)class_2561.method_43470((String)"Informaci\u00f3n de la zona:").method_27695(new class_124[]{class_124.field_1075, class_124.field_1067})), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.labelLine("Due\u00f1o", c.getOwnerName(), class_124.field_1068), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.labelLine("Zona", c.sizeLabel() + " bloques | Altura: +/-" + c.getHeight(), class_124.field_1054), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.labelLine("Coords", "X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ() + " - " + c.getWorld(), class_124.field_1068), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.labelLine("Miembros", String.valueOf(c.getMembers().size()), class_124.field_1068), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.labelLine("Baneados", String.valueOf(c.getBannedPlayers().size()), class_124.field_1068), false);
        ClaimFlags f = c.getFlags();
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"  Flags activas:").method_27692(class_124.field_1080), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Construir", f.blockBuilding), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Romper", f.blockBreaking), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Explosiones", f.blockExplosions), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Fuego", f.blockFire), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Mobs hostiles", f.blockMobSpawn), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("PVP", f.blockPVP), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Da\u00f1o de mobs", f.blockMobDamage), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Alertas", f.trespasserAlerts), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Usar items", f.blockItemUse), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Entidades", f.blockEntityInteract), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Cultivos", f.blockTrampling), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Fluidos", f.blockFluids), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("PVP libre", f.pvpAll), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("\u00c1rboles", f.blockTreeChopping), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Modo visita", f.publicMode), false);
        ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Bienvenida", f.showWelcome), false);
        ClaimTier tier = c.getTier();
        if (tier != null && tier.isPaid()) {
            ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Regeneraci\u00f3n", f.effectRegeneration), false);
            ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Resistencia", f.effectResistance), false);
            ((class_2168)ctx.getSource()).method_9226(() -> ClaimCommands.formatFlag("Velocidad", f.effectSpeed), false);
        }
        return 1;
    }

    private static class_2561 labelLine(String key, String value, class_124 valueColor) {
        return class_2561.method_43470((String)("  " + key + ": ")).method_27692(class_124.field_1080).method_10852((class_2561)class_2561.method_43470((String)value).method_27692(valueColor));
    }

    private static class_2561 formatFlag(String name, boolean on) {
        return class_2561.method_43470((String)("    " + name + ": ")).method_27692(class_124.field_1080).method_10852((class_2561)class_2561.method_43470((String)(on ? "[ON]" : "[OFF]")).method_27695(new class_124[]{on ? class_124.field_1060 : class_124.field_1061, class_124.field_1067}));
    }

    private static int ban(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 exec = ((class_2168)ctx.getSource()).method_9207();
        class_3222 target = class_2186.method_9315(ctx, (String)"jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.method_37908(), exec.method_24515());
        if (c == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No est\u00e1s en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657)exec) && !exec.method_5687(2)) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] Solo el due\u00f1o puede banear de esta zona.").method_27692(class_124.field_1061));
            return 0;
        }
        // v6.0.1: un jugador normal no puede actuar contra un OP.
        if (target.method_5687(2) && !exec.method_5687(2)) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No puedes banear a un operador.").method_27692(class_124.field_1061));
            return 0;
        }
        c.banPlayer(target.method_5667());
        ClaimManager.getInstance().save();
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)target.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)" baneado.").method_27692(class_124.field_1060)), true);
        return 1;
    }

    private static int unban(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 exec = ((class_2168)ctx.getSource()).method_9207();
        class_3222 target = class_2186.method_9315(ctx, (String)"jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.method_37908(), exec.method_24515());
        if (c == null) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] No est\u00e1s en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657)exec) && !exec.method_5687(2)) {
            ((class_2168)ctx.getSource()).method_9213((class_2561)class_2561.method_43470((String)"[x] Solo el due\u00f1o puede desbanear de esta zona.").method_27692(class_124.field_1061));
            return 0;
        }
        c.unbanPlayer(target.method_5667());
        ClaimManager.getInstance().save();
        ((class_2168)ctx.getSource()).method_9226(() -> class_2561.method_43470((String)"\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067}).method_10852((class_2561)class_2561.method_43470((String)target.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})).method_10852((class_2561)class_2561.method_43470((String)" desbaneado.").method_27692(class_124.field_1060)), true);
        return 1;
    }
    // ---------- v6 NUEVOS COMANDOS ----------

    private static int transfer(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 exec = ((class_2168) ctx.getSource()).method_9207();
        class_3222 target = class_2186.method_9315(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.method_37908(), exec.method_24515());
        if (c == null) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] No estás en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657) exec) && !exec.method_5687(2)) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] Solo el dueño puede transferir esta zona.").method_27692(class_124.field_1061));
            return 0;
        }
        // v6.0.1: un jugador normal no puede transferir a un OP.
        if (target.method_5687(2) && !exec.method_5687(2)) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] No puedes transferir tu zona a un operador.").method_27692(class_124.field_1061));
            return 0;
        }
        if (c.isOwner(target.method_5667())) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] Ya es el dueño actual.").method_27692(class_124.field_1061));
            return 0;
        }
        ClaimManager.getInstance().transferOwnership(c, target.method_5667(), target.method_5477().getString());
        ((class_2168) ctx.getSource()).method_9226(() -> class_2561.method_43470("\u2714 Zona transferida a ").method_27692(class_124.field_1060)
                .method_10852(class_2561.method_43470(target.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067})), true);
        target.method_7353(class_2561.method_43470("[Claim] ").method_27692(class_124.field_1080)
                .method_10852(class_2561.method_43470("Has recibido la propiedad de una zona en X=" + c.getX() + " Z=" + c.getZ()).method_27692(class_124.field_1060)), false);
        return 1;
    }

    private static int removeMember(CommandContext<class_2168> ctx) throws CommandSyntaxException {
        class_3222 exec = ((class_2168) ctx.getSource()).method_9207();
        class_3222 target = class_2186.method_9315(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.method_37908(), exec.method_24515());
        if (c == null) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] No estás en ninguna zona protegida.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isOwner((class_1657) exec) && !exec.method_5687(2)) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] Solo el dueño puede gestionar miembros.").method_27692(class_124.field_1061));
            return 0;
        }
        // v6.0.1: un jugador normal no puede actuar contra un OP.
        if (target.method_5687(2) && !exec.method_5687(2)) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] No puedes gestionar a un operador.").method_27692(class_124.field_1061));
            return 0;
        }
        if (!c.isMember(target.method_5667())) {
            ((class_2168) ctx.getSource()).method_9213(class_2561.method_43470("[x] " + target.method_5477().getString() + " no es miembro.").method_27692(class_124.field_1061));
            return 0;
        }
        c.removeMember(target.method_5667());
        ClaimManager.getInstance().save();
        ((class_2168) ctx.getSource()).method_9226(() -> class_2561.method_43470("\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067})
                .method_10852(class_2561.method_43470(target.method_5477().getString()).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}))
                .method_10852(class_2561.method_43470(" eliminado de la zona.").method_27692(class_124.field_1060)), true);
        return 1;
    }
}
