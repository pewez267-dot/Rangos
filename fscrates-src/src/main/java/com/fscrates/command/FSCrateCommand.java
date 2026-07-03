package com.fscrates.command;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.crate.LootEngine;
import com.fscrates.item.CrateItems;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.OpenEditorPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public final class FSCrateCommand {
    private FSCrateCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder root = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"fscrate").requires(s -> s.hasPermission(4))).executes(FSCrateCommand::help);
        root.then(Commands.literal((String)"help").executes(FSCrateCommand::help));
        root.then(Commands.literal((String)"create").executes(FSCrateCommand::create));
        root.then(Commands.literal((String)"list").executes(FSCrateCommand::list));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"editor").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate editor give <jugador>"))).then(((LiteralArgumentBuilder)Commands.literal((String)"give").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate editor give <jugador>"))).then(Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(FSCrateCommand::giveEditorWand))));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"give").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(((RequiredArgumentBuilder)Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(Commands.argument((String)"crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> FSCrateCommand.suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveCrate))));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"key").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((LiteralArgumentBuilder)Commands.literal((String)"give").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((RequiredArgumentBuilder)Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>  (tier: common/rare/epic/legendary/mythic)"))).then(Commands.argument((String)"tier", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> FSCrateCommand.suggestTiers((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveKey)))));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"preview").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate preview <crate>"))).then(Commands.argument((String)"crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> FSCrateCommand.suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::preview)));
        root.then(((LiteralArgumentBuilder)Commands.literal((String)"delete").executes(c -> FSCrateCommand.usage((CommandContext<CommandSourceStack>)c, "/fscrate delete <crate>"))).then(Commands.argument((String)"crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> FSCrateCommand.suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::delete)));
        dispatcher.register(root);
    }

    private static int usage(CommandContext<CommandSourceStack> ctx, String usage) {
        ((CommandSourceStack)ctx.getSource()).sendSystemMessage((Component)Component.literal((String)("\u00a7eUso: \u00a7f" + usage)));
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack s = (CommandSourceStack)ctx.getSource();
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7d\u2726 \u00a7fFantastic Crates \u00a7d\u2726 \u00a77comandos:"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate create \u00a77- crea y abre el editor de una crate nueva"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate editor give <jugador> \u00a77- da la \u00a7dVarita del Editor \u00a77(click derecho en un cofre para editarlo)"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate give <jugador> <crate> \u00a77- da el item de la crate"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate key give <jugador> <tier> \u00a77- da una llave (common/rare/epic/legendary/mythic)"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate preview <crate> \u00a77- simula 5 aperturas"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate list \u00a77- lista las crates guardadas"));
        s.sendSystemMessage((Component)Component.literal((String)"\u00a7e/fscrate delete <crate> \u00a77- elimina una crate"));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestCrates(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggest(CrateRegistry.get(((CommandSourceStack)ctx.getSource()).getLevel()).ids(), (SuggestionsBuilder)builder);
        }
        catch (Exception var3) {
            return builder.buildFuture();
        }
    }

    private static CompletableFuture<Suggestions> suggestTiers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ArrayList<String> tiers = new ArrayList<String>();
        for (Rarity r : Rarity.values()) {
            tiers.add(r.id());
        }
        return SharedSuggestionProvider.suggest(tiers, (SuggestionsBuilder)builder);
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = FSCrateCommand.player(ctx);
        if (player == null) {
            return 0;
        }
        CrateConfig crate = new CrateConfig("crate_" + System.currentTimeMillis() % 100000L);
        crate.rewards.add(FSCrateCommand.exampleItem("minecraft:diamond", 60, "Diamante"));
        crate.rewards.add(FSCrateCommand.exampleItem("minecraft:netherite_ingot", 10, "Netherite"));
        FSNetwork.sendToClient(player, new OpenEditorPacket(crate.save()));
        return 1;
    }

    private static RewardEntry exampleItem(String id, int chance, String label) {
        RewardEntry r = new RewardEntry(RewardEntry.Type.ITEM);
        r.chance = chance;
        r.label = label;
        Item item = (Item)ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse((String)id));
        r.item = item == null ? ItemStack.EMPTY : new ItemStack((ItemLike)item);
        return r;
    }

    private static int giveEditorWand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = FSCrateCommand.playerArg(ctx);
        if (target == null) {
            return 0;
        }
        ItemStack wand = CrateItems.buildEditorWand();
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7dVarita del Editor\u00a7a entregada a " + target.getName().getString() + ". \u00a77Click derecho sobre un cofre para editarlo.")), true);
        return 1;
    }

    private static int giveCrate(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = FSCrateCommand.playerArg(ctx);
        if (target == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, (String)"crate");
        CrateConfig crate = CrateRegistry.get(target.serverLevel()).get(id);
        if (crate == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)("No existe la crate '" + id + "'.")));
            return 0;
        }
        ItemStack item = CrateItems.buildCrate(crate);
        if (!target.getInventory().add(item)) {
            target.drop(item, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aCrate '" + id + "' entregada a " + target.getName().getString())), true);
        return 1;
    }

    private static int giveKey(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = FSCrateCommand.playerArg(ctx);
        if (target == null) {
            return 0;
        }
        String tierName = StringArgumentType.getString(ctx, (String)"tier");
        Rarity tier = Rarity.byName(tierName);
        ItemStack key = CrateItems.buildKey(tier);
        if (!target.getInventory().add(key)) {
            target.drop(key, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aLlave de tier " + tier.color() + tier.displayName() + "\u00a7a entregada a " + target.getName().getString())), true);
        return 1;
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = FSCrateCommand.player(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, (String)"crate");
        CrateConfig crate = CrateRegistry.get(player.serverLevel()).get(id);
        if (crate == null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7cNo existe la crate '" + id + "'.")));
            return 0;
        }
        Random random = new Random();
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7d\u2726 Vista previa de " + crate.displayName + "\u00a7d:")));
        for (int i = 0; i < 5; ++i) {
            List<RewardEntry> rolled = LootEngine.roll(crate, random);
            StringBuilder sb = new StringBuilder("\u00a77- ");
            for (RewardEntry r : rolled) {
                sb.append("\u00a7f").append(r.describe()).append("\u00a77, ");
            }
            player.sendSystemMessage((Component)Component.literal((String)sb.toString()));
        }
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = FSCrateCommand.player(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, (String)"crate");
        boolean removed = CrateRegistry.get(player.serverLevel()).remove(id);
        player.sendSystemMessage((Component)Component.literal((String)(removed ? "\u00a7aCrate '" + id + "' eliminada." : "\u00a7cNo existe la crate '" + id + "'.")));
        return removed ? 1 : 0;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = FSCrateCommand.player(ctx);
        if (player == null) {
            return 0;
        }
        Set<String> ids = CrateRegistry.get(player.serverLevel()).ids();
        if (ids.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7eNo hay crates. Crea una con \u00a7f/fscrate create"));
            return 1;
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7dCrates (" + ids.size() + "): \u00a7f" + String.join((CharSequence)", ", ids))));
        return 1;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        try {
            return ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        }
        catch (Exception var2) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }

    private static ServerPlayer playerArg(CommandContext<CommandSourceStack> ctx) {
        try {
            return EntityArgument.getPlayer(ctx, (String)"player");
        }
        catch (Exception var2) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"Jugador no encontrado."));
            return null;
        }
    }
}

