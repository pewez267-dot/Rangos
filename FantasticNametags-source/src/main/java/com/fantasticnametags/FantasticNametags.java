package com.fantasticnametags;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Fantastic Nametags: sube las lineas del nametag (nombre + lineas extra de otros mods como
 * FantasticPass / FantasticRanks) por encima de la cabeza, de forma uniforme.
 *
 * No usa mixins: se engancha al evento RenderNameTagEvent de Forge y desplaza el PoseStack,
 * lo que afecta al nombre vanilla Y a las lineas que otros mods dibujan en ese mismo render.
 *
 * La configuracion es de tipo SERVER: Forge la sincroniza automaticamente a todos los clientes
 * conectados, asi el operador la ajusta UNA vez en el server y todos la reciben.
 */
@Mod(FantasticNametags.MODID)
public class FantasticNametags {
    public static final String MODID = "fantasticnametags";

    public FantasticNametags() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, NametagConfig.SPEC);
    }
}
