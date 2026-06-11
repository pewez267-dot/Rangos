package com.gbaminecraft.minecraft.registry;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.minecraft.tileentity.GBATileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModTileEntities {

    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GBAMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<GBATileEntity>> GBA_CONSOLE =
            TILE_ENTITIES.register("gba_console",
                    () -> BlockEntityType.Builder
                            .of(GBATileEntity::new, ModBlocks.GBA_CONSOLE.get())
                            .build(null)
            );

    public static void register(IEventBus eventBus) {
        TILE_ENTITIES.register(eventBus);
    }
}
