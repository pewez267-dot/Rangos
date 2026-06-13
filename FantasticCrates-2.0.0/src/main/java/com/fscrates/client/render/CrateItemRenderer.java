// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.render;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import com.fscrates.config.Rarity;
import net.minecraft.client.renderer.RenderType;
import com.fscrates.registry.ModRegistry;
import net.minecraft.world.level.block.Block;
import com.fscrates.item.CrateItems;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

public class CrateItemRenderer extends BlockEntityWithoutLevelRenderer
{
    public CrateItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
    
    public void renderByItem(final ItemStack stack, final ItemDisplayContext ctx, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay) {
        final Rarity rarity = CrateItems.rarity(stack);
        final BakedModel base = CrateBakedModels.get(rarity);
        final BakedModel lid = CrateBakedModels.getLid(rarity);
        final BlockState state = ((Block)ModRegistry.CRATE_BLOCK.get()).defaultBlockState();
        final VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        final ModelBlockRenderer mr = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        pose.pushPose();
        mr.renderModel(pose.last(), vc, state, base, 1.0f, 1.0f, 1.0f, light, overlay);
        mr.renderModel(pose.last(), vc, state, lid, 1.0f, 1.0f, 1.0f, light, overlay);
        pose.popPose();
    }
}
