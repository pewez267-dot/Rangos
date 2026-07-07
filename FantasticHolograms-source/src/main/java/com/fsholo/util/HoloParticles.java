package com.fsholo.util;

import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * Registro de estilos de particulas para los hologramas. Cada estilo combina un tipo de
 * particula con un patron de movimiento (halo, ascenso, caida, orbita, nube, espiral, destello, aura).
 * Todo es del lado cliente; lo llama HologramRenderer.
 */
public final class HoloParticles {
    private static final int HALO = 0;
    private static final int RISE = 1;
    private static final int FALL = 2;
    private static final int ORBIT = 3;
    private static final int CLOUD = 4;
    private static final int SPIRAL = 5;
    private static final int SPARKLE = 6;
    private static final int AURA = 7;

    private HoloParticles() {
    }

    private static final class Style {
        final String name;
        final ParticleOptions particle;
        final int pattern;
        final boolean rainbowDust;

        Style(String name, ParticleOptions particle, int pattern) {
            this(name, particle, pattern, false);
        }

        Style(String name, ParticleOptions particle, int pattern, boolean rainbowDust) {
            this.name = name;
            this.particle = particle;
            this.pattern = pattern;
            this.rainbowDust = rainbowDust;
        }
    }

    private static ParticleOptions dust(int rgb, float scale) {
        float r = (float) ((rgb >> 16) & 0xFF) / 255.0f;
        float g = (float) ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (float) (rgb & 0xFF) / 255.0f;
        return new DustParticleOptions(new Vector3f(r, g, b), scale);
    }

    private static final Style[] STYLES = new Style[]{
        new Style("Llamas", ParticleTypes.FLAME, RISE),
        new Style("Llamas del Alma", ParticleTypes.SOUL_FIRE_FLAME, RISE),
        new Style("Corazones", ParticleTypes.HEART, RISE),
        new Style("Chispas Felices", ParticleTypes.HAPPY_VILLAGER, AURA),
        new Style("Magia Encantada", ParticleTypes.ENCHANT, SPIRAL),
        new Style("Portal", ParticleTypes.PORTAL, HALO),
        new Style("Vara del End", ParticleTypes.END_ROD, RISE),
        new Style("Notas Musicales", ParticleTypes.NOTE, RISE),
        new Style("Copos de Nieve", ParticleTypes.SNOWFLAKE, FALL),
        new Style("Chispa Electrica", ParticleTypes.ELECTRIC_SPARK, SPARKLE),
        new Style("Almas", ParticleTypes.SOUL, RISE),
        new Style("Aliento de Dragon", ParticleTypes.DRAGON_BREATH, HALO),
        new Style("Destello", ParticleTypes.GLOW, ORBIT),
        new Style("Petalos de Cerezo", ParticleTypes.CHERRY_LEAVES, FALL),
        new Style("Totem", ParticleTypes.TOTEM_OF_UNDYING, AURA),
        new Style("Fuegos Artificiales", ParticleTypes.FIREWORK, SPARKLE),
        new Style("Nubes", ParticleTypes.CLOUD, HALO),
        new Style("Humo", ParticleTypes.SMOKE, RISE),
        new Style("Lava Goteante", ParticleTypes.DRIPPING_LAVA, FALL),
        new Style("Bruja", ParticleTypes.WITCH, AURA),
        new Style("Ceniza", ParticleTypes.ASH, FALL),
        new Style("Ceniza Blanca", ParticleTypes.WHITE_ASH, FALL),
        new Style("Esporas del Alma", ParticleTypes.WARPED_SPORE, CLOUD),
        new Style("Esporas Carmesi", ParticleTypes.CRIMSON_SPORE, CLOUD),
        new Style("Flor de Espora", ParticleTypes.SPORE_BLOSSOM_AIR, FALL),
        new Style("Critico", ParticleTypes.CRIT, SPARKLE),
        new Style("Golpe Magico", ParticleTypes.ENCHANTED_HIT, SPARKLE),
        new Style("Humo de Fogata", ParticleTypes.CAMPFIRE_COSY_SMOKE, RISE),
        new Style("Estornudo", ParticleTypes.SNEEZE, HALO),
        new Style("Miel Goteante", ParticleTypes.DRIPPING_HONEY, FALL),
        new Style("Burbujas", ParticleTypes.BUBBLE, RISE),
        new Style("Tinta Brillante", ParticleTypes.GLOW_SQUID_INK, HALO),
        new Style("Nube Poof", ParticleTypes.POOF, SPARKLE),
        new Style("Anillo de Fuego", ParticleTypes.FLAME, HALO),
        new Style("Anillo del Alma", ParticleTypes.SOUL_FIRE_FLAME, HALO),
        new Style("Espiral del End", ParticleTypes.END_ROD, SPIRAL),
        new Style("Corazones Orbitando", ParticleTypes.HEART, ORBIT),
        new Style("Lluvia de Notas", ParticleTypes.NOTE, FALL),
        new Style("Tormenta Electrica", ParticleTypes.ELECTRIC_SPARK, HALO),
        new Style("Espiral Encantada", ParticleTypes.ENCHANT, ORBIT),
        new Style("Polvo Rojo", dust(0xFF3030, 1.1f), SPIRAL),
        new Style("Polvo Azul", dust(0x3080FF, 1.1f), SPIRAL),
        new Style("Polvo Verde", dust(0x30FF60, 1.1f), SPIRAL),
        new Style("Polvo Rosa", dust(0xFF66CC, 1.1f), HALO),
        new Style("Polvo Dorado", dust(0xFFD700, 1.1f), HALO),
        new Style("Polvo Purpura", dust(0xB266FF, 1.1f), SPIRAL),
        new Style("Polvo Cian", dust(0x30FFFF, 1.1f), HALO),
        new Style("Polvo Blanco", dust(0xFFFFFF, 1.0f), SPARKLE),
        new Style("Chispas Doradas", dust(0xFFD700, 0.9f), SPARKLE),
        new Style("Polvo Arcoiris", null, SPIRAL, true)
    };

