package com.fscrates.registry;

import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.item.CrateBlockItem;
import com.fscrates.item.EditorWandItem;
import com.fscrates.item.KeyItem;
import com.fscrates.item.UniqueKeyItem;
import com.mojang.datafixers.types.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCKS, (String)"fscrates");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ITEMS, (String)"fscrates");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCK_ENTITY_TYPES, (String)"fscrates");
    public static final RegistryObject<Block> CRATE_BLOCK = BLOCKS.register("crate", CrateBlock::new);
    public static final RegistryObject<Item> CRATE_ITEM = ITEMS.register("crate", () -> new CrateBlockItem((Block)CRATE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EDITOR_WAND = ITEMS.register("editor_wand", () -> new EditorWandItem());
    // Llave UNIVERSAL: una sola "Fantastic Key" (antes habia 5 llaves por rareza).
    public static final RegistryObject<Item> FANTASTIC_KEY = ITEMS.register("fantastic_key", () -> new KeyItem());
    // Llave UNICA por crate: un solo item cuyo modelo cambia via CustomModelData (50 modelos
    // importados de los packs). Se enlaza a una crate por NBT (crateId).
    public static final RegistryObject<Item> UNIQUE_KEY = ITEMS.register("unique_key", () -> new UniqueKeyItem());
    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE;

    private ModRegistry() {
    }

    public static Item key() {
        return (Item)FANTASTIC_KEY.get();
    }

    public static Item uniqueKey() {
        return (Item)UNIQUE_KEY.get();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    static {
        CRATE_BE = BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder.of(CrateBlockEntity::new, (Block[])new Block[]{(Block)CRATE_BLOCK.get()}).build((Type)null));
    }
}
