package com.fscrates.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class CrateModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("fscrates", "crate"), "main");
    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;

    public CrateModel(ModelPart root) {
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0f, 0.0f, 1.0f, 14.0f, 10.0f, 14.0f), PartPose.ZERO);
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0f, 0.0f, 1.0f, 14.0f, 5.0f, 14.0f), PartPose.offset((float)0.0f, (float)9.0f, (float)1.0f));
        root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0f, -1.0f, 15.0f, 2.0f, 4.0f, 1.0f), PartPose.offset((float)0.0f, (float)8.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)mesh, (int)64, (int)64);
    }

    public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, float lidAngleRad, float r, float g, float b, float a) {
        this.lock.xRot = this.lid.xRot = -lidAngleRad;
        this.lid.render(pose, vc, light, overlay, r, g, b, a);
        this.lock.render(pose, vc, light, overlay, r, g, b, a);
        this.bottom.render(pose, vc, light, overlay, r, g, b, a);
    }
}

