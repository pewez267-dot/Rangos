package com.fscrates.client.render;

import com.fscrates.config.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

/**
 * Modelos 3D por rareza (provenientes del 'Crates and Stuff Model Pack').
 *
 * Cada crate se divide en DOS modelos horneados:
 *   - base  (crate_X)       -> el cuerpo del cofre (estatico)
 *   - lid   (crate_X_lid)   -> la tapa, que se rota al abrir con la llave
 *
 * Asignacion modelo -> rareza (por tamano natural del modelo del pack):
 *   common    <- common_crate
 *   rare      <- vote_crate
 *   epic      <- rare_crate
 *   legendary <- cosmetic_crate
 *   mythic    <- legendary_crate
 *
 * El bloque usa RenderShape.ENTITYBLOCK_ANIMATED, por lo que su aspecto en el
 * mundo lo dibuja CrateRenderer. Estos modelos se registran como modelos
 * adicionales para que el ModelManager los hornee y los podamos pintar a mano.
 */
public final class CrateBakedModels {
    // --- modelos base ---
    public static final ResourceLocation COMMON = new ResourceLocation("fscrates", "block/crate_common");
    public static final ResourceLocation RARE = new ResourceLocation("fscrates", "block/crate_rare");
    public static final ResourceLocation EPIC = new ResourceLocation("fscrates", "block/crate_epic");
    public static final ResourceLocation LEGENDARY = new ResourceLocation("fscrates", "block/crate_legendary");
    public static final ResourceLocation MYTHIC = new ResourceLocation("fscrates", "block/crate_mythic");

    // --- modelos de tapa (lid) ---
    public static final ResourceLocation COMMON_LID = new ResourceLocation("fscrates", "block/crate_common_lid");
    public static final ResourceLocation RARE_LID = new ResourceLocation("fscrates", "block/crate_rare_lid");
    public static final ResourceLocation EPIC_LID = new ResourceLocation("fscrates", "block/crate_epic_lid");
    public static final ResourceLocation LEGENDARY_LID = new ResourceLocation("fscrates", "block/crate_legendary_lid");
    public static final ResourceLocation MYTHIC_LID = new ResourceLocation("fscrates", "block/crate_mythic_lid");

    /** Angulo (grados) al que abre la tapa, tomado de la animacion del pack. */
    public static final float OPEN_ANGLE_DEG = 62.5f;

    private CrateBakedModels() {
    }

    public static ResourceLocation locationFor(final Rarity rarity) {
        if (rarity == null) {
            return COMMON;
        }
        switch (rarity) {
            case RARE: return RARE;
            case EPIC: return EPIC;
            case LEGENDARY: return LEGENDARY;
            case MYTHIC: return MYTHIC;
            default: return COMMON;
        }
    }

    public static ResourceLocation lidLocationFor(final Rarity rarity) {
        if (rarity == null) {
            return COMMON_LID;
        }
        switch (rarity) {
            case RARE: return RARE_LID;
            case EPIC: return EPIC_LID;
            case LEGENDARY: return LEGENDARY_LID;
            case MYTHIC: return MYTHIC_LID;
            default: return COMMON_LID;
        }
    }

    /**
     * Bisagra (pivote) de la tapa en unidades de bloque 0..1 (x, y, z).
     * Calculada del borde trasero-inferior de la tapa de cada modelo.
     */
    public static float[] hinge(final Rarity rarity) {
        if (rarity == null) {
            return new float[]{0.5f, 0.19971f, 0.79445f};
        }
        switch (rarity) {
            case RARE:      return new float[]{0.5f, 0.39361f, 0.81105f};
            case EPIC:      return new float[]{0.5f, 0.40211f, 0.91443f};
            case LEGENDARY: return new float[]{0.47306f, 0.27729f, 0.71127f};
            case MYTHIC:    return new float[]{0.5f, 0.29358f, 0.84924f};
            default:        return new float[]{0.5f, 0.19971f, 0.79445f};
        }
    }

    /** Registra los 10 modelos (base + tapa) para que el ModelManager los hornee. */
    public static void registerAll(final ModelEvent.RegisterAdditional event) {
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
    }

    /** Modelo base (cuerpo) de la rareza dada. */
    public static BakedModel get(final Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(locationFor(rarity));
    }

    /** Modelo de la tapa (lid) de la rareza dada. */
    public static BakedModel getLid(final Rarity rarity) {
        return Minecraft.getInstance().getModelManager().getModel(lidLocationFor(rarity));
    }
}
