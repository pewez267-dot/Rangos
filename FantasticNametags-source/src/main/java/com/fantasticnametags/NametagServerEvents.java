package com.fantasticnametags;

import com.fantasticnametags.net.NametagNetwork;
import com.fantasticnametags.net.SyncNametagPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Comandos en tiempo real (op) + envio de valores al conectarse. Lado servidor. */
@Mod.EventBusSubscriber(modid = FantasticNametags.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NametagServerEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NametagNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncNametagPacket(NametagConfig.HEIGHT_OFFSET.get(), NametagConfig.PLAYERS_ONLY.get()));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fsnametag").requires(s -> s.hasPermission(2))
            .then(Commands.literal("altura")
                .then(Commands.argument("valor", DoubleArgumentType.doubleArg(-2.0, 4.0))
                    .executes(ctx -> setHeight(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "valor")))))
            .then(Commands.literal("jugadores")
                .then(Commands.argument("valor", BoolArgumentType.bool())
                    .executes(ctx -> setPlayersOnly(ctx.getSource(), BoolArgumentType.getBool(ctx, "valor")))))
            .then(Commands.literal("ver")
                .executes(ctx -> show(ctx.getSource()))));
    }

    private static int setHeight(CommandSourceStack src, double value) {
        NametagConfig.HEIGHT_OFFSET.set(value);
        NametagNetwork.syncToAll(value, NametagConfig.PLAYERS_ONLY.get());
        src.sendSuccess(() -> Component.literal("\u00a7a[Nametags] Altura fijada a \u00a7e" + value + "\u00a7a y aplicada a todos en vivo."), true);
        return 1;
    }

    private static int setPlayersOnly(CommandSourceStack src, boolean value) {
        NametagConfig.PLAYERS_ONLY.set(value);
        NametagNetwork.syncToAll(NametagConfig.HEIGHT_OFFSET.get(), value);
        src.sendSuccess(() -> Component.literal("\u00a7a[Nametags] Solo jugadores: \u00a7e" + value + "\u00a7a (aplicado en vivo)."), true);
        return 1;
    }

    private static int show(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("\u00a7b[Nametags] altura=\u00a7e" + NametagConfig.HEIGHT_OFFSET.get()
            + "\u00a7b, soloJugadores=\u00a7e" + NametagConfig.PLAYERS_ONLY.get()), false);
        return 1;
    }

    private NametagServerEvents() {
    }
}
