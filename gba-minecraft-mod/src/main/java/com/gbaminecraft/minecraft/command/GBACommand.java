package com.gbaminecraft.minecraft.command;

import com.gbaminecraft.minecraft.item.GBACartridgeItem;
import com.gbaminecraft.minecraft.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.cartridge.Cartridge;

import java.io.*;
import java.nio.file.*;

/**
 * GBA commands:
 *   /gba give           — Give the player a GBA Console block
 *   /gba cartridge      — Give the player an empty GBA Cartridge item
 *   /gba load <file>    — Load a .gba ROM from the world save folder into a cartridge in hand
 *   /gba info           — Show info about the cartridge in hand
 */
@Mod.EventBusSubscriber(modid = GBAMod.MOD_ID)
public class GBACommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("gba")
                .requires(src -> src.hasPermission(2))

                // /gba give — give GBA console block
                .then(Commands.literal("give")
                        .executes(ctx -> giveConsole(ctx)))

                // /gba cartridge — give empty cartridge
                .then(Commands.literal("cartridge")
                        .executes(ctx -> giveCartridge(ctx)))

                // /gba load <filename> — load ROM file
                .then(Commands.literal("load")
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> loadROM(ctx,
                                        StringArgumentType.getString(ctx, "file")))))

                // /gba info — show cartridge info
                .then(Commands.literal("info")
                        .executes(ctx -> cartridgeInfo(ctx)))
        );
    }

    private static int giveConsole(CommandContext<CommandSourceStack> ctx) {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = new ItemStack(ModItems.GBA_CONSOLE.get());
            player.getInventory().add(stack);
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Gave GBA Console. Place it in the world and right-click to open."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Must be a player: " + e.getMessage()));
            return 0;
        }
    }

    private static int giveCartridge(CommandContext<CommandSourceStack> ctx) {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = new ItemStack(ModItems.GBA_CARTRIDGE.get());
            player.getInventory().add(stack);
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Gave empty GBA Cartridge. Use /gba load <file.gba> to load a ROM."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Must be a player: " + e.getMessage()));
            return 0;
        }
    }

    private static int loadROM(CommandContext<CommandSourceStack> ctx, String fileName) {
        try {
            Player player = ctx.getSource().getPlayerOrException();

            // Look for ROM in the world save folder or a 'roms' subfolder
            Path worldPath = ctx.getSource().getServer().getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT);
            Path romPath = worldPath.resolve("roms").resolve(fileName);
            if (!romPath.toFile().exists()) {
                romPath = worldPath.resolve(fileName);
            }
            if (!romPath.toFile().exists()) {
                ctx.getSource().sendFailure(Component.literal(
                        "ROM file not found: " + fileName + "\n" +
                        "Place .gba files in: <world>/roms/" + fileName));
                return 0;
            }

            byte[] romData = Files.readAllBytes(romPath);
            Cartridge cart = new Cartridge();
            if (!cart.loadROM(romData)) {
                ctx.getSource().sendFailure(Component.literal("Invalid GBA ROM file."));
                return 0;
            }

            // Find cartridge in player's inventory, or create new one
            ItemStack cartStack = findOrCreateCartridge(player);
            GBACartridgeItem.setROM(cartStack, romData, cart.getTitle(), cart.getGameCode());

            if (!player.getInventory().contains(cartStack)) {
                player.getInventory().add(cartStack);
            }

            ctx.getSource().sendSuccess(() ->
                    Component.literal("Loaded ROM: " + cart.getTitle() +
                            " [" + cart.getGameCode() + "] " +
                            (romData.length / 1024) + "KB  →  Right-click GBA Console to play!"), false);
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error loading ROM: " + e.getMessage()));
            return 0;
        }
    }

    private static ItemStack findOrCreateCartridge(Player player) {
        // Check if player is holding a cartridge
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GBACartridgeItem) return held;

        // Check inventory for empty cartridge
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GBACartridgeItem && !GBACartridgeItem.hasROM(stack)) {
                return stack;
            }
        }

        // Create new cartridge
        return new ItemStack(ModItems.GBA_CARTRIDGE.get());
    }

    private static int cartridgeInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack held = player.getMainHandItem();

            if (!(held.getItem() instanceof GBACartridgeItem)) {
                ctx.getSource().sendFailure(Component.literal("Hold a GBA Cartridge to see its info."));
                return 0;
            }

            if (!GBACartridgeItem.hasROM(held)) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("Empty cartridge — use /gba load <file.gba>"), false);
                return 1;
            }

            String name     = GBACartridgeItem.getRomName(held);
            String code     = GBACartridgeItem.getGameCode(held);
            int    size     = GBACartridgeItem.getRomSize(held);

            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("GBA Cartridge: %s | Code: %s | Size: %dKB",
                            name, code, size / 1024)), false);
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Must be a player."));
            return 0;
        }
    }
}
