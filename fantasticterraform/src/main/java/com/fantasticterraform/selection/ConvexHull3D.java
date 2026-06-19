package com.fantasticterraform.selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Envolvente convexo 3D real, construido con un algoritmo incremental (estilo
 * "incremental hull"). Produce caras triangulares con normales orientadas hacia
 * afuera. El test {@link #contains(double, double, double)} comprueba que el punto
 * este del lado interior de TODAS las caras (interseccion de semiespacios), que es
 * la definicion exacta de pertenencia al poliedro convexo.
 *
 * <p>Se usa tanto del lado servidor (geometria de la seleccion) como del lado
 * cliente (wireframe de las caras), por lo que no depende de ninguna clase de
 * Minecraft.</p>
 */
public final class ConvexHull3D {

    private static final double EPS = 1.0E-7D;

    private final double[][] pts;
    private final List<int[]> faces = new ArrayList<>();
    private final List<double[]> normals = new ArrayList<>();
    private final double[] interior = new double[3];
    private final boolean valid;

    public ConvexHull3D(double[][] points) {
        this.pts = points;
        this.valid = build();
    }

    public boolean isValid() {
        return valid;
    }

    /** Caras triangulares como ternas de indices en el array de puntos original. */
    public List<int[]> faces() {
        return faces;
    }

    public double[][] points() {
        return pts;
    }

    private boolean build() {
        int n = pts.length;
        if (n < 4) {
            return false;
        }

        // 1) p0 = primer punto; p1 = el mas lejano de p0.
        int p0 = 0;
        int p1 = -1;
        double best = -1;
        for (int i = 1; i < n; i++) {
            double d = distSq(pts[p0], pts[i]);
            if (d > best) {
                best = d;
                p1 = i;
            }
        }
        if (p1 < 0 || best < EPS) {
            return false;
        }

        // 2) p2 = el mas lejano de la recta p0-p1.
        int p2 = -1;
        best = -1;
        for (int i = 0; i < n; i++) {
            if (i == p0 || i == p1) {
                continue;
            }
            double d = pointLineDistSq(pts[i], pts[p0], pts[p1]);
            if (d > best) {
                best = d;
                p2 = i;
            }
        }
        if (p2 < 0 || best < EPS) {
            return false;
        }

        // 3) p3 = el mas lejano del plano p0-p1-p2.
        double[] planeN = cross(sub(pts[p1], pts[p0]), sub(pts[p2], pts[p0]));
        int p3 = -1;
        best = -1;
        for (int i = 0; i < n; i++) {
            if (i == p0 || i == p1 || i == p2) {
                continue;
            }
            double d = Math.abs(dot(planeN, sub(pts[i], pts[p0])));
            if (d > best) {
                best = d;
                p3 = i;
            }
        }
        if (p3 < 0 || best < EPS) {
            // Todos los puntos son coplanares: no hay volumen 3D real.
            return false;
        }

        // Centroide del tetraedro inicial: punto interior garantizado del hull final.
        for (int k = 0; k < 3; k++) {
            interior[k] = (pts[p0][k] + pts[p1][k] + pts[p2][k] + pts[p3][k]) / 4.0D;
        }

        addOrientedFace(p0, p1, p2);
        addOrientedFace(p0, p1, p3);
        addOrientedFace(p0, p2, p3);
        addOrientedFace(p1, p2, p3);

        boolean[] used = new boolean[n];
        used[p0] = used[p1] = used[p2] = used[p3] = true;

        for (int i = 0; i < n; i++) {
            if (used[i]) {
                continue;
            }
            addPoint(i);
        }
        return faces.size() >= 4;
    }

    private void addPoint(int idx) {
        double[] p = pts[idx];
        // Caras visibles desde p (p esta fuera de ellas).
        List<Integer> visible = new ArrayList<>();
        for (int f = 0; f < faces.size(); f++) {
            if (signedDistance(f, p) > EPS) {
                visible.add(f);
            }
        }
        if (visible.isEmpty()) {
            return; // p esta dentro del hull actual.
        }

        // Aristas dirigidas de las caras visibles.
        Set<Long> directed = new HashSet<>();
        for (int f : visible) {
            int[] face = faces.get(f);
            directed.add(edgeKey(face[0], face[1]));
            directed.add(edgeKey(face[1], face[2]));
            directed.add(edgeKey(face[2], face[0]));
        }

        // Aristas del horizonte: aquellas cuya arista opuesta no esta en el conjunto visible.
        List<int[]> horizon = new ArrayList<>();
        for (int f : visible) {
            int[] face = faces.get(f);
            checkHorizon(directed, horizon, face[0], face[1]);
            checkHorizon(directed, horizon, face[1], face[2]);
            checkHorizon(directed, horizon, face[2], face[0]);
        }

        // Eliminar caras visibles (de mayor a menor indice para no invalidar).
        visible.sort((a, b) -> Integer.compare(b, a));
        for (int f : visible) {
            faces.remove(f);
            normals.remove(f);
        }

        // Crear nuevas caras conectando cada arista del horizonte con p.
        for (int[] e : horizon) {
            addOrientedFace(e[0], e[1], idx);
        }
    }

    private void checkHorizon(Set<Long> directed, List<int[]> horizon, int a, int b) {
        if (!directed.contains(edgeKey(b, a))) {
            horizon.add(new int[] {a, b});
        }
    }

    private void addOrientedFace(int a, int b, int c) {
        double[] n = cross(sub(pts[b], pts[a]), sub(pts[c], pts[a]));
        double len = Math.sqrt(dot(n, n));
        if (len < 1.0E-12D) {
            return; // triangulo degenerado.
        }
        n = new double[] {n[0] / len, n[1] / len, n[2] / len};
        // Orientar hacia afuera: el interior debe quedar del lado negativo.
        double side = dot(n, sub(interior, pts[a]));
        if (side > 0.0D) {
            // invertir
            int tmp = b;
            b = c;
            c = tmp;
            n = new double[] {-n[0], -n[1], -n[2]};
        }
        faces.add(new int[] {a, b, c});
        normals.add(n);
    }

    private double signedDistance(int face, double[] p) {
        double[] n = normals.get(face);
        int[] f = faces.get(face);
        return dot(n, sub(p, pts[f[0]]));
    }

    /** {@code true} si el punto esta dentro o sobre todas las caras del hull. */
    public boolean contains(double x, double y, double z) {
        if (!valid) {
            return false;
        }
        double[] p = {x, y, z};
        for (int f = 0; f < faces.size(); f++) {
            if (signedDistance(f, p) > EPS) {
                return false;
            }
        }
        return true;
    }

    // ----- helpers vectoriales -----

    private static long edgeKey(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double distSq(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }

    private static double pointLineDistSq(double[] p, double[] a, double[] b) {
        double[] ab = sub(b, a);
        double[] ap = sub(p, a);
        double[] cr = cross(ab, ap);
        double denom = dot(ab, ab);
        if (denom < 1.0E-12D) {
            return distSq(p, a);
        }
        return dot(cr, cr) / denom;
    }
}
