package com.fsmobs.command;

import com.fsmobs.MobControl;
import com.fsmobs.network.Net;
import com.fsmobs.network.OpenConfigPacket;
import com.fsmobs.network.SetOverlayPacket;
import com.fsmobs.network.SyncConfigPacket;
import com.fsmobs.stats.StatsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class FSMobsCommand {

    private FSMobsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fsmobs")
                .requires(src -> src.hasPermission(2))
                // /fsmobs -> abre la GUI de control
                .executes(ctx -> openGui(ctx.getSource()))
                .then(Commands.literal("gui").executes(ctx -> openGui(ctx.getSource())))
                // /fsmobs stats -> muestra/oculta el panel de estadisticas en pantalla
                .then(Commands.literal("stats").executes(ctx -> toggleStats(ctx.getSource())))
                // /fsmobs reset -> restaura la configuracion por defecto
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource()))));
    }

    private static int openGui(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("\u00a7cSolo un jugador puede abrir la GUI."));
            return 0;
        }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenConfigPacket());
        return 1;
    }

    private static int toggleStats(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("\u00a7cSolo un jugador puede usar el panel."));
            return 0;
        }
        boolean on = StatsManager.toggleHud(player.getUUID());
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SetOverlayPacket(on));
        src.sendSuccess(() -> Component.literal(on
                ? "\u00a76[Mobs] \u00a7aPanel de estadisticas ACTIVADO \u00a77(vuelve a escribir el comando para ocultarlo)."
                : "\u00a76[Mobs] \u00a77Panel de estadisticas oculto."), false);
        return 1;
    }

    private static int reset(CommandSourceStack src) {
        MobControl.reset();
        MobControl.save();
        ServerPlayer player = src.getPlayer();
        if (player != null) {
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigPacket());
        }
        src.sendSuccess(() -> Component.literal("\u00a76[Mobs] \u00a7aConfiguracion restaurada por defecto (sin limites)."), true);
        return 1;
    }
}
