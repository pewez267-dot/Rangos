package com.fantastickits;

import com.fantastickits.commands.FKitsCommand;
import com.fantastickits.data.AuditLog;
import com.fantastickits.data.ConfigHandler;
import com.fantastickits.data.GroupCommandData;
import com.fantastickits.data.KitData;
import com.fantastickits.data.PlayerData;
import com.fantastickits.gui.KitMenuRegistry;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.security.CommandRestrictionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FantasticKits.MOD_ID)
public class FantasticKits {

    public static final String MOD_ID = "fantastickits";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static FantasticKits instance;

    private KitData kitData;
    private PlayerData playerData;
    private GroupCommandData groupCommandData;
    private AuditLog auditLog;
    private LuckPermsIntegration luckPermsIntegration;
    private CommandRestrictionHandler commandRestrictionHandler;

    public FantasticKits() {
        instance = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Register menu types
        KitMenuRegistry.MENUS.register(modEventBus);

        // Register Forge config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.SPEC, "fantastickits/config.toml");

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("FantasticKits common setup initialized.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("FantasticKits: Server starting, loading data...");

        // Initialize data managers
        this.auditLog = new AuditLog();
        this.kitData = new KitData();
        this.playerData = new PlayerData();
        this.groupCommandData = new GroupCommandData();

        // Load all persistent data
        this.kitData.load();
        this.playerData.load();
        this.groupCommandData.load();

        // Initialize LuckPerms integration
        this.luckPermsIntegration = new LuckPermsIntegration();

        // Initialize command restriction handler and register it
        this.commandRestrictionHandler = new CommandRestrictionHandler(groupCommandData, luckPermsIntegration, auditLog);
        MinecraftForge.EVENT_BUS.register(this.commandRestrictionHandler);

        LOGGER.info("FantasticKits: Data loaded successfully.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("FantasticKits: Server stopping, saving data...");
        if (kitData != null) kitData.save();
        if (playerData != null) playerData.save();
        if (groupCommandData != null) groupCommandData.save();
        if (commandRestrictionHandler != null) {
            MinecraftForge.EVENT_BUS.unregister(this.commandRestrictionHandler);
        }
        LOGGER.info("FantasticKits: Data saved successfully.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FKitsCommand.register(event.getDispatcher());
        LOGGER.info("FantasticKits: Commands registered.");
    }

    public static FantasticKits getInstance() {
        return instance;
    }

    public KitData getKitData() {
        return kitData;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public GroupCommandData getGroupCommandData() {
        return groupCommandData;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

    public CommandRestrictionHandler getCommandRestrictionHandler() {
        return commandRestrictionHandler;
    }
}
