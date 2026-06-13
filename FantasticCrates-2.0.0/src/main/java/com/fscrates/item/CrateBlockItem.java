// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.item;

import com.fscrates.client.render.CrateItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;

public class CrateBlockItem extends BlockItem
{
    public CrateBlockItem(final Block block, final Item.Properties props) {
        super(block, props);
    }
    
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions)new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CrateItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
