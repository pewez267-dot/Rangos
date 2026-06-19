package com.fantasticterraform.ambience;

import java.util.UUID;

/**
 * Zona de ambiente sonoro asociada a una region cuboide. Cuando el jugador entra,
 * el cliente reproduce el sonido (con fade in/out); al salir, lo detiene.
 */
public final class AmbienceZone {

    public String id = UUID.randomUUID().toString();
    public String dimension = "minecraft:overworld";
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    /** ResourceLocation del sonido (vanilla o custom via resourcepack). */
    public String sound = "minecraft:ambient.cave";
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public boolean loop = true;
    public double fadeSeconds = 2.0D;

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX + 1
                && y >= minY && y <= maxY + 1
                && z >= minZ && z <= maxZ + 1;
    }
}
