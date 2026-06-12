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
        this.bottom = root.m_171324_("bottom");
        this.lid = root.m_171324_("lid");
        this.lock = root.m_171324_("lock");
    }
    
    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.m_171576_();
        root.m_171599_("bottom", CubeListBuilder.m_171558_().m_171514_(0, 19).m_171481_(1.0f, 0.0f, 1.0f, 14.0f, 10.0f, 14.0f), PartPose.f_171404_);
        root.m_171599_("lid", CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(1.0f, 0.0f, 1.0f, 14.0f, 5.0f, 14.0f), PartPose.m_171419_(0.0f, 9.0f, 1.0f));
        root.m_171599_("lock", CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(7.0f, -1.0f, 15.0f, 2.0f, 4.0f, 1.0f), PartPose.m_171419_(0.0f, 8.0f, 0.0f));
        return LayerDefinition.m_171565_(mesh, 64, 64);
    }
    
    public void render(final PoseStack pose, final VertexConsumer vc, final int light, final int overlay, final float lidAngleRad, final float r, final float g, final float b, final float a) {
        this.lid.f_104203_ = -lidAngleRad;
        this.lock.f_104203_ = this.lid.f_104203_;
        this.lid.m_104306_(pose, vc, light, overlay, r, g, b, a);
        this.lock.m_104306_(pose, vc, light, overlay, r, g, b, a);
        this.bottom.m_104306_(pose, vc, light, overlay, r, g, b, a);
    }
    
    static {
        LAYER = new ModelLayerLocation(new ResourceLocation("fscrates", "crate"), "main");
    }
}
