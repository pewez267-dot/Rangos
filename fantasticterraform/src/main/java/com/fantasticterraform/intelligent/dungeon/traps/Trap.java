package com.fantasticterraform.intelligent.dungeon.traps;

import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Una trampa funcional. {@code build} coloca el mecanismo, el trigger y el cableado de
 * redstone necesario alrededor de un punto de paso, devolviendo las colocaciones.
 */
public interface Trap {

    String id();

    /**
     * @param walkPoint punto del piso por donde pasaria el jugador (donde va el trigger)
     * @param facing    direccion de avance del jugador (el mecanismo apunta hacia el)
     */
    List<Placement> build(ServerLevel level, BlockPos walkPoint, Direction facing, RandomSource rnd, DungeonTheme theme);
}
