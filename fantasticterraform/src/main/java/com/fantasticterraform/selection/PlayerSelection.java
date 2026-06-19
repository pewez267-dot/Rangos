package com.fantasticterraform.selection;

import com.fantasticterraform.selection.shapes.ConvexHullSelection;
import com.fantasticterraform.selection.shapes.CuboidSelection;
import com.fantasticterraform.selection.shapes.CylinderSelection;
import com.fantasticterraform.selection.shapes.EllipsoidSelection;
import com.fantasticterraform.selection.shapes.PolygonSelection;
import com.fantasticterraform.selection.shapes.SphereSelection;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado de seleccion persistente de un jugador. Vive en memoria del servidor
 * asociado al UUID y NO se borra al alejarse: solo se limpia explicitamente o al
 * salir del modo editor.
 *
 * <p>Cachea la {@link SelectionShape} calculada y solo la reconstruye cuando los
 * puntos o el modo cambian (regla de rendimiento: no recalcular geometrias
 * complejas innecesariamente).</p>
 */
public final class PlayerSelection {

    private SelectionType type = SelectionType.CUBOID;
    private final List<BlockPos> points = new ArrayList<>();
    private int cylinderHeight = 5;
    private boolean closed;

    /** Geometria explicita (flood-fill SMART); cuando esta presente y el tipo es SMART, manda. */
    private SelectionShape explicitShape;

    private SelectionShape cachedShape;
    private boolean dirty = true;

    public SelectionType getType() {
        return type;
    }

    /** Cambiar de modo limpia los puntos previos (el dialogo de confirmacion vive en el HUD). */
    public void setType(SelectionType newType) {
        if (newType != this.type) {
            this.type = newType;
            clear();
        }
    }

    public List<BlockPos> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public int getCylinderHeight() {
        return cylinderHeight;
    }

