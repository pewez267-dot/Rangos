package com.fscrates.registry;

import com.fscrates.FSCrates;
import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registration for the crate block, its block-entity and the crate/key
 * items. Using real registered objects makes the crate a true placeable block
 * (like a chest) with persistent NBT, and makes keys a non-placeable item.
 */
public final class ModRegistry {

    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FSCrates.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FSCrates.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FSCrates.MOD_ID);

    /** The physical crate block placed in the world. */
    public static final RegistryObject<Block> CRATE_BLOCK =
            BLOCKS.register("crate", CrateBlock::new);

    /** BlockItem for the crate (carries the config via BlockEntityTag on place). */
    public static final RegistryObject<Item> CRATE_ITEM =
            ITEMS.register("crate", () -> new BlockItem(CRATE_BLOCK.get(), new Item.Properties()));

    /** The key item (plain item: never places a block). */
    public static final RegistryObject<Item> KEY_ITEM =
            ITEMS.register("key", () -> new Item(new Item.Properties()));

    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate", () ->
                    BlockEntityType.Builder.of(CrateBlockEntity::new, CRATE_BLOCK.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
