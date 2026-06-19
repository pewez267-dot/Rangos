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
        cachedShape = null;
        dirty = true;
    }

    /**
     * Click izquierdo: en modos de 2 puntos marca/reemplaza P1; en poligono/freehand
     * agrega un vertice ordenado.
     *
     * @return false si se rechaza (limite de vertices alcanzado).
     */
    public boolean leftClick(BlockPos pos, int maxPolygonVertices, int maxFreehandPoints) {
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
            default:
                return null;
        }
    }
}
