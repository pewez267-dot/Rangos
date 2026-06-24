package com.fantastic.kits.audit;

/**
 * Closed enumeration of every kit-domain action that must be permanently
 * recorded in the audit log. The list is intentionally fixed so reviewers can
 * audit the ledger with a known schema.
 */
public enum AuditEventType {
    CREATE_KIT,
    EDIT_KIT,
    DELETE_KIT,
    TEST_KIT,
    CLAIM_KIT,
    CLAIM_DENIED,
    CHANGE_GROUP,
    CHANGE_COMMANDS,
    CHANGE_NBT,
    RESTORE_KIT
}
