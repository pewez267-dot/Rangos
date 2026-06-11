package com.gbaminecraft.minecraft.registry;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.minecraft.block.GBABlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, GBAMod.MOD_ID);

    public static final RegistryObject<Block> GBA_CONSOLE = BLOCKS.register(
            "gba_console",
            () -> new GBABlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            )
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    // Helper to register BlockItems
    public static <T extends Block> RegistryObject<Item> fromBlock(RegistryObject<T> block) {
        return ModItems.ITEMS.register(block.getId().getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
