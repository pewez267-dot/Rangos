package com.fscrates.client.render;

import com.fscrates.config.Rarity;
import com.fscrates.item.CrateItems;
import com.fscrates.registry.ModRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderiza el ITEM de crate (en mano, inventario, GUI, suelo, marco) usando el
 * modelo 3D real del cofre por rareza (base + tapa cerrada) en vez de la textura
 * de "barril" plana que se veia antes.
 *
 * Se activa porque el modelo del item (models/item/crate.json) usa
 * "builtin/entity", lo que hace que Forge delegue el render aqui via
 * IClientItemExtensions#getCustomRenderer (ver CrateBlockItem).
 */
public class CrateItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CrateItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(final ItemStack stack, final ItemDisplayContext ctx, final PoseStack pose,
                             final MultiBufferSource buffers, final int light, final int overlay) {
        final Rarity rarity = CrateItems.rarity(stack);
        final BakedModel base = CrateBakedModels.get(rarity);
        final BakedModel lid = CrateBakedModels.getLid(rarity);
        final BlockState state = ((Block) ModRegistry.CRATE_BLOCK.get()).defaultBlockState();
        final VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        final ModelBlockRenderer mr = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        pose.pushPose();
        // El modelo ocupa el cubo 0..1; lo centramos igual que un item de bloque
        // vanilla (las transformaciones display del JSON estan calibradas para esto).
        pose.translate(-0.5, -0.5, -0.5);
        // No se aplica renderScale por rareza aqui: el item se ve a tamano normal.
        // La tapa se dibuja en su posicion cerrada (sin animacion).
        mr.renderModel(pose.last(), vc, state, base, 1.0f, 1.0f, 1.0f, light, overlay);
        mr.renderModel(pose.last(), vc, state, lid, 1.0f, 1.0f, 1.0f, light, overlay);
        pose.popPose();
    }
}
