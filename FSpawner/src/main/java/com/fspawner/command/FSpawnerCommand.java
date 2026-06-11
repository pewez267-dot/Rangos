package com.fspawner.command;

import com.fspawner.config.SpawnerConfig;
import com.fspawner.item.SpawnerItemBuilder;
import com.fspawner.network.FSNetwork;
import com.fspawner.network.OpenScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * {@code /fspawner} command tree. Every branch requires OP level 4.
 */
public final class FSpawnerCommand {

    private FSpawnerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fspawner")
                .requires(source -> source.hasPermission(4))
                .executes(FSpawnerCommand::openEditor)
                .then(Commands.literal("create").executes(FSpawnerCommand::createNew))
                .then(Commands.literal("pickup").executes(FSpawnerCommand::pickup))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(FSpawnerCommand::savePreset)))
                .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> suggestPresets(c, b))
                                .executes(FSpawnerCommand::loadPreset)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> suggestPresets(c, b))
                                .executes(FSpawnerCommand::deletePreset))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPresets(
            CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            return SharedSuggestionProvider.suggest(FSpawnerPresets.get(level).names(), builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    // ------------------------------------------------------------------

    private static int openEditor(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        // Seed the GUI from the held Fantastic Spawner if there is one.
        SpawnerConfig cfg = SpawnerItemBuilder.readConfig(player.getMainHandItem());
        if (cfg == null) {
            cfg = SpawnerItemBuilder.readConfig(player.getOffhandItem());
        }
        if (cfg == null) {
            cfg = new SpawnerConfig();
        }
        FSNetwork.sendToClient(player, new OpenScreenPacket(cfg.save()));
        return 1;
    }

    private static int createNew(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        FSNetwork.sendToClient(player, new OpenScreenPacket(new SpawnerConfig().save()));
        return 1;
    }

    private static int pickup(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        try {
            HitResult hit = player.pick(6.0D, 1.0F, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                fail(player);
                return 0;
            }
            BlockHitResult blockHit = (BlockHitResult) hit;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(blockHit.getBlockPos());
            if (!(be instanceof SpawnerBlockEntity)) {
                fail(player);
                return 0;
            }
            CompoundTag beTag = be.saveWithoutMetadata();
            ItemStack item = SpawnerItemBuilder.fromBlockEntityNbt(beTag);
            if (item == null) {
                fail(player);
                return 0;
            }
            level.removeBlock(blockHit.getBlockPos(), false);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.sendSystemMessage(Component.translatable("fspawner.command.pickup.success"));
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("\u00A7cError en pickup: " + e.getMessage()));
            return 0;
        }
    }

    private static void fail(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("fspawner.command.pickup.fail"));
    }

    private static int savePreset(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name");
        SpawnerConfig cfg = SpawnerItemBuilder.readConfig(player.getMainHandItem());
        if (cfg == null) {
            player.sendSystemMessage(Component.literal("\u00A7cSostén un item Fantastic Spawner en la mano para guardar su configuración."));
            return 0;
        }
        try {
            FSpawnerPresets.get(player.serverLevel()).put(name, cfg.save());
            player.sendSystemMessage(Component.translatable("fspawner.command.saved", name));
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("\u00A7cError al guardar: " + e.getMessage()));
            return 0;
        }
    }

    private static int loadPreset(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name");
        try {
            CompoundTag cfgTag = FSpawnerPresets.get(player.serverLevel()).get(name);
            if (cfgTag == null) {
                player.sendSystemMessage(Component.translatable("fspawner.command.not_found", name));
                return 0;
            }
            ItemStack item = SpawnerItemBuilder.build(SpawnerConfig.load(cfgTag));
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            player.sendSystemMessage(Component.translatable("fspawner.command.loaded", name));
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("\u00A7cError al cargar: " + e.getMessage()));
            return 0;
        }
    }

    private static int deletePreset(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name");
        try {
            boolean removed = FSpawnerPresets.get(player.serverLevel()).remove(name);
            player.sendSystemMessage(Component.translatable(
                    removed ? "fspawner.command.deleted" : "fspawner.command.not_found", name));
            return removed ? 1 : 0;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("\u00A7cError al eliminar: " + e.getMessage()));
            return 0;
        }
    }

    private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
}
