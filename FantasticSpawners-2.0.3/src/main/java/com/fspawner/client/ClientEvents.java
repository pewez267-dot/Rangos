// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.List;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import com.fspawner.item.SpawnerItemBuilder;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents
{
    private ClientEvents() {
    }
    
    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        if (!SpawnerItemBuilder.isFantasticSpawner(stack)) {
            return;
        }
        final SpawnerConfig cfg = SpawnerItemBuilder.readConfig(stack);
        if (cfg == null) {
            return;
        }
        final List<Component> tooltip = event.getToolTip();
        final List<Component> ours = TooltipBuilder.build(cfg, Screen.hasShiftDown());
        for (int i = 1; i < ours.size(); ++i) {
            tooltip.add(ours.get(i));
        }
    }
}
