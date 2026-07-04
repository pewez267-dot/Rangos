package com.fscrates.registry;

import com.fscrates.block.CrateBlock;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.Rarity;
import com.fscrates.item.CrateBlockItem;
import com.fscrates.item.EditorWandItem;
import com.fscrates.item.KeyItem;
import com.mojang.datafixers.types.Type;
import java.util.EnumMap;
import java.util.Map;
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
    public static final Map<Rarity, RegistryObject<? extends Item>> KEYS = new EnumMap<Rarity, RegistryObject<? extends Item>>(Rarity.class);
    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE;

    private ModRegistry() {
    }

    public static Item key(Rarity rarity) {
        return (Item)KEYS.get(rarity).get();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    static {
        for (Rarity rarity : Rarity.values()) {
            KEYS.put(rarity, (RegistryObject<? extends Item>)ITEMS.register("key_" + rarity.id(), () -> new KeyItem(rarity)));
        }
        CRATE_BE = BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder.of(CrateBlockEntity::new, (Block[])new Block[]{(Block)CRATE_BLOCK.get()}).build((Type)null));
    }
}

