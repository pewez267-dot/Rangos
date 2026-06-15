package com.pewez.fantasticshortcuts.mixin;

import com.pewez.fantasticshortcuts.brigadier.ReplacedCommands;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin sobre {@link net.minecraft.commands.Commands}.
 *
 * <p>{@code Commands.fillUsableCommands} es el método que construye, de forma recursiva, el árbol de
 * comandos que el servidor ENVÍA al cliente (lo que el jugador ve al pulsar {@code /} y en el TAB).
 * Para cada nodo se llama a {@code CommandNode.canUse(source)}.
 *
 * <p>Redirigimos esa llamada: si el nodo es un literal raíz marcado como "reemplazado" por un atajo
 * con {@code replaceOriginal=true}, devolvemos {@code false} para que NO se incluya en el árbol del
 * cliente. Así {@code /gamemode} desaparece del TAB y solo se ve {@code /gc}.
 *
 * <p>Clave: esto SOLO afecta al árbol enviado al cliente. El parseo/ejecución del dispatcher en el
 * servidor no pasa por {@code fillUsableCommands}, de modo que el comando real sigue funcionando con
 * normalidad y el atajo no se rompe. Tampoco se conceden ni alteran permisos: si el jugador ya no
 * podía usar el comando ({@code canUse} original = false), se respeta ese {@code false}.
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
    private boolean fantasticshortcuts$hideReplaced(CommandNode<CommandSourceStack> node, Object source) {
        final boolean originalCanUse = node.canUse((CommandSourceStack) source);
        if (!originalCanUse) {
            // Nunca elevamos: si vanilla ya lo ocultaba, sigue oculto.
            return false;
        }
        if (node instanceof LiteralCommandNode<?> literal && ReplacedCommands.isHidden(literal.getLiteral())) {
            // El comando original está reemplazado por un atajo: ocúltalo del TAB del cliente.
            return false;
        }
        return true;
    }
}
