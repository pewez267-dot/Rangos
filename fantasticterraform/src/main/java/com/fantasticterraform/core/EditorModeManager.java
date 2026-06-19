package com.fantasticterraform.core;

import com.fantasticterraform.network.EditorStatePacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.SelectionUpdatePacket;
import com.fantasticterraform.registry.ModItems;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionWand;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la entrada y salida del modo editor server-side: guarda gamemode e
 * inventario, cambia a espectador, entrega la varita y restaura todo al salir.
 *
 * <p>Todas las comprobaciones de permisos OP se realizan en quien invoca (comando o
 * handler de packet). Aqui se asume que el llamador ya valido {@code hasPermission(4)},
 * pero {@link #enter(ServerPlayer)} es idempotente y seguro.</p>
 */
public final class EditorModeManager {

    private static final EditorModeManager INSTANCE = new EditorModeManager();

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private EditorModeManager() {
    }

    public static EditorModeManager get() {
        return INSTANCE;
    }

    public boolean isEditor(ServerPlayer player) {
        return sessions.containsKey(player.getUUID());
    }

    /**
     * Entra al modo editor. Devuelve false (con mensaje al jugador) si ya estaba dentro.
     */
    public boolean enter(ServerPlayer player) {
        if (sessions.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("\u00a7cYa estas en el modo editor de Fantastic Terraform."));
            return false;
        }

        GameType previous = player.gameMode.getGameModeForPlayer();
        ListTag savedInventory = player.getInventory().save(new ListTag());

        sessions.put(player.getUUID(), new Session(previous, savedInventory));

        player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);

        ItemStack wand = SelectionWand.tagged(new ItemStack(ModItems.SELECTION_WAND.get()));
        player.getInventory().setItem(0, wand);
        player.getInventory().selected = 0;

        SelectionManager.get(player).clear();

        PacketHandler.sendToClient(player, new EditorStatePacket(true));
        PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(SelectionManager.get(player)));

        player.sendSystemMessage(Component.literal(
                "\u00a7d\u2726 \u00a7fFantastic Terraform \u00a7d\u2726 \u00a77modo editor \u00a7aACTIVADO\u00a77."));
        player.sendSystemMessage(Component.literal(
                "\u00a77Usa el HUD para elegir herramientas. \u00a7e/fsterraform exit\u00a77 para salir."));
        return true;
    }

    /**
     * Sale del modo editor restaurando gamemode e inventario. Devuelve false (con
     * mensaje) si el jugador no estaba dentro.
     */
    public boolean exit(ServerPlayer player) {
        Session session = sessions.remove(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.literal("\u00a7cNo estas en el modo editor."));
            return false;
        }

        // Limpiar seleccion y wireframe.
        SelectionManager.remove(player);

        // Restaurar inventario y gamemode.
        player.getInventory().clearContent();
        player.getInventory().load(session.savedInventory);
        player.setGameMode(session.previousGameType);

        PacketHandler.sendToClient(player, new EditorStatePacket(false));

        player.sendSystemMessage(Component.literal(
                "\u00a7d\u2726 \u00a7fFantastic Terraform \u00a7d\u2726 \u00a77modo editor \u00a7cDESACTIVADO\u00a77."));
        return true;
    }

    /** Restauracion de emergencia (logout / shutdown) sin enviar packets al cliente. */
    public void forceRestore(ServerPlayer player) {
        Session session = sessions.remove(player.getUUID());
        if (session == null) {
            return;
        }
        SelectionManager.remove(player);
        player.getInventory().clearContent();
        player.getInventory().load(session.savedInventory);
        player.setGameMode(session.previousGameType);
    }

    private static final class Session {
        final GameType previousGameType;
        final ListTag savedInventory;

        Session(GameType previousGameType, ListTag savedInventory) {
            this.previousGameType = previousGameType;
            this.savedInventory = savedInventory;
        }
    }
}
