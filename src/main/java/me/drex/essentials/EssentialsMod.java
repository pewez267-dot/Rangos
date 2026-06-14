package me.drex.essentials;

import me.drex.essentials.command.ModCommands;
import me.drex.essentials.config.Config;
import me.drex.essentials.storage.DataStorage;
import me.drex.essentials.storage.Location;
import me.drex.essentials.storage.PlayerData;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.CommandSpy;
import me.drex.essentials.util.MessageState;
import me.drex.essentials.util.TeleportManager;
import me.drex.essentials.util.TpaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(EssentialsMod.MOD_ID)
@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EssentialsMod {

    public static final String MOD_ID = "essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger("Essentials");

    public EssentialsMod() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("essentials");
        Config.load(configDir);
        Messages.load(configDir);
        Messages.save();
        LOGGER.info("Essentials (Forge port of Fabric Essentials by Drex, MIT) initialised");
    }

    public static void reload() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("essentials");
        Config.load(configDir);
        Messages.load(configDir);
        Messages.save();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        DataStorage.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DataStorage.saveAll();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TeleportManager.tick();
            TpaManager.tickExpire();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DataStorage.unload(player.getUUID());
            TeleportManager.clear(player.getUUID());
            TpaManager.clear(player.getUUID());
            CommandSpy.clear(player.getUUID());
            MessageState.clear(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = DataStorage.playerData(player);
            data.lastLocation = Location.of(player);
            DataStorage.savePlayerData(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportManager.onDamage(player);
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        try {
            ServerPlayer source = event.getParseResults().getContext().getSource().getPlayerOrException();
            String input = event.getParseResults().getReader().getString();
            if (!input.startsWith("/")) {
                input = "/" + input;
            }
            for (java.util.UUID uuid : CommandSpy.enabled()) {
                if (uuid.equals(source.getUUID())) {
                    continue;
                }
                ServerPlayer spy = source.server.getPlayerList().getPlayer(uuid);
                if (spy != null) {
                    spy.sendSystemMessage(Messages.get("commandspy.format",
                            "&7[CommandSpy] &e{player}&7: &f{command}",
                            Messages.of("player", source.getGameProfile().getName(), "command", input)));
                }
            }
        } catch (Exception ignored) {
            // Not a player or no command spies online
        }
    }

    public static Component prefix() {
        return Messages.prefix();
    }
}
