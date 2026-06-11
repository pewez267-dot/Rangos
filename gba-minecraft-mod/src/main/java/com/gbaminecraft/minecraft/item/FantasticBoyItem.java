package com.gbaminecraft.minecraft.item;

import com.gbaminecraft.minecraft.client.FantasticBoyClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fantastic Boy Advance — the handheld console item.
 * Right-click opens the client-side launcher/emulator screen.
 */
public class FantasticBoyItem extends Item {

    public FantasticBoyItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // Open the GUI on the client only.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> FantasticBoyClient::open);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clic derecho para encender"));
        tooltip.add(Component.literal("ROMs en la carpeta: RomsGBA"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
