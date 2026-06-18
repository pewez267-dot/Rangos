package com.fantasticchest.gui;

import com.fantasticchest.FantasticChest;
import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.data.ChestDefinition;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.gui.admin.ChestAdminMenu;
import com.fantasticchest.gui.terminal.ChestTerminalMenu;
import com.fantasticchest.network.TerminalEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Menu type registration and the server-side helpers that open each GUI. */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FantasticChest.MOD_ID);

    public static final RegistryObject<MenuType<ChestTerminalMenu>> TERMINAL_MENU =
            MENUS.register("terminal", () -> IForgeMenuType.create(ChestTerminalMenu::new));

    public static final RegistryObject<MenuType<ChestAdminMenu>> ADMIN_MENU =
            MENUS.register("admin", () -> IForgeMenuType.create(ChestAdminMenu::new));

    private ModMenus() {
    }

    public static void register(final IEventBus modBus) {
        MENUS.register(modBus);
    }

    /** Opens the user terminal (Interface 2) with the first page of stock. */
    public static void openTerminal(final ServerPlayer player, final ChestBlockEntity chest) {
        final List<TerminalEntry> full = ChestTerminalMenu.buildFullList(chest);
        final int total = full.size();
        final List<TerminalEntry> page0 = ChestTerminalMenu.page(full, 0);
        final BlockPos pos = chest.getBlockPos();
        final String name = chest.getChestName();
        final String title = name == null || name.isBlank() ? "Fantastic Chest" : name;
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new ChestTerminalMenu(id, inv, pos, name, total, page0),
                        Component.literal(title)),
                buf -> ChestTerminalMenu.writeOpen(buf, pos, name, total, page0));
    }

    /** Opens the admin GUI (Interface 1) in creation mode. */
    public static void openAdminCreate(final ServerPlayer player) {
        final List<String> existing = existingIds();
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new ChestAdminMenu(id, inv, false, null, "", "", List.of(), existing),
                        Component.literal("Fantastic Chest - Administracion")),
                buf -> ChestAdminMenu.writeOpen(buf, false, null, "", "", List.of(), existing));
    }

    /** Opens the admin GUI (Interface 1) in edit mode for a placed chest. */
    public static void openAdminEdit(final ServerPlayer player, final ChestBlockEntity chest) {
        final List<String> permitted = chest.permittedAsStrings();
        final List<String> existing = existingIds();
        final BlockPos pos = chest.getBlockPos();
        final String chestId = chest.getChestId();
        final String chestName = chest.getChestName();
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new ChestAdminMenu(id, inv, true, pos, chestId, chestName, permitted, existing),
                        Component.literal("Fantastic Chest - Administracion")),
                buf -> ChestAdminMenu.writeOpen(buf, true, pos, chestId, chestName, permitted, existing));
    }

    private static List<String> existingIds() {
        final List<String> ids = new ArrayList<>();
        for (final ChestDefinition d : ChestRegistry.get().all()) {
            ids.add(d.id);
        }
        return ids;
    }
}
