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
 * Sistema de particulas para hologramas. El TIPO de particula y el MOVIMIENTO se eligen por
 * separado (como el Shape de FSCrates), y ademas se editan posicion, offsets numericos,
 * densidad, velocidad, tamano y dispersion. Las particulas se colocan en el plano del texto
 * y se ADAPTAN al tamano real del holograma.
 */
public final class HoloParticles {
    // Patrones de movimiento
    private static final int RISE = 0;
    private static final int FALL = 1;
    private static final int HALO = 2;
    private static final int ORBIT = 3;
    private static final int SPIRAL = 4;
    private static final int CLOUD = 5;
    private static final int SPARKLE = 6;
    private static final int AURA = 7;
    private static final int WAVE = 8;

    public static final String[] MOVEMENT_NAMES = new String[]{"Ascender", "Caer", "Halo", "\u00d3rbita", "Espiral", "Nube", "Destello", "Aura", "Onda"};
    public static final String[] ANCHOR_NAMES = new String[]{"Centro", "Arriba", "Abajo", "Izquierda", "Derecha", "Ambos Lados", "Alrededor"};
    public static final String[] SPEED_NAMES = new String[]{"Lenta", "Normal", "R\u00e1pida", "Muy R\u00e1pida"};
    public static final String[] SIZE_NAMES = new String[]{"Peque\u00f1o", "Normal", "Grande"};
    public static final String[] SPREAD_NAMES = new String[]{"Estrecha", "Normal", "Ancha"};
    private static final float[] SPEED_MULT = new float[]{0.5f, 1.0f, 1.7f, 2.4f};
    private static final float[] SIZE_MULT = new float[]{0.7f, 1.0f, 1.5f};
    private static final float[] SPREAD_MULT = new float[]{0.55f, 1.0f, 1.8f};

    private HoloParticles() {
    }

    private static final class Type {
        final String name;
        final ParticleOptions particle;
        final int dustRgb;
        final float dustScale;

        Type(String name, ParticleOptions particle) {
            this.name = name;
            this.particle = particle;
            this.dustRgb = -1;
            this.dustScale = 1.0f;
        }

