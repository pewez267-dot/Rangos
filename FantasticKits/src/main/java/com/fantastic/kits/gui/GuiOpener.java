package com.fantastic.kits.gui;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.network.FKNetwork;
import com.fantastic.kits.network.OpenKitEditorPacket;
import com.fantastic.kits.network.OpenKitListPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Single server-side facade used by {@link com.fantastic.kits.commands.FKitsCommand}
 * to open Fantastic Kits screens on the client through the network channel.
 *
 * <p>The pattern is identical to FantasticCrates and FantasticSpawners: the
 * command builds the data payload server-side, sends it through
 * {@link FKNetwork}, and the client opens the corresponding {@code Screen}
 * subclass with everything it needs in one shot.
 */
public final class GuiOpener {

    /** Numeric mode to pass to {@link OpenKitListPacket}. */
    public enum ListMode {
        CREATE(0), EDIT(1), DELETE(2), TEST(3);
        public final int wire;
        ListMode(int wire) { this.wire = wire; }
    }

    private GuiOpener() {}

    /** Opens the kit list screen in the requested mode. */
    public static void openList(ServerPlayer player, ListMode mode) {
        if (FantasticKits.antiExploit().isGuiSpam(player.getUUID())) return;
        FKNetwork.sendToClient(player, new OpenKitListPacket(mode.wire,
                GuiPayload.allKits(), GuiPayload.groups(), GuiPayload.commandCatalogue()));
    }

    /** Opens the editor screen for a specific kit. */
    public static void openEditor(ServerPlayer player, Kit kit) {
        if (FantasticKits.antiExploit().isGuiSpam(player.getUUID())) return;
        FKNetwork.sendToClient(player, new OpenKitEditorPacket(
                kit.save(), GuiPayload.groups(), GuiPayload.commandCatalogue()));
    }
}
