package me.drex.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import me.drex.essentials.command.impl.AdminCommands;
import me.drex.essentials.command.impl.BackCommand;
import me.drex.essentials.command.impl.HomeCommands;
import me.drex.essentials.command.impl.ItemEditCommand;
import me.drex.essentials.command.impl.MenuCommands;
import me.drex.essentials.command.impl.MiscCommands;
import me.drex.essentials.command.impl.SignEditCommand;
import me.drex.essentials.command.impl.TpaCommands;
import me.drex.essentials.command.impl.WarpCommands;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registers every command provided by the mod.
 */
public final class ModCommands {

    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        HomeCommands.register(dispatcher);
        WarpCommands.register(dispatcher);
        BackCommand.register(dispatcher);
        TpaCommands.register(dispatcher);
        MenuCommands.register(dispatcher);
        MiscCommands.register(dispatcher);
        ItemEditCommand.register(dispatcher);
        SignEditCommand.register(dispatcher);
        AdminCommands.register(dispatcher);
    }
}
