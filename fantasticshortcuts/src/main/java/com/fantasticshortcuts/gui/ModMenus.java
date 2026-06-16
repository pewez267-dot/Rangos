package com.fantasticshortcuts.gui;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.network.ShortcutCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers the two {@link MenuType}s and provides the server-side helpers that open them
 * (via {@link NetworkHooks#openScreen}, which both opens the container and ships the
 * initial data to the client).
 */
public final class ModMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FantasticShortcuts.MOD_ID);

    public static final RegistryObject<MenuType<ShortcutsMenu>> SHORTCUTS =
            MENUS.register("shortcuts", () -> IForgeMenuType.create(ShortcutsMenu::new));

    public static final RegistryObject<MenuType<ShortcutEditorMenu>> EDITOR =
            MENUS.register("shortcut_editor", () -> IForgeMenuType.create(ShortcutEditorMenu::new));

    private ModMenus() {
    }

    public static void register(final IEventBus modBus) {
        MENUS.register(modBus);
    }

    /** Opens the main management screen, shipping the current shortcut list to the client. */
    public static void openMain(final ServerPlayer player) {
        final List<Shortcut> all = ShortcutManager.get().all();
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new ShortcutsMenu(id, inv, all),
                        Component.literal("Fantastic Shortcuts")),
                buf -> ShortcutCodec.writeList(buf, all));
    }

    /** Opens the editor for the given shortcut (a fresh instance means "create"). */
    public static void openEditor(final ServerPlayer player, final Shortcut shortcut) {
        final Shortcut copy = shortcut.copy();
        final List<Shortcut> others = new ArrayList<>();
        for (final Shortcut s : ShortcutManager.get().all()) {
            if (!s.getId().equals(copy.getId())) {
                others.add(s);
            }
        }
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new ShortcutEditorMenu(id, inv, copy, others),
                        Component.literal("Editor de Shortcut")),
                buf -> {
                    ShortcutCodec.write(buf, copy);
                    ShortcutCodec.writeList(buf, others);
                });
    }
}
