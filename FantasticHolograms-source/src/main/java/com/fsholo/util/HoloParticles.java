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
 * Las particulas se colocan en el PLANO del texto (billboard) y se ADAPTAN al tamano real
 * del holograma (ancho/alto medidos por el render). Soporta posicion por ancla, offsets numericos
 * (alto/lado), densidad, velocidad, tamano y dispersion.
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

    public static final String[] ANCHOR_NAMES = new String[]{"Centro", "Arriba", "Abajo", "Izquierda", "Derecha", "Ambos Lados", "Alrededor"};
    public static final String[] SPEED_NAMES = new String[]{"Lenta", "Normal", "R\u00e1pida", "Muy R\u00e1pida"};
    public static final String[] SIZE_NAMES = new String[]{"Peque\u00f1o", "Normal", "Grande"};
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
        new Style("Chispa El\u00e9ctrica", ParticleTypes.ELECTRIC_SPARK, SPARKLE),
        new Style("Almas", ParticleTypes.SOUL, RISE),
        new Style("Aliento de Drag\u00f3n", ParticleTypes.DRAGON_BREATH, HALO),
        new Style("Destello", ParticleTypes.GLOW, ORBIT),
        new Style("P\u00e9talos de Cerezo", ParticleTypes.CHERRY_LEAVES, FALL),
        new Style("T\u00f3tem", ParticleTypes.TOTEM_OF_UNDYING, AURA),
        new Style("Fuegos Artificiales", ParticleTypes.FIREWORK, SPARKLE),
        new Style("Nubes", ParticleTypes.CLOUD, HALO),
        new Style("Humo", ParticleTypes.SMOKE, RISE),
        new Style("Lava Goteante", ParticleTypes.DRIPPING_LAVA, FALL),
        new Style("Bruja", ParticleTypes.WITCH, AURA),
        new Style("Ceniza", ParticleTypes.ASH, FALL),
        new Style("Ceniza Blanca", ParticleTypes.WHITE_ASH, FALL),
        new Style("Esporas del Alma", ParticleTypes.WARPED_SPORE, CLOUD),
        new Style("Esporas Carmes\u00ed", ParticleTypes.CRIMSON_SPORE, CLOUD),
        new Style("Flor de Espora", ParticleTypes.SPORE_BLOSSOM_AIR, FALL),
        new Style("Cr\u00edtico", ParticleTypes.CRIT, SPARKLE),
        new Style("Golpe M\u00e1gico", ParticleTypes.ENCHANTED_HIT, SPARKLE),
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
        new Style("Tormenta El\u00e9ctrica", ParticleTypes.ELECTRIC_SPARK, HALO),
        new Style("Espiral Encantada", ParticleTypes.ENCHANT, ORBIT),
        new Style("Polvo Rojo", 0xFF3030, 1.1f, SPIRAL),
        new Style("Polvo Azul", 0x3080FF, 1.1f, SPIRAL),
        new Style("Polvo Verde", 0x30FF60, 1.1f, SPIRAL),
        new Style("Polvo Rosa", 0xFF66CC, 1.1f, HALO),
        new Style("Polvo Dorado", 0xFFD700, 1.1f, HALO),
        new Style("Polvo P\u00farpura", 0xB266FF, 1.1f, SPIRAL),
        new Style("Polvo Cian", 0x30FFFF, 1.1f, HALO),
        new Style("Polvo Blanco", 0xFFFFFF, 1.0f, SPARKLE),
        new Style("Chispas Doradas", 0xFFD700, 0.9f, SPARKLE),
        new Style("Polvo Arco\u00edris", -2, 1.1f, SPIRAL),
        new Style("Delf\u00edn", ParticleTypes.DOLPHIN, AURA),
        new Style("Lava", ParticleTypes.LAVA, RISE),
        new Style("Salpicadura", ParticleTypes.SPLASH, FALL),
        new Style("Agua Goteante", ParticleTypes.DRIPPING_WATER, FALL),
        new Style("Humo Se\u00f1al", ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, RISE),
        new Style("Miel Cayendo", ParticleTypes.FALLING_HONEY, FALL),
        new Style("Tinta de Calamar", ParticleTypes.SQUID_INK, CLOUD),
        new Style("Cera", ParticleTypes.WAX_ON, SPARKLE),
        new Style("Raspado", ParticleTypes.SCRAPE, SPARKLE),
        new Style("Explosi\u00f3n", ParticleTypes.EXPLOSION, SPARKLE),
        new Style("Destello Flash", ParticleTypes.FLASH, SPARKLE),
        new Style("Corazones Halo", ParticleTypes.HEART, HALO),
        new Style("Almas Halo", ParticleTypes.SOUL, HALO),
        new Style("Nieve Espiral", ParticleTypes.SNOWFLAKE, SPIRAL),
        new Style("Portal Espiral", ParticleTypes.PORTAL, SPIRAL),
        new Style("Notas Halo", ParticleTypes.NOTE, HALO),
        new Style("Polvo Naranja", 0xFF8800, 1.1f, SPIRAL),
        new Style("Polvo Turquesa", 0x00E5D0, 1.1f, HALO),
        new Style("Polvo Lima", 0xB6FF00, 1.1f, ORBIT),
        new Style("Estrellas Doradas", 0xFFE066, 1.0f, ORBIT)
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

    /**
     * @param cx,cy,cz  centro del texto (cy = centro vertical real)
     * @param rightX,rightZ vector unitario "derecha" del texto (plano billboard)
     * @param halfW,halfH  medio-ancho y medio-alto REALES del texto en bloques
     */
    public static void spawn(ClientLevel level, double cx, double cy, double cz, double rightX, double rightZ, double halfW, double halfH, HoloLine line, RandomSource rnd) {
        Style st = STYLES[Math.floorMod(line.particleStyle, STYLES.length)];
        float sizeMult = SIZE_MULT[clamp(line.particleSize, 0, SIZE_MULT.length - 1)];
        float spreadMult = SPREAD_MULT[clamp(line.particleSpread, 0, SPREAD_MULT.length - 1)];
        float speedMult = SPEED_MULT[clamp(line.particleSpeed, 0, SPEED_MULT.length - 1)];
        int count = clamp(line.particleDensity, 1, 4);
        int anchor = line.particleAnchor;
        // Extentes adaptados al tamano del holograma (con minimos para textos muy cortos).
        double hw = Math.max(0.18, halfW) * (double) spreadMult;
        double vh = Math.max(0.12, halfH);
        double marginU = 0.12 + hw * 0.15;
        double marginV = 0.1 + vh * 0.2;
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
                    pv = -vh - 0.05;
                    vy = 0.04;
                    break;
                case FALL:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = vh + 0.1;
                    vy = -0.02;
                    break;
                case HALO: {
                    double a = t + (double) rnd.nextFloat() * 0.5;
                    pu = Math.cos(a) * (hw + marginU);
                    pv = Math.sin(a) * (vh + marginV);
                    break;
                }
                case ORBIT: {
                    double a = t * 1.5;
                    pu = Math.cos(a) * (hw + marginU);
                    pv = Math.sin(t * 2.0) * (vh * 0.6);
                    break;
                }
                case SPIRAL: {
                    double a = t * 3.0;
                    double rise = (double) (System.currentTimeMillis() % 1400L) / 1400.0;
                    pu = Math.cos(a) * (hw * 0.85);
                    pv = -vh + rise * (2.0 * vh + 0.1);
                    break;
                }
                case CLOUD:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * 2.0 * vh;
                    break;
                case SPARKLE:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * 2.0 * vh;
                    break;
                default:
                    pu = ((double) rnd.nextFloat() - 0.5) * 2.0 * hw;
                    pv = ((double) rnd.nextFloat() - 0.5) * (2.0 * vh + 0.1);
                    vy = 0.02;
                    break;
            }
            double uOff = 0.0;
            double vOff = 0.0;
            double sideU = hw + marginU + 0.1;
            switch (anchor) {
                case 1:
                    vOff = vh + marginV + 0.08;
                    break;
                case 2:
                    vOff = -(vh + marginV + 0.05);
                    break;
                case 3:
                    uOff = -sideU;
                    break;
                case 4:
                    uOff = sideU;
                    break;
                case 5:
                    uOff = rnd.nextBoolean() ? sideU : -sideU;
                    break;
                case 6: {
                    double bw = hw + marginU;
                    double bh = vh + marginV;
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
            double u = uOff + pu + (double) line.particleOffX;
            double v = vOff + pv + (double) line.particleOffY;
            double wx = cx + rightX * u;
            double wy = cy + v;
            double wz = cz + rightZ * u;
            level.addParticle(p, wx, wy, wz, 0.0, vy * (double) speedMult, 0.0);
        }
    }
}
