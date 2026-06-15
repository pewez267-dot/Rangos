package com.fantasticwatch.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates globally-unique item identifiers for tracked items.
 *
 * <p>Format: {@code fw-{opUuidNoDashes}-{epochMillis}-{8 hex random}}, e.g.
 * {@code fw-a3f9c21b4e8d7f60...-1705312327441-3a9f12bc}. The combination of the operator UUID,
 * a millisecond timestamp and 32 bits of cryptographically-strong randomness makes collisions
 * effectively impossible even across many operators spawning items in the same millisecond.</p>
 */
public final class UidGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UidGenerator() {
    }

    /**
     * @param opUuid the UUID of the operator spawning the item
     * @return a new unique item id
     */
    public static String generate(UUID opUuid) {
        String noDashes = opUuid.toString().replace("-", "");
        long epochMillis = System.currentTimeMillis();
        int randomBits = RANDOM.nextInt();
        String hex8 = String.format("%08x", randomBits);
        return "fw-" + noDashes + "-" + epochMillis + "-" + hex8;
    }
}