    public static int count() {
        return STYLES.length;
    }

    public static String name(int i) {
        return STYLES[Math.floorMod(i, STYLES.length)].name;
    }

    public static void spawn(ClientLevel level, double cx, double cy, double cz, int style, RandomSource rnd) {
        Style st = STYLES[Math.floorMod(style, STYLES.length)];
        ParticleOptions p = st.particle;
        if (st.rainbowDust) {
            int rgb = Color.HSBtoRGB((float) ((double) (System.currentTimeMillis() % 4000L) / 4000.0), 1.0f, 1.0f) & 0xFFFFFF;
            p = dust(rgb, 1.1f);
        }
        if (p == null) {
            return;
        }
        double hw = 0.6;
        double t = (double) (System.currentTimeMillis() % 6283L) / 1000.0;
        switch (st.pattern) {
            case HALO: {
                double a = t + (double) rnd.nextFloat() * 0.6;
                double rad = 0.55;
                level.addParticle(p, cx + Math.cos(a) * rad, cy + 0.12 + ((double) rnd.nextFloat() - 0.5) * 0.1, cz + Math.sin(a) * rad, 0.0, 0.0, 0.0);
                break;
            }
            case RISE: {
                double x = cx + ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                double z = cz + ((double) rnd.nextFloat() - 0.5) * 0.25;
                level.addParticle(p, x, cy - 0.35, z, 0.0, 0.04, 0.0);
                break;
            }
            case FALL: {
                double x = cx + ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                double z = cz + ((double) rnd.nextFloat() - 0.5) * 0.25;
                level.addParticle(p, x, cy + 0.5, z, 0.0, -0.02, 0.0);
                break;
            }
            case ORBIT: {
                double a = t * 1.5;
                double rad = 0.6;
                level.addParticle(p, cx + Math.cos(a) * rad, cy + 0.12 + Math.sin(t * 2.0) * 0.15, cz + Math.sin(a) * rad, 0.0, 0.0, 0.0);
                break;
            }
            case CLOUD: {
                double x = cx + ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                double y = cy + ((double) rnd.nextFloat() - 0.5) * 0.5;
                double z = cz + ((double) rnd.nextFloat() - 0.5) * 0.5;
                level.addParticle(p, x, y, z, 0.0, 0.0, 0.0);
                break;
            }
            case SPIRAL: {
                double a = t * 3.0;
                double rad = 0.5;
                double yy = cy - 0.35 + (double) (System.currentTimeMillis() % 1400L) / 1400.0 * 0.75;
                level.addParticle(p, cx + Math.cos(a) * rad, yy, cz + Math.sin(a) * rad, 0.0, 0.0, 0.0);
                break;
            }
            case SPARKLE: {
                double x = cx + ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                double y = cy + ((double) rnd.nextFloat() - 0.5) * 0.35;
                double z = cz + ((double) rnd.nextFloat() - 0.5) * 0.15;
                level.addParticle(p, x, y, z, 0.0, 0.0, 0.0);
                break;
            }
            default: {
                double x = cx + ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                double y = cy + ((double) rnd.nextFloat() - 0.5) * 0.45;
                double z = cz + ((double) rnd.nextFloat() - 0.5) * 0.35;
                level.addParticle(p, x, y, z, 0.0, 0.02, 0.0);
                break;
            }
        }
    }
}