        Type(String name, int dustRgb, float dustScale) {
            this.name = name;
            this.particle = null;
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

    private static final Type[] TYPES = new Type[]{
        new Type("Llamas", ParticleTypes.FLAME),
        new Type("Llamas del Alma", ParticleTypes.SOUL_FIRE_FLAME),
        new Type("Corazones", ParticleTypes.HEART),
        new Type("Chispas Felices", ParticleTypes.HAPPY_VILLAGER),
        new Type("Magia Encantada", ParticleTypes.ENCHANT),
        new Type("Portal", ParticleTypes.PORTAL),
        new Type("Vara del End", ParticleTypes.END_ROD),
        new Type("Notas Musicales", ParticleTypes.NOTE),
        new Type("Copos de Nieve", ParticleTypes.SNOWFLAKE),
        new Type("Chispa El\u00e9ctrica", ParticleTypes.ELECTRIC_SPARK),
        new Type("Almas", ParticleTypes.SOUL),
        new Type("Aliento de Drag\u00f3n", ParticleTypes.DRAGON_BREATH),
        new Type("Destello", ParticleTypes.GLOW),
        new Type("P\u00e9talos de Cerezo", ParticleTypes.CHERRY_LEAVES),
        new Type("T\u00f3tem", ParticleTypes.TOTEM_OF_UNDYING),
        new Type("Fuegos Artificiales", ParticleTypes.FIREWORK),
        new Type("Nubes", ParticleTypes.CLOUD),
        new Type("Humo", ParticleTypes.SMOKE),
        new Type("Lava Goteante", ParticleTypes.DRIPPING_LAVA),
        new Type("Bruja", ParticleTypes.WITCH),
        new Type("Ceniza", ParticleTypes.ASH),
        new Type("Ceniza Blanca", ParticleTypes.WHITE_ASH),
        new Type("Esporas del Alma", ParticleTypes.WARPED_SPORE),
        new Type("Esporas Carmes\u00ed", ParticleTypes.CRIMSON_SPORE),
        new Type("Flor de Espora", ParticleTypes.SPORE_BLOSSOM_AIR),
        new Type("Cr\u00edtico", ParticleTypes.CRIT),
        new Type("Golpe M\u00e1gico", ParticleTypes.ENCHANTED_HIT),
        new Type("Humo de Fogata", ParticleTypes.CAMPFIRE_COSY_SMOKE),
        new Type("Humo Se\u00f1al", ParticleTypes.CAMPFIRE_SIGNAL_SMOKE),
        new Type("Estornudo", ParticleTypes.SNEEZE),
        new Type("Miel Goteante", ParticleTypes.DRIPPING_HONEY),
        new Type("Miel Cayendo", ParticleTypes.FALLING_HONEY),
        new Type("Burbujas", ParticleTypes.BUBBLE),
        new Type("Tinta Brillante", ParticleTypes.GLOW_SQUID_INK),
        new Type("Tinta de Calamar", ParticleTypes.SQUID_INK),
        new Type("Nube Poof", ParticleTypes.POOF),
        new Type("Delf\u00edn", ParticleTypes.DOLPHIN),
        new Type("Lava", ParticleTypes.LAVA),
        new Type("Salpicadura", ParticleTypes.SPLASH),
        new Type("Agua Goteante", ParticleTypes.DRIPPING_WATER),
        new Type("Cera", ParticleTypes.WAX_ON),
        new Type("Raspado", ParticleTypes.SCRAPE),
        new Type("Explosi\u00f3n", ParticleTypes.EXPLOSION),
        new Type("Destello Flash", ParticleTypes.FLASH),
        new Type("Polvo Rojo", 0xFF3030, 1.1f),
        new Type("Polvo Azul", 0x3080FF, 1.1f),
        new Type("Polvo Verde", 0x30FF60, 1.1f),
        new Type("Polvo Rosa", 0xFF66CC, 1.1f),
        new Type("Polvo Dorado", 0xFFD700, 1.1f),
        new Type("Polvo P\u00farpura", 0xB266FF, 1.1f),
        new Type("Polvo Cian", 0x30FFFF, 1.1f),
        new Type("Polvo Naranja", 0xFF8800, 1.1f),
        new Type("Polvo Turquesa", 0x00E5D0, 1.1f),
        new Type("Polvo Lima", 0xB6FF00, 1.1f),
        new Type("Polvo Magenta", 0xFF00AA, 1.1f),
        new Type("Polvo Blanco", 0xFFFFFF, 1.0f),
        new Type("Polvo Arco\u00edris", -2, 1.1f)
    };

    public static int count() {
        return TYPES.length;
    }

    public static String name(int i) {
        return TYPES[Math.floorMod(i, TYPES.length)].name;
    }

    public static int movementCount() {
        return MOVEMENT_NAMES.length;
    }

    public static String movementName(int i) {
        return MOVEMENT_NAMES[Math.floorMod(i, MOVEMENT_NAMES.length)];
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

    private static ParticleOptions resolve(Type ty, float sizeMult) {
        if (ty.dustRgb == -1) {
            return ty.particle;
        }
        int rgb = ty.dustRgb;
        if (ty.dustRgb == -2) {
            rgb = Color.HSBtoRGB((float) ((double) (System.currentTimeMillis() % 4000L) / 4000.0), 1.0f, 1.0f) & 0xFFFFFF;
        }
        return dust(rgb, ty.dustScale * sizeMult);
    }

    public static void spawn(ClientLevel level, double cx, double cy, double cz, double rightX, double rightZ, double halfW, double halfH, HoloLine line, RandomSource rnd) {
        Type ty = TYPES[Math.floorMod(line.particleStyle, TYPES.length)];
        int move = clamp(line.particleMovement, 0, MOVEMENT_NAMES.length - 1);
        float sizeMult = SIZE_MULT[clamp(line.particleSize, 0, SIZE_MULT.length - 1)];
        float spreadMult = SPREAD_MULT[clamp(line.particleSpread, 0, SPREAD_MULT.length - 1)];
        float speedMult = SPEED_MULT[clamp(line.particleSpeed, 0, SPEED_MULT.length - 1)];
        int count = clamp(line.particleDensity, 1, 4);
        int anchor = line.particleAnchor;
        double hw = Math.max(0.18, halfW) * (double) spreadMult;
        double vh = Math.max(0.12, halfH);
        double marginU = 0.12 + hw * 0.15;
        double marginV = 0.1 + vh * 0.2;
        double t = (double) (System.currentTimeMillis() % 6283L) / 1000.0;
        for (int k = 0; k < count; ++k) {
            ParticleOptions p = resolve(ty, sizeMult);
            if (p == null) {
                return;
            }
            double pu = 0.0;
            double pv = 0.0;
            double vy = 0.0;
            switch (move) {
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
                case WAVE: {
                    double fx = (double) rnd.nextFloat() * 2.0 - 1.0;
                    pu = fx * hw;
                    pv = Math.sin(t * 2.0 + fx * 3.14159) * (vh * 0.7);
                    break;
                }
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
