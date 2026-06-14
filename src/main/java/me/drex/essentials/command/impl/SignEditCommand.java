package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SignEditCommand {

    private SignEditCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("signedit")
                .requires(Permissions.require("essentials.command.signedit", 2))
                .then(Commands.argument("line", IntegerArgumentType.integer(1, 4))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(SignEditCommand::setLine))
                        .executes(SignEditCommand::clearLine)));
    }

    private static SignBlockEntity targetSign(ServerPlayer player) {
        HitResult hit = player.pick(6.0, 1.0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof SignBlockEntity sign ? sign : null;
    }

    private static int setLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int line = IntegerArgumentType.getInteger(context, "line");
        String text = StringArgumentType.getString(context, "text");
        return apply(player, line, Messages.LegacyText.parse(text));
    }

    private static int clearLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int line = IntegerArgumentType.getInteger(context, "line");
        return apply(player, line, Component.empty());
    }

    private static int apply(ServerPlayer player, int line, Component value) {
        SignBlockEntity sign = targetSign(player);
        if (sign == null) {
            player.sendSystemMessage(Messages.prefixed("signedit.nosign",
                    "&cYou must be looking at a sign within 6 blocks."));
            return 0;
        }
        SignText signText = sign.getFrontText();
        signText = signText.setMessage(line - 1, value);
        sign.setText(signText, true);
        sign.setChanged();
        BlockState state = player.level().getBlockState(sign.getBlockPos());
        ((ServerLevel) player.level()).sendBlockUpdated(sign.getBlockPos(), state, state, 3);
        player.sendSystemMessage(Messages.prefixed("signedit.updated",
                "&aSign line {line} updated.", Messages.of("line", String.valueOf(line))));
        return 1;
    }
}
