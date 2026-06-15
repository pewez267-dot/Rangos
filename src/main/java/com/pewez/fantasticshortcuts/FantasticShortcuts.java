package com.pewez.fantasticshortcuts;

import com.pewez.fantasticshortcuts.client.ClientSetup;
import com.pewez.fantasticshortcuts.config.FSConfig;
import com.pewez.fantasticshortcuts.network.FSNetwork;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Clase principal de Fantastic Shortcuts.
 *
 * <p>Sistema avanzado de atajos GLOBALES de comandos para Forge 1.20.1: traduce alias cortos a
 * comandos reales respetando al 100% el sistema de permisos (vanilla, mods y LuckPerms). NUNCA
 * otorga permisos ni ejecuta como consola.
 */
@Mod(FantasticShortcuts.MOD_ID)
public class FantasticShortcuts {

    public static final String MOD_ID = "fantasticshortcuts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasticShortcuts() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Configuración en config/fantasticshortcuts/config.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FSConfig.SPEC, MOD_ID + "/config.toml");

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        LOGGER.info("[F-Shortcuts] Inicializando Fantastic Shortcuts");
    }

    /** Directorio base de datos del mod: {@code config/fantasticshortcuts}. */
    public static Path baseDir() {
        return FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FSNetwork.register();
            ShortcutManager.get().init(baseDir());
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }
}
