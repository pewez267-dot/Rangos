package com.fspawner.client;

import com.fspawner.config.SpawnerConfig;
import com.fspawner.item.SpawnerItemBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/** Client-only Forge bus events: the dynamic FSpawner tooltip. */
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SpawnerItemBuilder.isFantasticSpawner(stack)) {
            return;
        }
        SpawnerConfig cfg = SpawnerItemBuilder.readConfig(stack);
        if (cfg == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        // The custom item name already shows the title, so append our detail
        // lines after it (skip TooltipBuilder's own title at index 0).
        List<Component> ours = TooltipBuilder.build(cfg, Screen.hasShiftDown());
        for (int i = 1; i < ours.size(); i++) {
            tooltip.add(ours.get(i));
        }
    }
}
