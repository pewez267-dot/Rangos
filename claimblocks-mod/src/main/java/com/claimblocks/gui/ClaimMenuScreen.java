package com.claimblocks.gui;

/**
 * The visual side of the menu is the vanilla GenericContainerScreen. We do not
 * register a custom screen on the client because the
 * {@link ClaimMenuHandler} reuses {@code ScreenHandlerType.GENERIC_9X6}, which
 * is rendered by Minecraft as a 9×6 chest. This file exists for symmetry with
 * the requested module layout.
 */
public final class ClaimMenuScreen {
    private ClaimMenuScreen() {}
}
