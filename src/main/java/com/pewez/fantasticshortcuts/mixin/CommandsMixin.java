package com.pewez.fantasticshortcuts.mixin;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pewez.fantasticshortcuts.brigadier.ReplaceRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hides "replaced" original commands from the client command tree.
 *
 * {@link Commands#fillUsableCommands} is what builds the per-player command packet (tab completion /
 * suggestions). By redirecting the {@code canUse} check for each child node, we can pretend that a
 * replaced original command (e.g. {@code gamemode}) is not usable, so it never appears on the client.
 *
 * Crucially this does NOT affect command execution: the dispatcher checks {@code canUse} directly
 * when parsing/executing, not through {@code fillUsableCommands}. So the original command still runs
 * (which is what the shortcut forwards to), it is simply invisible in tab completion.
 */
@Mixin(Commands.class)
public class CommandsMixin {

    @Redirect(
            method = "fillUsableCommands",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"
            )
    )
    private boolean fantasticshortcuts$hideReplacedCommands(CommandNode<CommandSourceStack> node, Object source) {
        if (!node.canUse((CommandSourceStack) source)) {
            return false;
        }
        if (node instanceof LiteralCommandNode<?> literal && ReplaceRegistry.isHidden(literal.getLiteral())) {
            return false;
        }
        return true;
    }
}
