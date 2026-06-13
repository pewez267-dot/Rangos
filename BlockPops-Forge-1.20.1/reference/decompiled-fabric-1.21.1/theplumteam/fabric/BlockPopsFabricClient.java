package com.theplumteam.fabric;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.client.config.ClientServerConfig;
import com.theplumteam.client.renderer.BoxBlockItemRenderer;
import com.theplumteam.client.renderer.BoxBlockRenderer;
import com.theplumteam.client.renderer.ClawMachineBlockItemRenderer;
import com.theplumteam.client.renderer.ClawMachineBlockRenderer;
import com.theplumteam.client.renderer.FigureBlockItemRenderer;
import com.theplumteam.client.renderer.FigureBlockRenderer;
import com.theplumteam.client.renderer.FigureWidgetRenderer;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.network.ModNetworking;
import com.theplumteam.registry.ModBlockEntities;
import com.theplumteam.registry.ModItems;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Map.Entry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import net.minecraft.class_2591;
import net.minecraft.class_2960;
import net.minecraft.class_3264;
import net.minecraft.class_3300;

public class BlockPopsFabricClient implements ClientModInitializer {
   public void onInitializeClient() {
      BlockPopsMod.logDebug("BlockPops client initialization on Fabric");
      ModNetworking.initClient();
      ClientServerConfig.loadLocalHiddenCollections();
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> FigureWidgetRenderer.clearCache());
      BlockEntityRendererRegistry.register((class_2591)ModBlockEntities.BOX_BLOCK.get(), context -> new BoxBlockRenderer());
      BlockEntityRendererRegistry.register((class_2591)ModBlockEntities.CLAW_MACHINE_BLOCK.get(), context -> new ClawMachineBlockRenderer());
      BlockEntityRendererRegistry.register((class_2591)ModBlockEntities.FIGURE_BLOCK.get(), context -> new FigureBlockRenderer());
      this.registerItemRenderers();
      ResourceManagerHelper.get(class_3264.field_14188).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public class_2960 getFabricId() {
            return class_2960.method_60655("blockpops", "collection_loader");
         }

         public void method_14491(class_3300 resourceManager) {
            CollectionRegistry.loadCollections(resourceManager);
         }
      });
      BlockPopsMod.logDebug("BlockPops Fabric client initialization complete");
   }

   private void registerItemRenderers() {
      BoxBlockItemRenderer boxRenderer = null;
      FigureBlockItemRenderer figureRenderer = null;
      ClawMachineBlockItemRenderer clawRenderer = null;

      for (PopBlockColor color : PopBlockColor.values()) {
         RegistrySupplier<class_1792> itemSupplier = ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(color);
         if (itemSupplier != null) {
            BoxBlockItemRenderer finalBoxRenderer = boxRenderer != null ? boxRenderer : (boxRenderer = new BoxBlockItemRenderer());
            BuiltinItemRendererRegistry.INSTANCE.register((class_1935)itemSupplier.get(), finalBoxRenderer::method_3166);
         }
      }

      for (Entry<String, RegistrySupplier<class_1792>> entry : ModItems.BOX_BLOCK_ITEMS.entrySet()) {
         RegistrySupplier<class_1792> itemSupplier = entry.getValue();
         if (itemSupplier != null) {
            BoxBlockItemRenderer finalBoxRenderer = boxRenderer != null ? boxRenderer : (boxRenderer = new BoxBlockItemRenderer());
            BuiltinItemRendererRegistry.INSTANCE.register((class_1935)itemSupplier.get(), finalBoxRenderer::method_3166);
         }
      }

      if (ModItems.FIGURE_BLOCK_ITEM != null) {
         figureRenderer = new FigureBlockItemRenderer();
         BuiltinItemRendererRegistry.INSTANCE.register((class_1935)ModItems.FIGURE_BLOCK_ITEM.get(), figureRenderer::method_3166);
      }

      if (ModItems.CLAW_MACHINE_BLOCK_ITEM != null) {
         clawRenderer = new ClawMachineBlockItemRenderer();
         BuiltinItemRendererRegistry.INSTANCE.register((class_1935)ModItems.CLAW_MACHINE_BLOCK_ITEM.get(), clawRenderer::method_3166);
      }

      BlockPopsMod.logDebug("Registered item renderers for GeckoLib blocks");
   }
}
