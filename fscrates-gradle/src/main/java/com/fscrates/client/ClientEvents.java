// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client;

import com.fscrates.client.render.CrateRenderer;
import com.fscrates.client.render.CrateBakedModels;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.function.Supplier;
import com.fscrates.client.render.CrateModel;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fscrates", value = { Dist.CLIENT }, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents
{
    private ClientEvents() {
    }
    
    @SubscribeEvent
    public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CrateModel.LAYER, (Supplier)CrateModel::createLayer);
    }
    
    @SubscribeEvent
    public static void registerAdditionalModels(final ModelEvent.RegisterAdditional event) {
        CrateBakedModels.registerAll(event);
    }
    
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)ModRegistry.CRATE_BE.get(), CrateRenderer::new);
    }
}
