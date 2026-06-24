package com.fantasticterraform.intelligent.dungeon.themes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Un tema de dungeon es un CONJUNTO DE PARAMETROS del algoritmo (paleta de bloques,
 * mobs de spawner, bloque de acento/decoracion), nunca una estructura prefabricada de
 * archivo. El mismo algoritmo de grafos usa estos parametros para materializar.
 */
public abstract class DungeonTheme {

    public abstract String id();

    public abstract String displayName();

    public abstract BlockState wall();

    public abstract BlockState floor();

    public abstract BlockState ceiling();

    public abstract BlockState pillar();

    public abstract BlockState light();

    public abstract BlockState accent();

    public abstract List<EntityType<?>> spawnerMobs();

    public static DungeonTheme byId(String id) {
        if (id == null) {
            return new CatacombsTheme();
        }
        switch (id) {
            case "ruined_fortress":
                return new RuinedFortressTheme();
            case "spider_cave":
                return new SpiderCaveTheme();
            case "abandoned_castle":
                return new AbandonedCastleTheme();
            case "ancient_crypt":
                return new AncientCryptTheme();
            case "mystic_elven":
                return new MysticElvenLairTheme();
            case "catacombs":
            default:
                return new CatacombsTheme();
        }
    }
}
