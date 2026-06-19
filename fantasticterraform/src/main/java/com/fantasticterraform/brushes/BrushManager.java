package com.fantasticterraform.brushes;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.ListWriteTask;
import com.fantasticterraform.editing.Placement;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra los brushes disponibles, guarda la configuracion de cada jugador y aplica
 * un brush validando que TODO su volumen quede contenido en la seleccion activa
 * (regla transversal). Si el brush se sale total o parcialmente, se rechaza.
 */
public final class BrushManager {

    private static final Map<String, Brush> BRUSHES = new LinkedHashMap<>();
    private static final Map<UUID, BrushSettings> SETTINGS = new ConcurrentHashMap<>();

    static {
        register(new SphereBrush());
        register(new CylinderBrush());
        register(new SmoothBrush());
        register(new ErodeBrush());
        register(new OverlayBrush());
        register(new SphereClearBrush());
        register(new NoisePaintBrush());
        register(new BlendBrush());
        register(new FlattenBrush());
        register(new MeltBrush());
    }

    private BrushManager() {
    }

    private static void register(Brush brush) {
        BRUSHES.put(brush.id(), brush);
    }

    public static Map<String, Brush> brushes() {
        return BRUSHES;
    }

    public static BrushSettings settings(UUID id) {
        return SETTINGS.computeIfAbsent(id, k -> new BrushSettings());
    }

    public static void setSettings(UUID id, BrushSettings settings) {
        SETTINGS.put(id, settings);
    }

    public static void remove(UUID id) {
        SETTINGS.remove(id);
    }

    /** Aplica el brush activo del jugador en {@code center}. */
    public static void apply(ServerPlayer player, ServerLevel level, BlockPos center) {
        SelectionShape sel = SelectionManager.get(player).getShape();
        if (sel == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7cNecesitas una seleccion activa valida antes de usar un brush."));
            return;
        }

        BrushSettings s = settings(player.getUUID());
        int maxRadius = TerraformConfig.GENERAL.maxBrushRadius.get();
        s.radius = Math.max(1, Math.min(maxRadius, s.radius));

        Brush brush = BRUSHES.get(s.brushId);
        if (brush == null) {
            player.sendSystemMessage(Component.literal("\u00a7cBrush desconocido: " + s.brushId));
            return;
        }

        List<Placement> placements = brush.computePlacements(level, center, s);
        if (placements.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7eEl brush no afecta ningun bloque aqui."));
            return;
        }

        // Contencion estricta: cualquier bloque fuera de la seleccion rechaza la operacion.
        for (Placement p : placements) {
            if (!sel.contains(p.pos)) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cEl brush se sale de la seleccion activa. Operacion rechazada."));
                return;
            }
        }

        Mask mask = MaskManager.combinedFor(player);
        BlockChangeQueue.enqueue(new ListWriteTask(level, player.getUUID(),
                "Brush " + brush.displayName(), mask, placements, true));
    }
}
