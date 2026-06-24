package com.fantastickits.gui;

import com.fantastickits.FantasticKits;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers all MenuType instances for the mod's GUIs using Forge's DeferredRegister.
 */
public class KitMenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FantasticKits.MOD_ID);

    /**
     * Kit Editor Menu - main edit screen with item slots, group selection, and command tabs.
     */
    public static final RegistryObject<MenuType<KitEditMenu>> KIT_EDIT_MENU =
            MENUS.register("kit_edit", () -> IForgeMenuType.create(
                    (windowId, inv, data) -> {
                        String kitName = data.readUtf(256);
                        return new KitEditMenu(windowId, inv, kitName);
                    }
            ));

    /**
     * Kit Claim Menu - player-facing menu to browse and claim available kits.
     */
    public static final RegistryObject<MenuType<KitClaimMenu>> KIT_CLAIM_MENU =
            MENUS.register("kit_claim", () -> IForgeMenuType.create(
                    (windowId, inv, data) -> {
                        String kitName = data.readUtf(256);
                        return new KitClaimMenu(windowId, inv, kitName);
                    }
            ));
}
