package com.fsrecipes;

import com.fsrecipes.command.FSRecipesCommand;
import com.fsrecipes.network.Net;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FSRecipes.MODID)
public final class FSRecipes {

    public static final String MODID = "fsrecipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FSRecipes() {
        Net.register();
    }

    /** Handlers del bus de FORGE (servidor). */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {

        private ForgeEvents() {}

        @SubscribeEvent
        public static void onAddReloadListener(AddReloadListenerEvent event) {
            // Se anade DESPUES del RecipeManager: reaplica los baneos tras cada carga de datapacks.
            event.addListener(new BanReloadListener(
                    event.getServerResources().getRecipeManager(),
                    event.getRegistryAccess()));
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            FSRecipesCommand.register(event.getDispatcher(), event.getBuildContext());
        }
    }
}