    public void setCylinderHeight(int height) {
        int clamped = Math.max(1, Math.min(384, height));
        if (clamped != this.cylinderHeight) {
            this.cylinderHeight = clamped;
            this.dirty = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void clear() {
        points.clear();
        closed = false;
        explicitShape = null;
        cachedShape = null;
        dirty = true;
    }

    /** Fija una geometria explicita (resultado de un flood-fill SMART). */
    public void setExplicitShape(SelectionShape shape) {
        this.explicitShape = shape;
        this.dirty = true;
    }

    /**
     * Click izquierdo: en modos de 2 puntos marca/reemplaza P1; en poligono/freehand
     * agrega un vertice ordenado.
     *
     * @return false si se rechaza (limite de vertices alcanzado).
     */
    public boolean leftClick(BlockPos pos, int maxPolygonVertices, int maxFreehandPoints) {
        if (type == SelectionType.SMART) {
            return false; // SMART se define con flood-fill (SmartSelectPacket), no con puntos.
        }
        dirty = true;
        if (type.isMultiPoint()) {
            if (closed) {
                // Empezar una seleccion nueva tras haber cerrado la anterior.
                points.clear();
                closed = false;
            }
            int cap = type == SelectionType.POLYGON ? maxPolygonVertices : maxFreehandPoints;
            if (points.size() >= cap) {
                return false;
            }
            points.add(pos);
            return true;
        }
        if (points.isEmpty()) {
            points.add(pos);
        } else {
            points.set(0, pos);
        }
        closed = false;
        return true;
    }

    /**
     * Click derecho: en modos de 2 puntos marca/reemplaza P2; en poligono/freehand
     * cierra la seleccion si hay vertices suficientes.
     *
     * @return false si la accion no es valida en el estado actual.
     */
    public boolean rightClick(BlockPos pos) {
        if (type == SelectionType.SMART) {
            return false;
        }
        dirty = true;
        if (type.isMultiPoint()) {
            if (points.size() >= type.minPoints()) {
                closed = true;
                return true;
            }
            return false;
        }
        if (points.isEmpty()) {
            // Sin P1 todavia: el click derecho marca P1 para no perder la interaccion.
            points.add(pos);
            return true;
        }
        if (points.size() < 2) {
            points.add(pos);
        } else {
            points.set(1, pos);
        }
        return true;
    }

    /** Indica si hay una geometria valida y completa segun el modo activo. */
    public boolean isComplete() {
        return getShape() != null;
    }

    /**
     * Devuelve la geometria cacheada o la reconstruye si los puntos cambiaron.
     * Devuelve {@code null} si la seleccion aun no es valida.
     */
    public SelectionShape getShape() {
        if (!dirty) {
            return cachedShape;
        }
        dirty = false;
        cachedShape = computeShape();
        return cachedShape;
    }

    private SelectionShape computeShape() {
        switch (type) {
            case CUBOID:
                return points.size() >= 2 ? new CuboidSelection(points.get(0), points.get(1)) : null;
            case SPHERE:
                return points.size() >= 2 ? new SphereSelection(points.get(0), points.get(1)) : null;
            case CYLINDER:
                return points.size() >= 2 ? new CylinderSelection(points.get(0), points.get(1), cylinderHeight) : null;
            case ELLIPSOID:
                return points.size() >= 2 ? new EllipsoidSelection(points.get(0), points.get(1)) : null;
            case POLYGON:
                return (closed && points.size() >= 3) ? new PolygonSelection(new ArrayList<>(points)) : null;
            case CONVEX_HULL:
                if (closed && points.size() >= 4) {
                    ConvexHullSelection hull = new ConvexHullSelection(new ArrayList<>(points));
                    return hull.isValid() ? hull : null;
                }
                return null;
            case SMART:
                return explicitShape;
            default:
                return null;
        }
    }

    // ----- transformaciones de region (//shift //expand //contract //outset) -----

    /** Traslada toda la seleccion por (dx,dy,dz). Funciona en cualquier tipo. */
    public void shift(int dx, int dy, int dz) {
        if (type == SelectionType.SMART) {
            if (explicitShape instanceof com.fantasticterraform.selection.shapes.SetSelection) {
                explicitShape = ((com.fantasticterraform.selection.shapes.SetSelection) explicitShape).translate(dx, dy, dz);
            }
        } else {
            for (int i = 0; i < points.size(); i++) {
                points.set(i, points.get(i).offset(dx, dy, dz));
            }
        }
        dirty = true;
    }

    /** Expande (n&gt;0) o contrae (n&lt;0) la region. {@code horizontalOnly} = solo ejes X/Z (outset). */
    public void resize(int n, boolean horizontalOnly) {
        if (n == 0 || points.size() < 2) {
            if (!type.isMultiPoint() || points.size() < 2) {
                if (type != SelectionType.POLYGON && type != SelectionType.CONVEX_HULL) {
                    return;
                }
            }
        }
        dirty = true;
        switch (type) {
            case CUBOID:
            case ELLIPSOID: {
                if (points.size() < 2) {
                    return;
                }
                BlockPos a = points.get(0);
                BlockPos b = points.get(1);
                int minX = Math.min(a.getX(), b.getX()) - n;
                int maxX = Math.max(a.getX(), b.getX()) + n;
                int minZ = Math.min(a.getZ(), b.getZ()) - n;
                int maxZ = Math.max(a.getZ(), b.getZ()) + n;
                int minY = Math.min(a.getY(), b.getY()) - (horizontalOnly ? 0 : n);
                int maxY = Math.max(a.getY(), b.getY()) + (horizontalOnly ? 0 : n);
                if (minX > maxX) {
                    int m = (minX + maxX) / 2;
                    minX = maxX = m;
                }
                if (minY > maxY) {
                    int m = (minY + maxY) / 2;
                    minY = maxY = m;
                }
                if (minZ > maxZ) {
                    int m = (minZ + maxZ) / 2;
                    minZ = maxZ = m;
                }
                if (type == SelectionType.CUBOID) {
                    points.set(0, new BlockPos(minX, minY, minZ));
                    points.set(1, new BlockPos(maxX, maxY, maxZ));
                } else {
                    // Elipsoide: P0 = centro, P1 = esquina (radios). Mantener centro, crecer radios.
                    int cx = (minX + maxX) / 2;
                    int cy = (minY + maxY) / 2;
                    int cz = (minZ + maxZ) / 2;
                    points.set(0, new BlockPos(cx, cy, cz));
                    points.set(1, new BlockPos(maxX, maxY, maxZ));
                }
                break;
            }
            case SPHERE: {
                growRadialPoint(n, false);
                break;
            }
            case CYLINDER: {
                growRadialPoint(n, true);
                break;
            }
            case POLYGON:
            case CONVEX_HULL: {
                scaleAroundCentroid(n, horizontalOnly || type == SelectionType.POLYGON);
                break;
            }
            default:
                break;
        }
    }

    /** Crece/encoge el radio de esfera/cilindro moviendo P1 respecto a P0. */
    private void growRadialPoint(int n, boolean xzOnly) {
        if (points.size() < 2) {
            return;
        }
        BlockPos c = points.get(0);
        BlockPos s = points.get(1);
        double dx = s.getX() - c.getX();
        double dy = xzOnly ? 0 : s.getY() - c.getY();
        double dz = s.getZ() - c.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double newLen = Math.max(1.0D, len + n);
        if (len < 1.0E-6D) {
            points.set(1, new BlockPos(c.getX() + (int) newLen, c.getY(), c.getZ()));
            return;
        }
        double k = newLen / len;
        int nx = c.getX() + (int) Math.round(dx * k);
        int ny = xzOnly ? s.getY() : c.getY() + (int) Math.round(dy * k);
        int nz = c.getZ() + (int) Math.round(dz * k);
        points.set(1, new BlockPos(nx, ny, nz));
    }

    /** Escala los vertices de poligono/hull alejandolos del centroide por n bloques. */
    private void scaleAroundCentroid(int n, boolean xzOnly) {
        if (points.isEmpty()) {
            return;
        }
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (BlockPos p : points) {
            cx += p.getX();
            cy += p.getY();
            cz += p.getZ();
        }
        cx /= points.size();
        cy /= points.size();
        cz /= points.size();
        for (int i = 0; i < points.size(); i++) {
            BlockPos p = points.get(i);
            double dx = p.getX() - cx;
            double dy = xzOnly ? 0 : p.getY() - cy;
            double dz = p.getZ() - cz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-6D) {
                continue;
            }
            double k = (len + n) / len;
            int nx = (int) Math.round(cx + dx * k);
            int ny = xzOnly ? p.getY() : (int) Math.round(cy + dy * k);
            int nz = (int) Math.round(cz + dz * k);
            points.set(i, new BlockPos(nx, ny, nz));
        }
    }
}
