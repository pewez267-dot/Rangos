package com.pewez.fantasticshortcuts.brigadier;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro global (thread-safe) de los literales raíz que deben OCULTARSE del árbol de comandos
 * enviado al cliente, porque un atajo con {@code replaceOriginal=true} los reemplaza.
 *
 * <p>Esta clase es el único punto de contacto entre la lógica de atajos y el Mixin
 * {@code CommandsMixin}: el Mixin consulta {@link #isHidden(String)} dentro de
 * {@code Commands.fillUsableCommands} para decidir si un literal aparece o no en el TAB del cliente.
 *
 * <p>Importante: ocultar del árbol NO afecta a la ejecución (el parseo del dispatcher no pasa por
 * {@code fillUsableCommands}), así que el comando real sigue funcionando con normalidad.
 */
public final class ReplacedCommands {

    private static final Set<String> HIDDEN = ConcurrentHashMap.newKeySet();

    private ReplacedCommands() {}

    /** Reemplaza por completo el conjunto de literales ocultos. */
    public static void set(Set<String> roots) {
        HIDDEN.clear();
        if (roots != null) {
            for (String r : roots) {
                if (r != null && !r.isBlank()) {
                    HIDDEN.add(r.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public static void clear() {
        HIDDEN.clear();
    }

    /** Consultado por el Mixin para cada literal del árbol de comandos. */
    public static boolean isHidden(String literal) {
        return literal != null && HIDDEN.contains(literal.toLowerCase(Locale.ROOT));
    }

    public static Set<String> view() {
        return Collections.unmodifiableSet(HIDDEN);
    }
}
