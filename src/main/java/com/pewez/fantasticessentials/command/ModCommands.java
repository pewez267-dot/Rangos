package com.pewez.fantasticessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pewez.fantasticessentials.command.impl.AdminCommands;
import com.pewez.fantasticessentials.command.impl.BackCommand;
import com.pewez.fantasticessentials.command.impl.HomeCommands;
import com.pewez.fantasticessentials.command.impl.ItemEditCommand;
import com.pewez.fantasticessentials.command.impl.MenuCommands;
import com.pewez.fantasticessentials.command.impl.MiscCommands;
import com.pewez.fantasticessentials.command.impl.SignEditCommand;
import com.pewez.fantasticessentials.command.impl.TpaCommands;
import com.pewez.fantasticessentials.command.impl.WarpCommands;
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
