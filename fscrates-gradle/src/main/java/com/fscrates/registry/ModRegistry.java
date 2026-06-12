// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.registry;

import java.util.EnumMap;
import java.util.function.Supplier;
import com.fscrates.block.CrateBlock;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.BlockItem;
import com.fscrates.item.KeyItem;
import com.mojang.datafixers.types.Type;
import net.minecraftforge.eventbus.api.IEventBus;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.Rarity;
import java.util.Map;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;

public final class ModRegistry
{
    public static final DeferredRegister<Block> BLOCKS;
    public static final DeferredRegister<Item> ITEMS;
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES;
    public static final RegistryObject<Block> CRATE_BLOCK;
    public static final RegistryObject<Item> CRATE_ITEM;
    public static final Map<Rarity, RegistryObject<? extends Item>> KEYS;
    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE;
    
    private ModRegistry() {
    }
    
    public static Item key(final Rarity rarity) {
        return (Item)ModRegistry.KEYS.get(rarity).get();
    }
    
    public static void register(final IEventBus bus) {
        ModRegistry.BLOCKS.register(bus);
        ModRegistry.ITEMS.register(bus);
        ModRegistry.BLOCK_ENTITIES.register(bus);
    }
    
    static {
        BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "fscrates");
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "fscrates");
        BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "fscrates");
        CRATE_BLOCK = ModRegistry.BLOCKS.register("crate", CrateBlock::new);
        CRATE_ITEM = ModRegistry.ITEMS.register("crate", () ->
            new com.fscrates.item.CrateBlockItem((Block)ModRegistry.CRATE_BLOCK.get(), new Item.Properties()));
        KEYS = new EnumMap<Rarity, RegistryObject<? extends Item>>(Rarity.class);
        final Rarity[] values = Rarity.values();
        for (int length = values.length, i = 0; i < length; ++i) {
            final Rarity rarity = values[i];
            ModRegistry.KEYS.put(rarity, ModRegistry.ITEMS.register("key_" + rarity.id(), () -> new KeyItem(rarity)));
        }
        CRATE_BE = ModRegistry.BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder.of(CrateBlockEntity::new, new Block[] { (Block)ModRegistry.CRATE_BLOCK.get() }).build((Type)null));
    }
}
