/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Server-safe helper for the advanced item editor: add, remove and reorder kit
 * contents. Supports any item type (armor, tools, weapons, consumables,
 * blocks) because it operates purely on {@link ItemStack} instances.
 */
public final class ItemEditorService {

    /** Hard cap on the number of items a single kit may contain. */
    public static final int MAX_ITEMS = 54;

    private ItemEditorService() {
    }

    public static boolean addItem(List<ItemStack> items, ItemStack stack) {
        if (items == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (items.size() >= MAX_ITEMS) {
            return false;
        }
        items.add(stack.copy());
        return true;
    }

    public static boolean removeItem(List<ItemStack> items, int index) {
        if (items == null || index < 0 || index >= items.size()) {
            return false;
        }
        items.remove(index);
        return true;
    }

    public static boolean moveUp(List<ItemStack> items, int index) {
        if (items == null || index <= 0 || index >= items.size()) {
            return false;
        }
        swap(items, index, index - 1);
        return true;
    }

    public static boolean moveDown(List<ItemStack> items, int index) {
        if (items == null || index < 0 || index >= items.size() - 1) {
            return false;
        }
        swap(items, index, index + 1);
        return true;
    }

    public static void replace(List<ItemStack> items, int index, ItemStack stack) {
        if (items == null || index < 0 || index >= items.size() || stack == null) {
            return;
        }
        items.set(index, stack.copy());
    }

    private static void swap(List<ItemStack> items, int a, int b) {
        ItemStack tmp = items.get(a);
        items.set(a, items.get(b));
        items.set(b, tmp);
    }

    /** Returns a defensive copy of the list with empty stacks removed. */
    public static List<ItemStack> sanitized(List<ItemStack> items) {
        List<ItemStack> out = new ArrayList<>();
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack != null && !stack.isEmpty()) {
                    out.add(stack.copy());
                }
            }
        }
        return out;
    }
}
