package com.pewez.fantasticshortcuts.audit;

/**
 * Tipos de evento registrados por el sistema de auditoría.
 *
 * <p>Cubre tanto la gestión (CRUD) como la seguridad avanzada: ejecuciones, conflictos, accesos
 * inválidos, comandos forjados, atajos inválidos, intentos de permiso denegado e intentos de
 * inyección.
 */
public enum AuditEvent {
    CREATE_SHORTCUT,
    EDIT_SHORTCUT,
    DELETE_SHORTCUT,
    EXECUTE_SHORTCUT,
    CONFLICT,
    INVALID_ACCESS,
    FORGED_COMMAND,
    INVALID_SHORTCUT,
    PERMISSION_DENIED,
    INJECTION_ATTEMPT
}
