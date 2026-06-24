package com.fspawner.item;

import com.fspawner.config.SpawnerConfig;
import com.fspawner.network.EditContext;
import com.fspawner.network.FSNetwork;
import com.fspawner.network.OpenScreenPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.List;

/**
 * Varita del Editor de Spawners.
 *
 * Con click derecho sobre cualquier spawner (vanilla o Fantastic Spawner ya
 * colocado) abre la GUI de edicion de ESE spawner concreto y guarda los cambios
 * en su sitio. Reemplaza al antiguo comando "/fspawner edit".
 */
public class SpawnerWandItem extends Item {
    public SpawnerWandItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockEntity be = level.getBlockEntity(pos);

        // Solo actua sobre spawners; cualquier otro bloque se ignora.
        if (!(be instanceof SpawnerBlockEntity)) {
            return InteractionResult.PASS;
        }

        // El cliente solo confirma la interaccion (mueve el brazo); la GUI la
        // abre el servidor enviando el OpenScreenPacket.
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        final Player player = context.getPlayer();
        if (player instanceof ServerPlayer) {
            final ServerPlayer sp = (ServerPlayer) player;
            if (!sp.hasPermissions(4)) {
                sp.sendSystemMessage(Component.translatable("fspawner.command.no_permission"));
                return InteractionResult.FAIL;
            }
            final CompoundTag beTag = be.saveWithoutMetadata();
            CompoundTag cfgTag = SpawnerItemBuilder.extractConfigForEditing(beTag);
            if (cfgTag == null) {
                cfgTag = new SpawnerConfig().save();
            }
            FSNetwork.sendToClient(sp, new OpenScreenPacket(cfgTag, EditContext.block(pos)));
            sp.sendSystemMessage(Component.translatable("fspawner.wand.editing"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(final ItemStack stack) {
        // Efecto de brillo encantado para que se vea "magica".
        return true;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(Component.translatable("fspawner.wand.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("fspawner.wand.tooltip2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
