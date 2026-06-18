package com.fantasticchest.commands;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.config.ChestConfig;
import com.fantasticchest.data.ChestDefinition;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.gui.ModMenus;
import com.fantasticchest.item.EditorWandItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The three and only commands, all requiring OP ({@code hasPermissions(4)}):
 * <pre>
 *   /fschest create
 *   /fschest delete &lt;id&gt;
 *   /fschest editor give &lt;jugador&gt;
 * </pre>
 */
public final class FsChestCommand {

    private FsChestCommand() {
    }

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fschest")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("create").executes(FsChestCommand::create))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(FsChestCommand::suggestIds)
                                .executes(FsChestCommand::delete)))
                .then(Commands.literal("editor")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(FsChestCommand::editorGive)))));
    }

    private static CompletableFuture<Suggestions> suggestIds(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        final List<String> ids = new ArrayList<>();
        for (final ChestDefinition d : ChestRegistry.get().all()) {
            ids.add(d.id);
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static int create(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = playerOrNull(ctx);
        if (player == null) {
            return 0;
        }
        ModMenus.openAdminCreate(player);
        return 1;
    }

    private static int delete(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final String id = ChestRegistry.normalizeId(StringArgumentType.getString(ctx, "id"));
        final ChestDefinition def = ChestRegistry.get().get(id);
        if (def == null) {
            source.sendFailure(Component.literal("§cNo existe un cofre con el ID §e" + id + "§c."));
            return 0;
        }
        if (ChestConfig.requirePickupBeforeDelete() && def.inventory != null && !def.inventory.isEmpty()) {
            source.sendFailure(Component.literal("§cEl cofre no esta vacio. Vacialo antes de eliminarlo (config)."));
            return 0;
        }
        if (def.placed && def.pos != null && def.world != null && !def.world.isBlank()) {
            final ResourceLocation dim = ResourceLocation.tryParse(def.world);
            final ServerLevel level = dim == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (level != null) {
                final BlockPos pos = new BlockPos(def.pos.x, def.pos.y, def.pos.z);
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
                    level.removeBlock(pos, false);
                }
            }
        }
        ChestRegistry.get().remove(id);
        source.sendSuccess(() -> Component.literal("§aCofre §e" + id + " §aeliminado."), true);
        return 1;
    }

    private static int editorGive(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(ctx, "player");
        } catch (final Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cJugador no encontrado."));
            return 0;
        }
        final ItemStack wand = EditorWandItem.buildWand();
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§aVarita del Editor entregada a §e" + target.getGameProfile().getName() + "§a."), true);
        return 1;
    }

    private static ServerPlayer playerOrNull(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (final Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cEste comando debe ejecutarlo un jugador."));
            return null;
        }
    }
}
