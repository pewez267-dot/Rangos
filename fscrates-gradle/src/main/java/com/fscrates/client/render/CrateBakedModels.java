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

    /** Angulo (grados) al que abre la tapa. Se abre BASTANTE (100 grados, mas alla
     *  de la vertical) para que la tapa quede echada hacia atras y NO choque con el
     *  haz de luz que sale del centro del cofre. */
    public static final float OPEN_ANGLE_DEG = 100.0f;

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
            case RARE:      return new float[]{0.5f, 0.57405f, 0.81105f};
            case EPIC:      return new float[]{0.5f, 0.52543f, 0.91443f};
            case LEGENDARY:      return new float[]{0.47306f, 0.3235f, 0.71127f};
            case MYTHIC:      return new float[]{0.5f, 0.45352f, 0.84924f};
            default:        return new float[]{0.5f, 0.55052f, 0.79445f};
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

    /**
     * Escala base de renderizado por rareza (en reposo, sin animacion).
     * mythic >= legendary > las demas (common, rare, epic se quedan en 1.0).
     *
     * Las dos rarezas top deben verse CLARAMENTE mas grandes que el resto.
     * El CUERPO del modelo legendary es el mas pequeno de todos (~6.8x6.8px vs
     * ~8x7.7 de mythic y ~9.6 de epic), por eso aun a 1.90 se veia casi igual al
     * resto. Lo subimos a 2.10 para acercarlo a mythic (sin pasarlo):
     *   legendary cuerpo: 6.8 * 2.10 = 14.3px
     *   mythic    cuerpo: 8.1 * 1.80 = 14.6px  (queda apenas mas grande -> OK)
     *   legendary alto:   9.7 * 2.10 = 20.4px < mythic 12 * 1.80 = 21.6px -> OK
     *
     * Legendary = 2.10x  |  Mythic = 1.80x  (legendary queda CERCA de mythic pero
     * por debajo; su modelo es mas pequeno, por eso el numero es mayor).
     *
     * IMPORTANTE: el escalado en CrateRenderer esta anclado al suelo (y=0), por
     * lo que subir estos valores agranda el cofre hacia arriba/los lados SIN
     * hundirlo en el suelo.
     */
    public static float renderScale(final Rarity rarity) {
        if (rarity == null) return 1.0f;
        switch (rarity) {
            case LEGENDARY: return 2.10f;
            case MYTHIC:    return 1.80f;
            default:        return 1.0f;
        }
    }
}
