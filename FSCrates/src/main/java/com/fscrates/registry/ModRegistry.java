package com.fscrates.registry;

import com.fscrates.FSCrates;
import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.Rarity;
import com.fscrates.item.KeyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Central registration. The crate is a real placeable block (like a chest)
 * backed by a BlockEntity. There are five tier key items — one per
 * {@link Rarity} — instead of a single crate-bound key.
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

    /** One key item per tier. */
    public static final Map<Rarity, RegistryObject<Item>> KEYS = new EnumMap<>(Rarity.class);

    static {
        for (Rarity rarity : Rarity.values()) {
            KEYS.put(rarity, ITEMS.register("key_" + rarity.id(), () -> new KeyItem(rarity)));
        }
    }

    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate", () ->
                    BlockEntityType.Builder.of(CrateBlockEntity::new, CRATE_BLOCK.get()).build(null));

    public static Item key(Rarity rarity) {
        return KEYS.get(rarity).get();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
