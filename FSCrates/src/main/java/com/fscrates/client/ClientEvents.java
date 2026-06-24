package com.fscrates.client;

import com.fscrates.FSCrates;
import com.fscrates.client.render.CrateModel;
import com.fscrates.client.render.CrateRenderer;
import com.fscrates.registry.ModRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only mod-bus event subscriber: registers the crate model layer and the
 * BlockEntityRenderer that draws and animates the chest in the world.
 */
@Mod.EventBusSubscriber(modid = FSCrates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CrateModel.LAYER, CrateModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.CRATE_BE.get(), CrateRenderer::new);
    }
}
