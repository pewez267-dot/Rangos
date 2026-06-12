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
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;

public final class FSCrateCommand
{
    private FSCrateCommand() {
    }
    
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("fscrate").requires(s -> s.m_6761_(4))).executes(FSCrateCommand::help)).then(Commands.m_82127_("help").executes(FSCrateCommand::help))).then(Commands.m_82127_("create").executes(FSCrateCommand::create))).then(Commands.m_82127_("list").executes(FSCrateCommand::list))).then(((LiteralArgumentBuilder)Commands.m_82127_("edit").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate edit <crate>"))).then(Commands.m_82129_("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::edit)))).then(((LiteralArgumentBuilder)Commands.m_82127_("give").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(((RequiredArgumentBuilder)Commands.m_82129_("player", (ArgumentType)EntityArgument.m_91466_()).executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate give <jugador> <crate>"))).then(Commands.m_82129_("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveCrate))))).then(((LiteralArgumentBuilder)Commands.m_82127_("key").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((LiteralArgumentBuilder)Commands.m_82127_("give").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>"))).then(((RequiredArgumentBuilder)Commands.m_82129_("player", (ArgumentType)EntityArgument.m_91466_()).executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate key give <jugador> <tier>  (tier: common/rare/epic/legendary/mythic)"))).then(Commands.m_82129_("tier", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestTiers((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::giveKey)))))).then(((LiteralArgumentBuilder)Commands.m_82127_("preview").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate preview <crate>"))).then(Commands.m_82129_("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::preview)))).then(((LiteralArgumentBuilder)Commands.m_82127_("delete").executes(c -> usage((CommandContext<CommandSourceStack>)c, "/fscrate delete <crate>"))).then(Commands.m_82129_("crate", (ArgumentType)StringArgumentType.word()).suggests((c, b) -> suggestCrates((CommandContext<CommandSourceStack>)c, b)).executes(FSCrateCommand::delete))));
    }
    
    private static int usage(final CommandContext<CommandSourceStack> ctx, final String usage) {
        ((CommandSourceStack)ctx.getSource()).m_243053_((Component)Component.m_237113_("§eUso: §f" + usage));
        return 1;
    }
    
    private static int help(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack s = (CommandSourceStack)ctx.getSource();
        s.m_243053_((Component)Component.m_237113_("§d\u2726 §fFantastic Crates §d\u2726 §7comandos:"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate create §7- crea y abre el editor de una crate nueva"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate edit <crate> §7- edita una crate guardada"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate give <jugador> <crate> §7- da el item de la crate"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate key give <jugador> <tier> §7- da una llave (common/rare/epic/legendary/mythic)"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate preview <crate> §7- simula 5 aperturas"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate list §7- lista las crates guardadas"));
        s.m_243053_((Component)Component.m_237113_("§e/fscrate delete <crate> §7- elimina una crate"));
        return 1;
    }
    
    private static CompletableFuture<Suggestions> suggestCrates(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.m_82970_((Iterable)CrateRegistry.get(((CommandSourceStack)ctx.getSource()).m_81372_()).ids(), builder);
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
        return SharedSuggestionProvider.m_82970_((Iterable)tiers, builder);
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
        final Item item = (Item)ForgeRegistries.ITEMS.getValue(ResourceLocation.m_135820_(id));
        r.item = ((item == null) ? ItemStack.f_41583_ : new ItemStack((ItemLike)item));
        return r;
    }
    
    private static int edit(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final CrateConfig crate = CrateRegistry.get(player.m_284548_()).get(id);
        if (crate == null) {
            player.m_213846_((Component)Component.m_237113_("§cNo existe la crate '" + id + "'."));
            return 0;
        }
        FSNetwork.sendToClient(player, new OpenEditorPacket(crate.save()));
        return 1;
    }
    
    private static int giveCrate(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer target = playerArg(ctx);
        if (target == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final CrateConfig crate = CrateRegistry.get(target.m_284548_()).get(id);
        if (crate == null) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_("No existe la crate '" + id + "'."));
            return 0;
        }
        final ItemStack item = CrateItems.buildCrate(crate);
        if (!target.m_150109_().m_36054_(item)) {
            target.m_36176_(item, false);
        }
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("§aCrate '" + id + "' entregada a " + target.m_7755_().getString()), true);
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
        if (!target.m_150109_().m_36054_(key)) {
            target.m_36176_(key, false);
        }
        ((CommandSourceStack)ctx.getSource()).m_288197_(() -> Component.m_237113_("§aLlave de tier " + String.valueOf(tier.color()) + tier.displayName() + "§a entregada a " + target.m_7755_().getString()), true);
        return 1;
    }
    
    private static int preview(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final CrateConfig crate = CrateRegistry.get(player.m_284548_()).get(id);
        if (crate == null) {
            player.m_213846_((Component)Component.m_237113_("§cNo existe la crate '" + id + "'."));
            return 0;
        }
        final Random random = new Random();
        player.m_213846_((Component)Component.m_237113_("§d\u2726 Vista previa de " + crate.displayName + "§d:"));
        for (int i = 0; i < 5; ++i) {
            final List<RewardEntry> rolled = LootEngine.roll(crate, random);
            final StringBuilder sb = new StringBuilder("§7- ");
            for (final RewardEntry r : rolled) {
                sb.append("§f").append(r.describe()).append("§7, ");
            }
            player.m_213846_((Component)Component.m_237113_(sb.toString()));
        }
        return 1;
    }
    
    private static int delete(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final String id = StringArgumentType.getString((CommandContext)ctx, "crate");
        final boolean removed = CrateRegistry.get(player.m_284548_()).remove(id);
        player.m_213846_((Component)Component.m_237113_(removed ? ("§aCrate '" + id + "' eliminada.") : ("§cNo existe la crate '" + id + "'.")));
        return removed ? 1 : 0;
    }
    
    private static int list(final CommandContext<CommandSourceStack> ctx) {
        final ServerPlayer player = player(ctx);
        if (player == null) {
            return 0;
        }
        final Set<String> ids = CrateRegistry.get(player.m_284548_()).ids();
        if (ids.isEmpty()) {
            player.m_213846_((Component)Component.m_237113_("§eNo hay crates. Crea una con §f/fscrate create"));
            return 1;
        }
        player.m_213846_((Component)Component.m_237113_("§dCrates (" + ids.size() + "): §f" + String.join(", ", ids)));
        return 1;
    }
    
    private static ServerPlayer player(final CommandContext<CommandSourceStack> ctx) {
        try {
            return ((CommandSourceStack)ctx.getSource()).m_81375_();
        }
        catch (final Exception e) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_("Este comando debe ejecutarlo un jugador."));
            return null;
        }
    }
    
    private static ServerPlayer playerArg(final CommandContext<CommandSourceStack> ctx) {
        try {
            return EntityArgument.m_91474_((CommandContext)ctx, "player");
        }
        catch (final Exception e) {
            ((CommandSourceStack)ctx.getSource()).m_81352_((Component)Component.m_237113_("Jugador no encontrado."));
            return null;
        }
    }
}
