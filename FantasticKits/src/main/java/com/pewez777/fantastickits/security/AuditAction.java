/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.security;

/**
 * The complete, fixed set of auditable actions. Every administrative or claim
 * operation maps to exactly one of these values.
 */
public enum AuditAction {
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
