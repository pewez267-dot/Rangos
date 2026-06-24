package com.fantasticterraform.terrain;

import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

/**
 * Erosion HIDRAULICA por simulacion de gotas de lluvia sobre el heightmap. Cada gota
 * cae en un punto aleatorio y desciende por la pendiente arrastrando sedimento: erosiona
 * donde la corriente es fuerte (laderas) y deposita en zonas llanas, formando valles,
 * carcavas y crestas naturales. Es muy superior a la erosion termica para un acabado
 * realista. Toda la simulacion ocurre sobre un array de alturas (operacion pura) y solo
 * al final se escribe el mundo una vez.
 *
 * <p>Algoritmo de transporte de sedimento con interpolacion bilineal del gradiente
 * (modelo clasico de "droplet erosion").</p>
 */
public final class HydraulicErosionOperation {

    // Parametros del modelo (equilibrados para terreno de Minecraft).
    private static final int MAX_STEPS = 48;
    private static final double INERTIA = 0.05D;
    private static final double SEDIMENT_CAPACITY = 4.0D;
    private static final double MIN_SLOPE = 0.01D;
    private static final double ERODE_SPEED = 0.3D;
    private static final double DEPOSIT_SPEED = 0.3D;
    private static final double EVAPORATION = 0.02D;
    private static final double GRAVITY = 4.0D;
    private static final int DROPLET_CAP = 400_000;

    private HydraulicErosionOperation() {
    }

    public static void apply(ServerPlayer player, ServerLevel level, SelectionShape sel,
                             int droplets, double strength, long seed, Mask mask) {
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        TerrainUtil.Heightmap hm = TerrainUtil.buildHeightmap(level, sel);
        int w = hm.width;
        int d = hm.depth;
        if (w < 4 || d < 4) {
            return;
        }

        double[] map = new double[w * d];
        boolean[] valid = new boolean[w * d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                map[x * d + z] = hm.height[x][z];
                valid[x * d + z] = hm.hasColumn(x, z);
            }
        }

        int n = Math.min(DROPLET_CAP, Math.max(1, droplets));
        double str = Math.max(0.05D, Math.min(2.0D, strength));
        double erodeSpeed = ERODE_SPEED * str;
        double depositSpeed = DEPOSIT_SPEED * str;
        Random rng = new Random(seed);

        for (int i = 0; i < n; i++) {
            double posX = rng.nextDouble() * (w - 1);
            double posZ = rng.nextDouble() * (d - 1);
            double dirX = 0.0D;
            double dirZ = 0.0D;
            double speed = 1.0D;
            double water = 1.0D;
            double sediment = 0.0D;

            for (int step = 0; step < MAX_STEPS; step++) {
                int nodeX = (int) posX;
                int nodeZ = (int) posZ;
                if (nodeX < 0 || nodeZ < 0 || nodeX >= w - 1 || nodeZ >= d - 1) {
                    break;
                }
                if (!valid[nodeX * d + nodeZ]) {
                    break;
                }
                double offX = posX - nodeX;
                double offZ = posZ - nodeZ;

                double[] hg = heightAndGradient(map, valid, w, d, nodeX, nodeZ, offX, offZ);
                double height = hg[0];
                double gradX = hg[1];
                double gradZ = hg[2];

                dirX = dirX * INERTIA - gradX * (1.0D - INERTIA);
                dirZ = dirZ * INERTIA - gradZ * (1.0D - INERTIA);
                double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (len < 1.0E-6D) {
                    // Sin pendiente clara: empuje aleatorio para no estancarse.
                    double ang = rng.nextDouble() * Math.PI * 2.0D;
                    dirX = Math.cos(ang);
                    dirZ = Math.sin(ang);
                } else {
                    dirX /= len;
                    dirZ /= len;
                }

                double newPosX = posX + dirX;
                double newPosZ = posZ + dirZ;
                int nx = (int) newPosX;
                int nz = (int) newPosZ;
                if (nx < 0 || nz < 0 || nx >= w - 1 || nz >= d - 1 || !valid[nx * d + nz]) {
                    break;
                }

                double newHeight = heightAndGradient(map, valid, w, d, nx, nz, newPosX - nx, newPosZ - nz)[0];
                double deltaH = newHeight - height;

                double capacity = Math.max(-deltaH, MIN_SLOPE) * speed * water * SEDIMENT_CAPACITY;

                if (sediment > capacity || deltaH > 0.0D) {
                    // Depositar: en subida deposita lo justo para rellenar el hueco.
                    double deposit = (deltaH > 0.0D)
                            ? Math.min(deltaH, sediment)
                            : (sediment - capacity) * depositSpeed;
                    sediment -= deposit;
                    deposit(map, w, d, nodeX, nodeZ, offX, offZ, deposit);
                } else {
                    // Erosionar (limitado por la pendiente para no abrir pozos).
                    double erode = Math.min((capacity - sediment) * erodeSpeed, -deltaH);
                    erode = Math.max(0.0D, erode);
                    erodeAround(map, valid, w, d, nodeX, nodeZ, erode);
                    sediment += erode;
                }

                speed = Math.sqrt(Math.max(0.0D, speed * speed + deltaH * -GRAVITY));
                water *= (1.0D - EVAPORATION);
                posX = newPosX;
                posZ = newPosZ;
                if (water < 0.01D) {
                    break;
                }
            }
        }

        int[][] target = new int[w][d];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                target[x][z] = (int) Math.round(map[x * d + z]);
            }
        }
        TerrainUtil.applyHeightmap(player, level, sel, "Erosion hidraulica", target, hm, mask);
    }

    /** Altura interpolada bilinealmente y gradiente (dX,dZ) en la celda (nodeX,nodeZ)+offset. */
    private static double[] heightAndGradient(double[] map, boolean[] valid, int w, int d,
                                              int nodeX, int nodeZ, double offX, double offZ) {
        int i = nodeX * d + nodeZ;
        double hNW = map[i];
        double hNE = map[i + d];
        double hSW = map[i + 1];
        double hSE = map[i + d + 1];

        double gradX = (hNE - hNW) * (1.0D - offZ) + (hSE - hSW) * offZ;
        double gradZ = (hSW - hNW) * (1.0D - offX) + (hSE - hNE) * offX;
        double height = hNW * (1 - offX) * (1 - offZ) + hNE * offX * (1 - offZ)
                + hSW * (1 - offX) * offZ + hSE * offX * offZ;
        return new double[] {height, gradX, gradZ};
    }

    private static void deposit(double[] map, int w, int d, int nodeX, int nodeZ, double offX, double offZ, double amount) {
        int i = nodeX * d + nodeZ;
        map[i] += amount * (1 - offX) * (1 - offZ);
        map[i + d] += amount * offX * (1 - offZ);
        map[i + 1] += amount * (1 - offX) * offZ;
        map[i + d + 1] += amount * offX * offZ;
    }

    /** Erosiona el sedimento repartido en un pequeno radio (3x3) para suavizar surcos. */
    private static void erodeAround(double[] map, boolean[] valid, int w, int d, int cx, int cz, double amount) {
        double total = 0.0D;
        double[] weights = new double[9];
        int k = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                double weight = (x >= 0 && z >= 0 && x < w && z < d && valid[x * d + z])
                        ? 1.0D / (1.0D + dx * dx + dz * dz) : 0.0D;
                weights[k++] = weight;
                total += weight;
            }
        }
        if (total <= 0.0D) {
            return;
        }
        k = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                double weight = weights[k++];
                if (weight > 0.0D) {
                    map[x * d + z] -= amount * (weight / total);
                }
            }
        }
    }
}
