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
                .executes(FSCrateCommand::help)
                .then(Commands.literal("help").executes(FSCrateCommand::help))
                .then(Commands.literal("create").executes(FSCrateCommand::create))
                .then(Commands.literal("list").executes(FSCrateCommand::list))
                .then(Commands.literal("edit")
                        .executes(c -> usage(c, "/fscrate edit <crate>"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::edit)))
                .then(Commands.literal("give")
                        .executes(c -> usage(c, "/fscrate give <jugador> <crate>"))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> usage(c, "/fscrate give <jugador> <crate>"))
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests((c, b) -> suggestCrates(c, b))
                                        .executes(FSCrateCommand::giveCrate))))
                .then(Commands.literal("key")
                        .executes(c -> usage(c, "/fscrate key give <jugador> <tier>"))
                        .then(Commands.literal("give")
                                .executes(c -> usage(c, "/fscrate key give <jugador> <tier>"))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> usage(c, "/fscrate key give <jugador> <tier>  (tier: common/rare/epic/legendary/mythic)"))
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .suggests((c, b) -> suggestTiers(c, b))
                                                .executes(FSCrateCommand::giveKey)))))
                .then(Commands.literal("preview")
                        .executes(c -> usage(c, "/fscrate preview <crate>"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::preview)))
                .then(Commands.literal("delete")
                        .executes(c -> usage(c, "/fscrate delete <crate>"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests((c, b) -> suggestCrates(c, b))
                                .executes(FSCrateCommand::delete))));
    }

    private static int usage(CommandContext<CommandSourceStack> ctx, String usage) {
        ctx.getSource().sendSystemMessage(Component.literal("\u00A7eUso: \u00A7f" + usage));
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack s = ctx.getSource();
        s.sendSystemMessage(Component.literal("\u00A7d\u2726 \u00A7fFantastic Crates \u00A7d\u2726 \u00A77comandos:"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate create \u00A77- crea y abre el editor de una crate nueva"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate edit <crate> \u00A77- edita una crate guardada"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate give <jugador> <crate> \u00A77- da el item de la crate"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate key give <jugador> <tier> \u00A77- da una llave (common/rare/epic/legendary/mythic)"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate preview <crate> \u00A77- simula 5 aperturas"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate list \u00A77- lista las crates guardadas"));
        s.sendSystemMessage(Component.literal("\u00A7e/fscrate delete <crate> \u00A77- elimina una crate"));
        return 1;
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

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTiers(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        java.util.List<String> tiers = new java.util.ArrayList<>();
        for (com.fscrates.config.Rarity r : com.fscrates.config.Rarity.values()) {
            tiers.add(r.id());
        }
        return SharedSuggestionProvider.suggest(tiers, builder);
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

    private static RewardEntry exampleItem(String id, int chance, String label) {
        RewardEntry r = new RewardEntry(RewardEntry.Type.ITEM);
        r.chance = chance;
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
        String tierName = StringArgumentType.getString(ctx, "tier");
        com.fscrates.config.Rarity tier = com.fscrates.config.Rarity.byName(tierName);
        ItemStack key = CrateItems.buildKey(tier);
        if (!target.getInventory().add(key)) {
            target.drop(key, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aLlave de tier "
                + tier.color() + tier.displayName() + "\u00A7a entregada a "
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
