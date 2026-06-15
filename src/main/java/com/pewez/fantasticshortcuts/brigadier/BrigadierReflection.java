package com.pewez.fantasticshortcuts.brigadier;

import com.pewez.fantasticshortcuts.FantasticShortcuts;
import com.mojang.brigadier.tree.CommandNode;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Utilidades de reflexión sobre el árbol de Brigadier.
 *
 * <p>Brigadier no expone un método público para ELIMINAR nodos de un {@code RootCommandNode}. Para
 * permitir la sincronización en vivo (crear/editar/eliminar atajos sin reiniciar), accedemos a los
 * mapas internos {@code children}, {@code literals} y {@code arguments} del nodo y quitamos por
 * nombre solo los nodos que nosotros añadimos. No se toca ningún otro comando.
 */
public final class BrigadierReflection {

    private static Field CHILDREN;
    private static Field LITERALS;
    private static Field ARGUMENTS;

    private BrigadierReflection() {}

    private static synchronized void ensureFields() {
        if (CHILDREN != null) {
            return;
        }
        try {
            CHILDREN = CommandNode.class.getDeclaredField("children");
            LITERALS = CommandNode.class.getDeclaredField("literals");
            ARGUMENTS = CommandNode.class.getDeclaredField("arguments");
            CHILDREN.setAccessible(true);
            LITERALS.setAccessible(true);
            ARGUMENTS.setAccessible(true);
        } catch (NoSuchFieldException e) {
            FantasticShortcuts.LOGGER.error("[F-Shortcuts] No se pudieron mapear los campos internos de Brigadier: {}", e.toString());
        }
    }

    /**
     * Elimina un hijo del nodo raíz por su nombre de literal. Seguro: si el campo no existe o el
     * nodo no está, no hace nada.
     */
    @SuppressWarnings("unchecked")
    public static void removeChild(CommandNode<?> root, String name) {
        if (root == null || name == null) {
            return;
        }
        ensureFields();
        if (CHILDREN == null) {
            return;
        }
        try {
            ((Map<String, ?>) CHILDREN.get(root)).remove(name);
            ((Map<String, ?>) LITERALS.get(root)).remove(name);
            ((Map<String, ?>) ARGUMENTS.get(root)).remove(name);
        } catch (IllegalAccessException e) {
            FantasticShortcuts.LOGGER.warn("[F-Shortcuts] No se pudo eliminar el nodo '{}': {}", name, e.toString());
        }
    }
}
