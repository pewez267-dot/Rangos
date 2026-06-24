// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import java.util.Iterator;
import com.fscrates.crate.LootEngine;
import java.util.Random;
import com.fscrates.item.CrateItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import com.fscrates.config.RewardEntry;
import net.minecraft.server.level.ServerPlayer;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.OpenEditorPacket;
import com.fscrates.config.CrateConfig;
import java.util.List;
import com.fscrates.config.Rarity;
import java.util.ArrayList;
import net.minecraft.commands.SharedSuggestionProvider;
import com.fscrates.crate.CrateRegistry;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;

public final class FSCrateCommand
{
    private FSCrateCommand() {
    }
    
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal("fscrate").requires(s -> s.hasPermission(4))).executes(FSCrateCommand::help);
        root.then(Commands.literal("help").executes(FSCrateCommand::help));
        root.then(Commands.literal("create").executes(FSCrateCommand::create));
        root.then(Commands.literal("list").executes(FSCrateCommand::list));
        root.then(((LiteralArgumentBuilder)Commands.literal("editor").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate editor give <jugador>"))).then(((LiteralArgumentBuilder)Commands.literal("give").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate editor give <jugador>"))).then(Commands.argument("player", (ArgumentType)EntityArgument.player()).executes(FSCrateCommand::giveEditorWand))));
        root.then(((LiteralArgumentBuilder)Commands.literal("give").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(((RequiredArgumentBuilder)Commands.argument("player", (ArgumentType)EntityArgument.player()).executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(Commands.argument("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveCrate))));
        root.then(((LiteralArgumentBuilder)Commands.literal("key").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((LiteralArgumentBuilder)Commands.literal("give").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((RequiredArgumentBuilder)Commands.argument("player", (ArgumentType)EntityArgument.player()).executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>  (tier: common/rare/epic/legendary/mythic)"))).then(Commands.argument("tier", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestTiers((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveKey)))));
        root.then(((LiteralArgumentBuilder)Commands.literal("preview").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate preview <crate>"))).then(Commands.argument("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::preview)));
        root.then(((LiteralArgumentBuilder)Commands.literal("delete").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate delete <crate>"))).then(Commands.argument("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::delete)));
        dispatcher.register((LiteralArgumentBuilder)root);
    }
    
    private static int usage(final CommandContext<CommandSourceStack> ctx, final String usage) {
        ((CommandSourceStack)ctx.getSource()).sendSystemMessage((Component)Component.literal("§eUso: §f" + usage));
        return 1;
    }
    
    private static int help(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack s = (CommandSourceStack)ctx.getSource();
        s.sendSystemMessage((Component)Component.literal("§d\u2726 §fFantastic Crates §d\u2726 §7comandos:"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate create §7- crea y abre el editor de una crate nueva"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate editor give <jugador> §7- da la §dVarita del Editor §7(click derecho en un cofre para editarlo)"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate give <jugador> <crate> §7- da el item de la crate"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate key give <jugador> <tier> §7- da una llave (common/rare/epic/legendary/mythic)"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate preview <crate> §7- simula 5 aperturas"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate list §7- lista las crates guardadas"));
        s.sendSystemMessage((Component)Component.literal("§e/fscrate delete <crate> §7- elimina una crate"));
        return 1;
    }
    
    private static CompletableFuture<Suggestions> suggestCrates(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggest((Iterable)CrateRegistry.get(((CommandSourceStack)ctx.getSource()).getLevel()).ids(), builder);
        }
        catch (final Exception e) {
            return builder.buildFuture();
        }
    }
    
    private static CompletableFuture<Suggestions> suggestTiers(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        final List<String> tiers = new ArrayList<String>();
        for (final Rarity r : Rarity.values()) {
            tiers.add(r.id());
        }
        return SharedSuggestionProvider.suggest((Iterable)tiers, builder);
    }
    
    private static int create(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final CrateConfig crate = new CrateConfig("crate_" + System.currentTimeMillis() % 100000L);
        crate.rewards.add(exampleItem("minecraft:diamond", 60, "Diamante"));
        crate.rewards.add(exampleItem("minecraft:netherite_ingot", 10, "Netherite"));
        FSNetwork.sendToClient(player, new OpenEditorPacket(crate.save()));
        return 1;
    }
    
    private static RewardEntry exampleItem(final String id, final int chance, final String label) {
        final RewardEntry r = new RewardEntry(RewardEntry.Type.ITEM);
        r.chance = chance;
        r.label = label;
        final Item item = (Item)ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
        r.item = ((item == null) ? ItemStack.EMPTY : new ItemStack((ItemLike)item));
        return r;
    }
    
    private static int giveEditorWand(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        final ItemStack wand = CrateItems.buildEditorWand();
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("§dVarita del Editor§a entregada a " + target.getName().getString() + ". §7Click derecho sobre un cofre para editarlo."), true);
        return 1;
    }
    
    private static int giveCrate(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final CrateConfig crate = CrateRegistry.get(target.serverLevel()).get(id);
        if (crate == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal("No existe la crate '" + id + "'."));
            return 0;
        }
        final ItemStack item = CrateItems.buildCrate(crate);
        if (!target.getInventory().add(item)) {
            target.drop(item, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("§aCrate '" + id + "' entregada a " + target.getName().getString()), true);
        return 1;
    }
    
    private static int giveKey(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        final String tierName = StringArgumentType.getString((CommandContext)ctx, "tier");
        final Rarity tier = Rarity.byName(tierName);
        final ItemStack key = CrateItems.buildKey(tier);
        if (!target.getInventory().add(key)) {
            target.drop(key, false);
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("§aLlave de tier " + String.valueOf(tier.color()) + tier.displayName() + "§a entregada a " + target.getName().getString()), true);
        return 1;
    }
    
    private static int preview(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final CrateConfig crate = CrateRegistry.get(player.serverLevel()).get(id);
        if (crate == null) {
            player.sendSystemMessage((Component)Component.literal("§cNo existe la crate '" + id + "'."));
            return 0;
        }
        final Random random = new Random();
        player.sendSystemMessage((Component)Component.literal("§d\u2726 Vista previa de " + crate.displayName + "§d:"));
        for (int i = 0; i < 5; ++i) {
            final List<RewardEntry> rolled = LootEngine.roll(crate, random);
            final StringBuilder sb = new StringBuilder("§7- ");
            for (final RewardEntry r : rolled) {
                sb.append("§f").append(r.describe()).append("§7, ");
            }
            player.sendSystemMessage((Component)Component.literal(sb.toString()));
        }
        return 1;
    }
    
    private static int delete(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final boolean removed = CrateRegistry.get(player.serverLevel()).remove(id);
        player.sendSystemMessage((Component)Component.literal(removed ? ("§aCrate '" + id + "' eliminada.") : ("§cNo existe la crate '" + id + "'.")));
        return removed ? 1 : 0;
    }
    
    private static int list(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final Set<String> ids = CrateRegistry.get(player.serverLevel()).ids();
        if (ids.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal("§eNo hay crates. Crea una con §f/fscrate create"));
            return 1;
        }
        player.sendSystemMessage((Component)Component.literal("§dCrates (" + ids.size() + "): §f" + String.join(", ", ids)));
        return 1;
    }
    
    private static ServerPlayer player(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
        }
        catch (final Exception e) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
    
    private static ServerPlayer playerArg(final CommandContext<CommandSourceStack> ctx) {
        try {
            return EntityArgument.getPlayer((CommandContext)ctx, "player");
        }
        catch (final Exception e) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal("Jugador no encontrado."));
            return null;
        }
    }
}
