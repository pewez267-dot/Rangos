package com.fscrates.item;

import com.fscrates.client.render.CrateItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * BlockItem de la crate. Solo difiere de un BlockItem normal en que provee un
 * renderizador de item personalizado (BEWLR) que dibuja el modelo 3D real del
 * cofre por rareza en lugar de la textura plana.
 *
 * initializeClient es la via oficial de Forge para esto y solo se invoca en el
 * cliente; el cuerpo (que referencia clases de cliente) nunca se ejecuta en el
 * servidor dedicado, por lo que es seguro tener esta clase en codigo comun.
 */
public class CrateBlockItem extends BlockItem {
    public CrateBlockItem(final Block block, final Properties props) {
        super(block, props);
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CrateItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
