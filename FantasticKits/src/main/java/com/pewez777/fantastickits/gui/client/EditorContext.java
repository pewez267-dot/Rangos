/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.gui.client;

import java.util.ArrayList;
import java.util.List;

import com.pewez777.fantastickits.kits.Kit;

/**
 * Client-side editing state for the kit editor. Holds the LOCAL editable copy
 * of the kit plus the supporting data (groups, command catalogue) shipped by
 * the server. Saving sends this local copy back for server-side re-validation.
 */
public final class EditorContext {

    private final boolean editMode;
    private final boolean luckPermsAvailable;
    private final Kit kit;
    private final List<String> groups;
    private final List<String> commandCatalog;

    /** Index of the item currently being edited in the NBT tab (-1 = none). */
    private int selectedItemIndex = -1;

    public EditorContext(boolean editMode, boolean luckPermsAvailable, Kit kit,
                         List<String> groups, List<String> commandCatalog) {
        this.editMode = editMode;
        this.luckPermsAvailable = luckPermsAvailable;
        this.kit = kit == null ? new Kit() : kit;
        this.groups = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
        this.commandCatalog = commandCatalog == null ? new ArrayList<>() : new ArrayList<>(commandCatalog);
    }

    public boolean isEditMode() {
        return editMode;
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }

    public Kit getKit() {
        return kit;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getCommandCatalog() {
        return commandCatalog;
    }

    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    public void setSelectedItemIndex(int selectedItemIndex) {
        this.selectedItemIndex = selectedItemIndex;
    }
}
