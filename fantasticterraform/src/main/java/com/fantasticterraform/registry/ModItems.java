package com.fantasticterraform.registry;

import com.fantasticterraform.FantasticTerraform;
import com.fantasticterraform.selection.SelectionWand;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de items del mod. Solo registra la varita de seleccion.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FantasticTerraform.MOD_ID);

    public static final RegistryObject<Item> SELECTION_WAND =
            ITEMS.register("selection_wand", SelectionWand::new);

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
