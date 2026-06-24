// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.event.ModelEvent;
import com.fscrates.config.Rarity;
import net.minecraft.resources.ResourceLocation;

public final class CrateBakedModels
{
    public static final ResourceLocation COMMON;
    public static final ResourceLocation RARE;
    public static final ResourceLocation EPIC;
    public static final ResourceLocation LEGENDARY;
    public static final ResourceLocation MYTHIC;
    public static final ResourceLocation COMMON_LID;
    public static final ResourceLocation RARE_LID;
    public static final ResourceLocation EPIC_LID;
    public static final ResourceLocation LEGENDARY_LID;
    public static final ResourceLocation MYTHIC_LID;
    public static final float OPEN_ANGLE_DEG = 100.0f;
    
    private CrateBakedModels() {
    }
    
    public static ResourceLocation locationFor(final Rarity rarity) {
        if (rarity == null) {
            return CrateBakedModels.COMMON;
        }
        switch (rarity) {
            case RARE: {
                return CrateBakedModels.RARE;
            }
            case EPIC: {
                return CrateBakedModels.EPIC;
            }
            case LEGENDARY: {
                return CrateBakedModels.LEGENDARY;
            }
            case MYTHIC: {
                return CrateBakedModels.MYTHIC;
            }
            default: {
                return CrateBakedModels.COMMON;
            }
        }
    }
    
    public static ResourceLocation lidLocationFor(final Rarity rarity) {
        if (rarity == null) {
            return CrateBakedModels.COMMON_LID;
        }
        switch (rarity) {
            case RARE: {
                return CrateBakedModels.RARE_LID;
            }
            case EPIC: {
                return CrateBakedModels.EPIC_LID;
            }
            case LEGENDARY: {
                return CrateBakedModels.LEGENDARY_LID;
            }
            case MYTHIC: {
                return CrateBakedModels.MYTHIC_LID;
            }
            default: {
                return CrateBakedModels.COMMON_LID;
            }
        }
    }
    
    public static float[] hinge(final Rarity rarity) {
        if (rarity == null) {
            return new float[] { 0.5f, 0.19971f, 0.79445f };
        }
        switch (rarity) {
            case RARE: {
                return new float[] { 0.5f, 0.57405f, 0.81105f };
            }
            case EPIC: {
                return new float[] { 0.5f, 0.52543f, 0.91443f };
            }
            case LEGENDARY: {
                return new float[] { 0.47306f, 0.3235f, 0.71127f };
            }
            case MYTHIC: {
                return new float[] { 0.5f, 0.45352f, 0.84924f };
            }
            default: {
                return new float[] { 0.5f, 0.55052f, 0.79445f };
            }
        }
    }
    
    public static void registerAll(final ModelEvent.RegisterAdditional event) {
        event.register(CrateBakedModels.COMMON);
        event.register(CrateBakedModels.RARE);
        event.register(CrateBakedModels.EPIC);
        event.register(CrateBakedModels.LEGENDARY);
        event.register(CrateBakedModels.MYTHIC);
        event.register(CrateBakedModels.COMMON_LID);
        event.register(CrateBakedModels.RARE_LID);
        event.register(CrateBakedModels.EPIC_LID);
        event.register(CrateBakedModels.LEGENDARY_LID);
        event.register(CrateBakedModels.MYTHIC_LID);
    }
    
    public static BakedModel get(final Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(locationFor(rarity));
    }
    
    public static BakedModel getLid(final Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(lidLocationFor(rarity));
    }
    
    public static float renderScale(final Rarity rarity) {
        if (rarity == null) {
            return 1.0f;
        }
        return rarity.sizeScale();
    }
    
    static {
        COMMON = new ResourceLocation("fscrates", "block/crate_common");
        RARE = new ResourceLocation("fscrates", "block/crate_rare");
        EPIC = new ResourceLocation("fscrates", "block/crate_epic");
        LEGENDARY = new ResourceLocation("fscrates", "block/crate_legendary");
        MYTHIC = new ResourceLocation("fscrates", "block/crate_mythic");
        COMMON_LID = new ResourceLocation("fscrates", "block/crate_common_lid");
        RARE_LID = new ResourceLocation("fscrates", "block/crate_rare_lid");
        EPIC_LID = new ResourceLocation("fscrates", "block/crate_epic_lid");
        LEGENDARY_LID = new ResourceLocation("fscrates", "block/crate_legendary_lid");
        MYTHIC_LID = new ResourceLocation("fscrates", "block/crate_mythic_lid");
    }
}
