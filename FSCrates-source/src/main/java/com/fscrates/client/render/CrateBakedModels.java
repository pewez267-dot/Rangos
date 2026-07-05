package com.fscrates.client.render;

import com.fscrates.client.render.CrateStyles;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

public final class CrateBakedModels {
    public static final ResourceLocation COMMON = new ResourceLocation("fscrates", "block/crate_common");
    public static final ResourceLocation RARE = new ResourceLocation("fscrates", "block/crate_rare");
    public static final ResourceLocation EPIC = new ResourceLocation("fscrates", "block/crate_epic");
    public static final ResourceLocation LEGENDARY = new ResourceLocation("fscrates", "block/crate_legendary");
    public static final ResourceLocation MYTHIC = new ResourceLocation("fscrates", "block/crate_mythic");
    public static final ResourceLocation COMMON_LID = new ResourceLocation("fscrates", "block/crate_common_lid");
    public static final ResourceLocation RARE_LID = new ResourceLocation("fscrates", "block/crate_rare_lid");
    public static final ResourceLocation EPIC_LID = new ResourceLocation("fscrates", "block/crate_epic_lid");
    public static final ResourceLocation LEGENDARY_LID = new ResourceLocation("fscrates", "block/crate_legendary_lid");
    public static final ResourceLocation MYTHIC_LID = new ResourceLocation("fscrates", "block/crate_mythic_lid");
    public static final float OPEN_ANGLE_DEG = 100.0f;

    private CrateBakedModels() {
    }

    public static ResourceLocation locationFor(Rarity rarity) {
        if (rarity == null) {
            return COMMON;
        }
        switch (rarity) {
            case RARE: {
                return RARE;
            }
            case EPIC: {
                return EPIC;
            }
            case LEGENDARY: {
                return LEGENDARY;
            }
            case MYTHIC: {
                return MYTHIC;
            }
        }
        return COMMON;
    }

    public static ResourceLocation lidLocationFor(Rarity rarity) {
        if (rarity == null) {
            return COMMON_LID;
        }
        switch (rarity) {
            case RARE: {
                return RARE_LID;
            }
            case EPIC: {
                return EPIC_LID;
            }
            case LEGENDARY: {
                return LEGENDARY_LID;
            }
            case MYTHIC: {
                return MYTHIC_LID;
            }
        }
        return COMMON_LID;
    }

    public static float[] hinge(Rarity rarity) {
        if (rarity == null) {
            return new float[]{0.5f, 0.19971f, 0.79445f};
        }
        switch (rarity) {
            case RARE: {
                return new float[]{0.5f, 0.57405f, 0.81105f};
            }
            case EPIC: {
                return new float[]{0.5f, 0.52543f, 0.91443f};
            }
            case LEGENDARY: {
                return new float[]{0.47306f, 0.3235f, 0.71127f};
            }
            case MYTHIC: {
                return new float[]{0.5f, 0.45352f, 0.84924f};
            }
        }
        return new float[]{0.5f, 0.55052f, 0.79445f};
    }

    public static void registerAll(ModelEvent.RegisterAdditional event) {
        event.register(COMMON);
        event.register(RARE);
        event.register(EPIC);
        event.register(LEGENDARY);
        event.register(MYTHIC);
        event.register(COMMON_LID);
        event.register(RARE_LID);
        event.register(EPIC_LID);
        event.register(LEGENDARY_LID);
        event.register(MYTHIC_LID);
        for (CrateStyles.Style s : CrateStyles.all()) {
            event.register(s.base);
            if (s.lid == null) continue;
            event.register(s.lid);
        }
    }

    public static BakedModel get(Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(CrateBakedModels.locationFor(rarity));
    }

    public static BakedModel getLid(Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(CrateBakedModels.lidLocationFor(rarity));
    }

    public static float renderScale(Rarity rarity) {
        return rarity == null ? 1.0f : rarity.sizeScale();
    }

    public static BakedModel baseModel(CrateConfig cfg) {
        CrateStyles.Style s;
        CrateStyles.Style style = s = cfg == null ? null : CrateStyles.get(cfg.styleId);
        ResourceLocation rl = s != null ? s.base : CrateBakedModels.locationFor(cfg == null ? null : cfg.rarity);
        return Minecraft.getInstance().getModelManager().getModel(rl);
    }

    public static BakedModel lidModel(CrateConfig cfg) {
        CrateStyles.Style s;
        CrateStyles.Style style = s = cfg == null ? null : CrateStyles.get(cfg.styleId);
        if (s != null) {
            return s.hasLid() ? Minecraft.getInstance().getModelManager().getModel(s.lid) : null;
        }
        return CrateBakedModels.getLid(cfg == null ? null : cfg.rarity);
    }

    public static float[] hingeFor(CrateConfig cfg) {
        CrateStyles.Style s;
        CrateStyles.Style style = s = cfg == null ? null : CrateStyles.get(cfg.styleId);
        if (s != null && s.hasLid()) {
            return s.hinge;
        }
        return CrateBakedModels.hinge(cfg == null ? null : cfg.rarity);
    }

    public static float scaleFor(CrateConfig cfg) {
        CrateStyles.Style s;
        CrateStyles.Style style = s = cfg == null ? null : CrateStyles.get(cfg.styleId);
        return s != null ? s.scale : CrateBakedModels.renderScale(cfg == null ? null : cfg.rarity);
    }
}

