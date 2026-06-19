package com.fantasticterraform.brushes;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Configuracion de brush controlada desde el HUD: tipo, radio, bloque, intensidad y
 * altura (para el cilindro).
 */
public final class BrushSettings {

    public String brushId = "sphere";
    public int radius = 5;
    public BlockState block = Blocks.STONE.defaultBlockState();
    public double intensity = 0.5D;
    public int height = 5;

    public BrushSettings copy() {
        BrushSettings s = new BrushSettings();
        s.brushId = this.brushId;
        s.radius = this.radius;
        s.block = this.block;
        s.intensity = this.intensity;
        s.height = this.height;
        return s;
    }
}
