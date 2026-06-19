package com.fantasticterraform.brushes;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Configuracion de brush controlada desde el HUD: tipo, radio, bloque, intensidad,
 * altura, curva de atenuacion (falloff), bloque secundario, mezcla (scatter), profundidad
 * y modo hueco. Cada campo es leido por uno o varios brushes.
 */
public final class BrushSettings {

    public String brushId = "sphere";
    public int radius = 5;
    public BlockState block = Blocks.STONE.defaultBlockState();
    public double intensity = 0.5D;
    public int height = 5;

    /** Curva de borde de los brushes de colocacion/escultura. */
    public Falloff falloff = Falloff.SMOOTH;
    /** Bloque secundario para mezcla/scatter (NoisePaint, Sphere con mezcla, Overlay). */
    public BlockState secondaryBlock = Blocks.COBBLESTONE.defaultBlockState();
    /** Proporcion de mezcla del bloque secundario [0,1] (0 = solo primario). */
    public double mix = 0.0D;
    /** Profundidad en bloques para brushes de superficie (Overlay/NoisePaint). */
    public int depth = 1;
    /** Modo hueco: solo la cascara de la forma (Sphere/Cylinder). */
    public boolean hollow = false;

    public BrushSettings copy() {
        BrushSettings s = new BrushSettings();
        s.brushId = this.brushId;
        s.radius = this.radius;
        s.block = this.block;
        s.intensity = this.intensity;
        s.height = this.height;
        s.falloff = this.falloff;
        s.secondaryBlock = this.secondaryBlock;
        s.mix = this.mix;
        s.depth = this.depth;
        s.hollow = this.hollow;
        return s;
    }
}
