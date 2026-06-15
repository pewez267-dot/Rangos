package com.fantastic.kits;

import com.fantastic.kits.audit.AuditLogger;
import com.fantastic.kits.audit.SecurityEventLogger;
import com.fantastic.kits.commands.FKitsCommand;
import com.fantastic.kits.commandsystem.CommandDiscoveryService;
import com.fantastic.kits.config.FKConfig;
import com.fantastic.kits.kits.KitManager;
import com.fantastic.kits.luckperms.LuckPermsHook;
import com.fantastic.kits.network.FKNetwork;
import com.fantastic.kits.security.AntiExploitGuard;
import com.fantastic.kits.storage.PlayerDataManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Mod entry point. Wires together services in a strict, dependency-aware order:
 * <ol>
 *     <li>Configuration is loaded from disk.</li>
 *     <li>Storage directories are created.</li>
 *     <li>Audit + security loggers are initialised.</li>
 *     <li>LuckPerms is hooked (best-effort; never crashes).</li>
 *     <li>Kits are loaded into memory.</li>
 *     <li>Commands and network are registered.</li>
 * </ol>
 * <p>
 * The class deliberately exposes static accessors for the singletons so that
 * the rest of the codebase can rely on them through a single boot sequence,
 * mirroring the pattern used in FantasticSpawners and FantasticCrates.
 */
@Mod(Reference.MOD_ID)
public final class FantasticKits {

    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_NAME);

    static {
        // Visible boot banner so the copyright stays in every server log.
        LOGGER.info("================================================================");
        LOGGER.info(" {} - {}", Reference.MOD_NAME, Reference.COPYRIGHT);
        LOGGER.info(" Author: {} - Unauthorised redistribution is prohibited.", Reference.AUTHOR);
        LOGGER.info("================================================================");
    }

    private static FantasticKits instance;

    private FKConfig config;
    private AuditLogger auditLogger;
    private SecurityEventLogger securityLogger;
    private LuckPermsHook luckPerms;
    private KitManager kitManager;
    private PlayerDataManager playerDataManager;
    private CommandDiscoveryService commandDiscovery;
    private AntiExploitGuard antiExploit;

    private Path configRoot;

    public FantasticKits() {
        instance = this;
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FKNetwork::register);
        LOGGER.info("[{}] Common setup complete.", Reference.MOD_NAME);
    }

    private void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // Hook for client-only init (no body needed - the event bus subscriber
        // in com.fantastic.kits.client.ClientSetup handles registrations).
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        configRoot = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(Reference.MOD_ID);

        // 1. Configuration
        this.config = FKConfig.loadOrCreate(configRoot);
        LOGGER.info("[{}] Configuration loaded.", Reference.MOD_NAME);

        // 2. Audit + security loggers
        this.auditLogger = new AuditLogger(configRoot.resolve(Reference.DIR_AUDIT), config);
        this.securityLogger = new SecurityEventLogger(configRoot.resolve(Reference.DIR_SECURITY), config);
        LOGGER.info("[{}] Audit + security loggers initialised.", Reference.MOD_NAME);

        // 3. LuckPerms (best effort)
        this.luckPerms = new LuckPermsHook();
        this.luckPerms.tryAttach();

        // 4. Kits + player data
        this.kitManager = new KitManager(configRoot.resolve(Reference.DIR_KITS));
        this.kitManager.loadAll();
        this.playerDataManager = new PlayerDataManager(configRoot.resolve(Reference.DIR_PLAYERS));

        // 5. Command discovery + anti-exploit
        this.commandDiscovery = new CommandDiscoveryService(event.getServer());
        this.antiExploit = new AntiExploitGuard();

        LOGGER.info("[{}] Server bootstrap finished. Kits loaded: {}", Reference.MOD_NAME, kitManager.size());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FKitsCommand.register(event.getDispatcher());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (kitManager != null) kitManager.flush();
        if (playerDataManager != null) playerDataManager.flushAll();
        if (auditLogger != null) auditLogger.close();
        if (securityLogger != null) securityLogger.close();
        LOGGER.info("[{}] Server shutdown clean.", Reference.MOD_NAME);
    }

    // ------------------------------------------------------------------
    // Static accessors
    // ------------------------------------------------------------------

    public static FantasticKits get() { return instance; }
    public static FKConfig config() { return instance.config; }
    public static AuditLogger audit() { return instance.auditLogger; }
    public static SecurityEventLogger security() { return instance.securityLogger; }
    public static LuckPermsHook luckPerms() { return instance.luckPerms; }
    public static KitManager kits() { return instance.kitManager; }
    public static PlayerDataManager players() { return instance.playerDataManager; }
    public static CommandDiscoveryService commands() { return instance.commandDiscovery; }
    public static AntiExploitGuard antiExploit() { return instance.antiExploit; }
    public static Path configRoot() { return instance.configRoot; }
}
