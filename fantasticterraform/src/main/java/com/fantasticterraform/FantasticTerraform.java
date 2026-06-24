package com.fantasticterraform;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Fantastic Terraform: editor de terrenos completo para Forge 1.20.1, estrictamente
 * OP-only y controlado por HUD. Punto de entrada del mod.
 */
@Mod(FantasticTerraform.MOD_ID)
public final class FantasticTerraform {

    public static final String MOD_ID = "fantasticterraform";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasticTerraform() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TerraformConfig.SPEC, "fantasticterraform/config.toml");

        LOGGER.info("[FantasticTerraform] Editor de terrenos cargado (Forge 1.20.1, OP-only, control por HUD).");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
