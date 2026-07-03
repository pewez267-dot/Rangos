package com.fshop.command;

import com.fshop.FShop;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers and implements the /fshop command tree. */
@Mod.EventBusSubscriber(modid = FShop.MOD_ID)
public final class FShopCommand {
   private FShopCommand() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   public static void register(CommandDispatcher<CommandSourceStack> d) {
      d.register(Commands.literal("fshop")
            .then(Commands.literal("create")
                  .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(FShopCommands::createShop)))
            .then(Commands.literal("buy").executes(FShopCommands::buy))
            .then(Commands.literal("edit").executes(FShopCommands::edit))
            .then(Commands.literal("collect").executes(FShopCommands::collect))
            .then(Commands.literal("balance").executes(FShopCommands::balance))
            .then(Commands.literal("help").executes(FShopCommands::help))
            .then(adminRoot())
            .executes(FShopCommands::help));
   }

   private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> adminRoot() {
      return Commands.literal("admin").requires(src -> src.hasPermission(2))
            .then(Commands.literal("wand").executes(AdminCommands::giveWand))
            .then(Commands.literal("create").executes(AdminCommands::createMainShop))
            .then(Commands.literal("collect").executes(AdminCommands::collectMain))
            .then(Commands.literal("reload").executes(AdminCommands::reload))
            .then(Commands.literal("zone")
                  .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                              .executes(AdminCommands::zoneCreate)))
                  .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                              .executes(AdminCommands::zoneRemove)))
                  .then(Commands.literal("list").executes(AdminCommands::zoneList)))
            .then(Commands.literal("shop")
                  .then(Commands.literal("list").executes(AdminCommands::shopList))
                  .then(Commands.literal("removeall")
                        .then(Commands.argument("target", EntityArgument.player())
                              .executes(AdminCommands::shopRemoveAll))))
            .then(Commands.literal("coins")
                  .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.player())
                              .then(Commands.argument("coin", StringArgumentType.word())
                                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                          .executes(ctx -> AdminCommands.coins(ctx, true))))))
                  .then(Commands.literal("take")
                        .then(Commands.argument("target", EntityArgument.player())
                              .then(Commands.argument("coin", StringArgumentType.word())
                                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                          .executes(ctx -> AdminCommands.coins(ctx, false)))))));
   }
}
