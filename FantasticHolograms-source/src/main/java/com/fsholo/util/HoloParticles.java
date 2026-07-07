package com.fsholo.util;

import com.fsholo.data.HoloLine;
import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * Registro de estilos de particulas para hologramas + propiedades editables.
 * Las particulas se colocan en el PLANO del texto (billboard, usando el vector derecha de la camara),
 * asi la posicion (arriba/abajo/lados/alrededor) se ve correcta desde cualquier angulo.
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

    // Propiedades editables (nombres para la GUI y multiplicadores para el spawn).
    public static final String[] ANCHOR_NAMES = new String[]{"Centro", "Arriba", "Abajo", "Izquierda", "Derecha", "Ambos Lados", "Alrededor"};
    public static final String[] SPEED_NAMES = new String[]{"Lenta", "Normal", "Rapida", "Muy Rapida"};
    public static final String[] SIZE_NAMES = new String[]{"Pequeno", "Normal", "Grande"};
    public static final String[] SPREAD_NAMES = new String[]{"Estrecha", "Normal", "Ancha"};
    private static final float[] SPEED_MULT = new float[]{0.5f, 1.0f, 1.7f, 2.4f};
    private static final float[] SIZE_MULT = new float[]{0.7f, 1.0f, 1.5f};
    private static final float[] SPREAD_MULT = new float[]{0.55f, 1.0f, 1.8f};

    private HoloParticles() {
    }

    private static final class Style {
        final String name;
        final ParticleOptions particle;
        final int pattern;
        final int dustRgb;
        final float dustScale;

        Style(String name, ParticleOptions particle, int pattern) {
            this.name = name;
            this.particle = particle;
            this.pattern = pattern;
            this.dustRgb = -1;
            this.dustScale = 1.0f;
        }

        Style(String name, int dustRgb, float dustScale, int pattern) {
            this.name = name;
            this.particle = null;
            this.pattern = pattern;
            this.dustRgb = dustRgb;
            this.dustScale = dustScale;
        }
    }

    private static ParticleOptions dust(int rgb, float scale) {
        float r = (float) ((rgb >> 16) & 0xFF) / 255.0f;
        float g = (float) ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (float) (rgb & 0xFF) / 255.0f;
        return new DustParticleOptions(new Vector3f(r, g, b), Math.max(0.3f, scale));
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
        new Style("Polvo Rojo", 0xFF3030, 1.1f, SPIRAL),
        new Style("Polvo Azul", 0x3080FF, 1.1f, SPIRAL),
        new Style("Polvo Verde", 0x30FF60, 1.1f, SPIRAL),
        new Style("Polvo Rosa", 0xFF66CC, 1.1f, HALO),
        new Style("Polvo Dorado", 0xFFD700, 1.1f, HALO),
        new Style("Polvo Purpura", 0xB266FF, 1.1f, SPIRAL),
        new Style("Polvo Cian", 0x30FFFF, 1.1f, HALO),
        new Style("Polvo Blanco", 0xFFFFFF, 1.0f, SPARKLE),
        new Style("Chispas Doradas", 0xFFD700, 0.9f, SPARKLE),
        new Style("Polvo Arcoiris", -2, 1.1f, SPIRAL)
    };

    public static int count() {
        return STYLES.length;
    }

    public static String name(int i) {
        return STYLES[Math.floorMod(i, STYLES.length)].name;
    }

    public static int anchorCount() {
        return ANCHOR_NAMES.length;
    }

    public static String anchorName(int i) {
        return ANCHOR_NAMES[Math.floorMod(i, ANCHOR_NAMES.length)];
    }

    public static int speedCount() {
        return SPEED_NAMES.length;
    }

    public static String speedName(int i) {
        return SPEED_NAMES[Math.floorMod(i, SPEED_NAMES.length)];
    }

    public static int sizeCount() {
        return SIZE_NAMES.length;
    }

    public static String sizeName(int i) {
        return SIZE_NAMES[Math.floorMod(i, SIZE_NAMES.length)];
    }

    public static int spreadCount() {
        return SPREAD_NAMES.length;
    }

    public static String spreadName(int i) {
        return SPREAD_NAMES[Math.floorMod(i, SPREAD_NAMES.length)];
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static ParticleOptions resolve(Style st, float sizeMult) {
        if (st.dustRgb == -1) {
            return st.particle;
        }
        int rgb = st.dustRgb;
        if (st.dustRgb == -2) {
            rgb = Color.HSBtoRGB((float) ((double) (System.currentTimeMillis() % 4000L) / 4000.0), 1.0f, 1.0f) & 0xFFFFFF;
        }
        return dust(rgb, st.dustScale * sizeMult);
    }

    public static void spawn(ClientLevel level, double cx, double cy, double cz, double rightX, double rightZ, HoloLine line, RandomSource rnd) {
        Style st = STYLES[Math.floorMod(line.particleStyle, STYLES.length)];
        float sizeMult = SIZE_MULT[clamp(line.particleSize, 0, SIZE_MULT.length - 1)];
        float spreadMult = SPREAD_MULT[clamp(line.particleSpread, 0, SPREAD_MULT.length - 1)];
        float speedMult = SPEED_MULT[clamp(line.particleSpeed, 0, SPEED_MULT.length - 1)];
        int count = clamp(line.particleDensity, 1, 4);
        int anchor = line.particleAnchor;
        double hw = 0.6 * (double) spreadMult;
        double t = (double) (System.currentTimeMillis() % 6283L) / 1000.0;
        for (int k = 0; k < count; ++k) {
            ParticleOptions p = resolve(st, sizeMult);
            if (p == null) {
                return;
            }
            double pu = 0.0;
            double pv = 0.0;
            double vy = 0.0;
            switch (st.pattern) {
                case RISE:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = -0.35;
                    vy = 0.04;
                    break;
                case FALL:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = 0.5;
                    vy = -0.02;
                    break;
                case HALO: {
                    double a = t + (double) rnd.nextFloat() * 0.6;
                    pu = Math.cos(a) * 0.5 * (double) spreadMult;
                    pv = Math.sin(a) * 0.28;
                    break;
                }
                case ORBIT: {
                    double a = t * 1.5;
                    pu = Math.cos(a) * 0.55 * (double) spreadMult;
                    pv = Math.sin(t * 2.0) * 0.18;
                    break;
                }
                case SPIRAL: {
                    double a = t * 3.0;
                    double rise = (double) (System.currentTimeMillis() % 1400L) / 1400.0;
                    pu = Math.cos(a) * 0.45 * (double) spreadMult;
                    pv = -0.35 + rise * 0.75;
                    break;
                }
                case CLOUD:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * 0.55;
                    break;
                case SPARKLE:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * 0.4;
                    break;
                default:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * 0.5;
                    vy = 0.02;
                    break;
            }
            double uOff = 0.0;
            double vOff = 0.0;
            double side = 0.55 + hw;
            switch (anchor) {
                case 1:
                    vOff = 0.5;
                    break;
                case 2:
                    vOff = -0.45;
                    break;
                case 3:
                    uOff = -side;
                    break;
                case 4:
                    uOff = side;
                    break;
                case 5:
                    uOff = rnd.nextBoolean() ? side : -side;
                    break;
                case 6: {
                    double bw = hw + 0.2;
                    double bh = 0.42;
                    double f = (double) rnd.nextFloat() * 2.0 - 1.0;
                    int s = rnd.nextInt(4);
                    if (s == 0) {
                        pu = f * bw;
                        pv = bh;
                    } else if (s == 1) {
                        pu = f * bw;
                        pv = -bh;
                    } else if (s == 2) {
                        pu = -bw;
                        pv = f * bh;
                    } else {
                        pu = bw;
                        pv = f * bh;
                    }
                    vy = 0.0;
                    break;
                }
                default:
                    break;
            }
            double u = uOff + pu;
            double v = vOff + pv;
            double wx = cx + rightX * u;
            double wy = cy + v;
            double wz = cz + rightZ * u;
            level.addParticle(p, wx, wy, wz, 0.0, vy * (double) speedMult, 0.0);
        }
    }
}
