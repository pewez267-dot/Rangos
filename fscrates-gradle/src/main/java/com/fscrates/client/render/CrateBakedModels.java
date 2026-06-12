package com.fscrates.client.render;

import com.fscrates.config.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

/**
 * Modelos 3D por rareza (provenientes del 'Crates and Stuff Model Pack',
 * convertidos a modelos de bloque JSON en assets/fscrates/models/block/).
 *
 * Asignacion modelo -> rareza (por tamano natural del modelo del pack):
 *   common    <- common_crate
 *   rare      <- vote_crate
 *   epic      <- rare_crate
 *   legendary <- cosmetic_crate
 *   mythic    <- legendary_crate (el dorado / de mayor volumen)
 *
 * El bloque usa RenderShape.ENTITYBLOCK_ANIMATED, por lo que su aspecto en el
 * mundo lo dibuja CrateRenderer. Estos modelos se registran como modelos
 * adicionales para que el ModelManager los hornee y los podamos pintar a mano.
 */
public final class CrateBakedModels {
    public static final ResourceLocation COMMON = new ResourceLocation("fscrates", "block/crate_common");
    public static final ResourceLocation RARE = new ResourceLocation("fscrates", "block/crate_rare");
    public static final ResourceLocation EPIC = new ResourceLocation("fscrates", "block/crate_epic");
    public static final ResourceLocation LEGENDARY = new ResourceLocation("fscrates", "block/crate_legendary");
    public static final ResourceLocation MYTHIC = new ResourceLocation("fscrates", "block/crate_mythic");

    private CrateBakedModels() {
    }

    public static ResourceLocation locationFor(final Rarity rarity) {
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
            default: {
                return COMMON;
            }
        }
    }

    /** Registra los 5 modelos para que el ModelManager los cargue y hornee. */
    public static void registerAll(final ModelEvent.RegisterAdditional event) {
        event.register(COMMON);
        event.register(RARE);
        event.register(EPIC);
        event.register(LEGENDARY);
        event.register(MYTHIC);
    }

    /** Devuelve el modelo horneado correspondiente a la rareza dada. */
    public static BakedModel get(final Rarity rarity) {
        return Minecraft.m_91087_().m_91304_().m_119422_(locationFor(rarity));
    }
}
