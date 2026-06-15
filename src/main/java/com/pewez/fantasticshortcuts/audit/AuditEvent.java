package com.pewez.fantasticshortcuts.audit;

/**
 * The categories of events recorded by the audit log.
 */
public enum AuditEvent {
    CREATE_SHORTCUT,
    EDIT_SHORTCUT,
    DELETE_SHORTCUT,
    EXECUTE_SHORTCUT,
    CONFLICT,
    INVALID_ACCESS,
    PERMISSION_DENIED,
    INVALID_SHORTCUT,
    INJECTION_ATTEMPT,
    FORGED_COMMAND
}
