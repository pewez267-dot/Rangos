package com.fscrates.command;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.crate.LootEngine;
import com.fscrates.item.CrateItems;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.OpenEditorPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/** {@code /fscrate} command tree. Every branch requires OP level 4. */
public final class FSCrateCommand {

    private FSCrateCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fscrate")
                .requires(s -> s.hasPermission(4))
                .then(Commands.literal("create").executes(FSCrateCommand::create))
                .then(Commands.literal("edit")
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::edit)))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests((c, b) -> suggestCrates(c, b))
                                        .executes(FSCrateCommand::giveCrate))))
                .then(Commands.literal("key")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("crate", StringArgumentType.word())
                                                .suggests((c, b) -> suggestCrates(c, b))
                                                .executes(FSCrateCommand::giveKey)))))
                .then(Commands.literal("preview")
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::preview)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::delete)))
                .then(Commands.literal("list").executes(FSCrateCommand::list)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestCrates(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggest(
                    CrateRegistry.get(ctx.getSource().getLevel()).ids(), builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    // ------------------------------------------------------------------

    private static int create(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        CrateConfig crate = new CrateConfig("crate_" + (System.currentTimeMillis() % 100000));
        // seed with a couple of example rewards so the GUI isn't empty
        crate.rewards.add(exampleItem("minecraft:diamond", 60, "Diamante"));
        crate.rewards.add(exampleItem("minecraft:netherite_ingot", 10, "Netherite"));
        FSNetwork.sendToClient(player, new OpenEditorPacket(crate.save()));
        return 1;
    }

    private static RewardEntry exampleItem(String id, int weight, String label) {
        RewardEntry r = new RewardEntry(RewardEntry.Type.ITEM);
        r.weight = weight;
        r.label = label;
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
        r.item = item == null ? ItemStack.EMPTY : new ItemStack(item);
        return r;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "crate");
        CrateConfig crate = CrateRegistry.get(player.serverLevel()).get(id);
        if (crate == null) {
            player.sendSystemMessage(Component.literal("\u00A7cNo existe la crate '" + id + "'."));
            return 0;
        }
        FSNetwork.sendToClient(player, new OpenEditorPacket(crate.save()));
        return 1;
    }

    private static int giveCrate(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "crate");
        CrateConfig crate = CrateRegistry.get(target.serverLevel()).get(id);
        if (crate == null) {
            ctx.getSource().sendFailure(Component.literal("No existe la crate '" + id + "'."));
            return 0;
        }
        ItemStack item = CrateItems.buildCrate(crate);
        if (!target.getInventory().add(item)) {
            target.drop(item, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aCrate '" + id + "' entregada a "
                + target.getName().getString()), true);
        return 1;
    }

    private static int giveKey(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "crate");
        CrateConfig crate = CrateRegistry.get(target.serverLevel()).get(id);
        if (crate == null) {
            ctx.getSource().sendFailure(Component.literal("No existe la crate '" + id + "'."));
            return 0;
        }
        ItemStack key = CrateItems.buildKey(crate, crate.rarity);
        if (!target.getInventory().add(key)) {
            target.drop(key, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aLlave de '" + id + "' entregada a "
                + target.getName().getString()), true);
        return 1;
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "crate");
        CrateConfig crate = CrateRegistry.get(player.serverLevel()).get(id);
        if (crate == null) {
            player.sendSystemMessage(Component.literal("\u00A7cNo existe la crate '" + id + "'."));
            return 0;
        }
        // Preview rolls 5 times and reports the results without consuming a key.
        Random random = new Random();
        player.sendSystemMessage(Component.literal("\u00A7d\u2726 Vista previa de " + crate.displayName + "\u00A7d:"));
        for (int i = 0; i < 5; i++) {
            List<RewardEntry> rolled = LootEngine.roll(crate, random);
            StringBuilder sb = new StringBuilder("\u00A77- ");
            for (RewardEntry r : rolled) {
                sb.append("\u00A7f").append(r.describe()).append("\u00A77, ");
            }
            player.sendSystemMessage(Component.literal(sb.toString()));
        }
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "crate");
        boolean removed = CrateRegistry.get(player.serverLevel()).remove(id);
        player.sendSystemMessage(Component.literal(removed
                ? "\u00A7aCrate '" + id + "' eliminada."
                : "\u00A7cNo existe la crate '" + id + "'."));
        return removed ? 1 : 0;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        var ids = CrateRegistry.get(player.serverLevel()).ids();
        if (ids.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00A7eNo hay crates. Crea una con \u00A7f/fscrate create"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("\u00A7dCrates (" + ids.size() + "): \u00A7f" + String.join(", ", ids)));
        return 1;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        try {
            return ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }

    private static ServerPlayer playerArg(CommandContext<CommandSourceStack> ctx) {
        try {
            return EntityArgument.getPlayer(ctx, "player");
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Jugador no encontrado."));
            return null;
        }
    }
}
