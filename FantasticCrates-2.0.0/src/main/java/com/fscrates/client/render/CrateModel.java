// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.render;

import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class CrateModel
{
    public static final ModelLayerLocation LAYER;
    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    
    public CrateModel(final ModelPart root) {
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
    }
    
    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0f, 0.0f, 1.0f, 14.0f, 10.0f, 14.0f), PartPose.ZERO);
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0f, 0.0f, 1.0f, 14.0f, 5.0f, 14.0f), PartPose.offset(0.0f, 9.0f, 1.0f));
        root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0f, -1.0f, 15.0f, 2.0f, 4.0f, 1.0f), PartPose.offset(0.0f, 8.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
    
    public void render(final PoseStack pose, final VertexConsumer vc, final int light, final int overlay, final float lidAngleRad, final float r, final float g, final float b, final float a) {
        this.lid.xRot = -lidAngleRad;
        this.lock.xRot = this.lid.xRot;
        this.lid.render(pose, vc, light, overlay, r, g, b, a);
        this.lock.render(pose, vc, light, overlay, r, g, b, a);
        this.bottom.render(pose, vc, light, overlay, r, g, b, a);
    }
    
    static {
        LAYER = new ModelLayerLocation(new ResourceLocation("fscrates", "crate"), "main");
    }
}
