// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.command;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.fspawner.network.FSNetwork;
import com.fspawner.network.OpenScreenPacket;
import com.fspawner.network.EditContext;
import com.fspawner.config.SpawnerConfig;
import com.fspawner.item.SpawnerItemBuilder;
import com.fspawner.item.FSItems;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;

public final class FSpawnerCommand
{
    private FSpawnerCommand() {
    }
    
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fspawner")
            .requires(s -> s.hasPermission(4))
            .then(Commands.literal("editor")
                .then(Commands.literal("give")
                    .executes(FSpawnerCommand::giveWand)
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(FSpawnerCommand::giveWandToTargets))))
            .then(Commands.literal("create").executes(FSpawnerCommand::createNew))
            .then(Commands.literal("pickup").executes(FSpawnerCommand::pickup))
            .then(Commands.literal("save")
                .then(Commands.argument("name", StringArgumentType.word()).executes(FSpawnerCommand::savePreset)))
            .then(Commands.literal("load")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(FSpawnerCommand::suggestPresets).executes(FSpawnerCommand::loadPreset)))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(FSpawnerCommand::suggestPresets).executes(FSpawnerCommand::deletePreset))));
    }

    private static int giveWand(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        giveWandTo(player);
        return 1;
    }

    private static int giveWandToTargets(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "player");
        for (final ServerPlayer target : targets) {
            giveWandTo(target);
        }
        final int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("fspawner.command.wand_given_others", count), true);
        return count;
    }

    private static void giveWandTo(final ServerPlayer target) {
        final ItemStack wand = new ItemStack(FSItems.SPAWNER_WAND.get());
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        target.sendSystemMessage(Component.translatable("fspawner.command.wand_given"));
    }
    
    private static CompletableFuture<Suggestions> suggestPresets(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        try {
            final ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
            return SharedSuggestionProvider.suggest((Iterable)FSpawnerPresets.get(level).names(), builder);
        }
        catch (final Exception e) {
            return builder.buildFuture();
        }
    }
    
    private static int createNew(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        FSNetwork.sendToClient(player, new OpenScreenPacket(new SpawnerConfig().save(), EditContext.newSession()));
        return 1;
    }
    
    private static int pickup(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        try {
            final HitResult hit = player.pick(6.0, 1.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                fail(player);
                return 0;
            }
            final BlockHitResult bh = (BlockHitResult)hit;
            final ServerLevel level = player.serverLevel();
            final BlockEntity be = level.getBlockEntity(bh.getBlockPos());
            if (!(be instanceof SpawnerBlockEntity)) {
                fail(player);
                return 0;
            }
            final CompoundTag beTag = be.saveWithoutMetadata();
            final ItemStack item = SpawnerItemBuilder.fromBlockEntityNbt(beTag);
            if (item == null) {
                fail(player);
                return 0;
            }
            level.removeBlock(bh.getBlockPos(), false);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.sendSystemMessage((Component)Component.translatable("fspawner.command.pickup.success"));
            return 1;
        }
        catch (final Exception e) {
            player.sendSystemMessage((Component)Component.literal("§cError en pickup: " + e.getMessage()));
            return 0;
        }
    }
    
    private static void fail(final ServerPlayer player) {
        player.sendSystemMessage((Component)Component.translatable("fspawner.command.pickup.fail"));
    }
    
    private static int savePreset(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        final String name = StringArgumentType.getString((CommandContext)ctx, "name");
        SpawnerConfig cfg = SpawnerItemBuilder.readConfig(player.getMainHandItem());
        if (cfg == null) {
            cfg = SpawnerItemBuilder.readConfig(player.getOffhandItem());
        }
        if (cfg == null) {
            player.sendSystemMessage((Component)Component.literal("§cSost\u00e9n un Fantastic Spawner para guardar su configuraci\u00f3n."));
            return 0;
        }
        try {
            FSpawnerPresets.get(player.serverLevel()).put(name, cfg.save());
            player.sendSystemMessage((Component)Component.translatable("fspawner.command.saved", new Object[] { name }));
            return 1;
        }
        catch (final Exception e) {
            player.sendSystemMessage((Component)Component.literal("§cError al guardar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int loadPreset(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        final String name = StringArgumentType.getString((CommandContext)ctx, "name");
        try {
            final CompoundTag cfgTag = FSpawnerPresets.get(player.serverLevel()).get(name);
            if (cfgTag == null) {
                player.sendSystemMessage((Component)Component.translatable("fspawner.command.not_found", new Object[] { name }));
                return 0;
            }
            final ItemStack item = SpawnerItemBuilder.build(SpawnerConfig.load(cfgTag));
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.sendSystemMessage((Component)Component.translatable("fspawner.command.loaded", new Object[] { name }));
            return 1;
        }
        catch (final Exception e) {
            player.sendSystemMessage((Component)Component.literal("§cError al cargar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int deletePreset(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        final String name = StringArgumentType.getString((CommandContext)ctx, "name");
        try {
            final boolean removed = FSpawnerPresets.get(player.serverLevel()).remove(name);
            player.sendSystemMessage((Component)Component.translatable(removed ? "fspawner.command.deleted" : "fspawner.command.not_found", new Object[] { name }));
            return removed ? 1 : 0;
        }
        catch (final Exception e) {
            player.sendSystemMessage((Component)Component.literal("§cError al eliminar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static ServerPlayer playerOrNull(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        }
        catch (final Exception e) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
}
