package com.claimblocks.gui;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entry-point used by commands and the claim block to open the menu.
 *
 * Also holds the "pending add-member" state: when a player clicks
 * "Add Member" inside the chest GUI, we close the screen, record their UUID
 * here, and wait for the next chat line which we interpret as the target name.
 */
public class ClaimMenuScreen {
    private static final Map<UUID, Claim> pendingAddMember = new HashMap<>();

    public static void open(ServerPlayerEntity player, Claim claim) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInv, p) -> new ClaimMenuScreenHandler(syncId, playerInv, claim),
            Text.literal("Claim Menu - Tier " + claim.getTier())
        ));
    }

    public static void requestAddMember(ServerPlayerEntity player, Claim claim) {
        pendingAddMember.put(player.getUuid(), claim);
        player.sendMessage(Text.literal("§e[Claim] §fType the name of the player to add as a member, or §c'cancel'§f to cancel."), false);
    }

    /**
     * Registers the screen handler factory and the chat listener that backs
     * "Add Member".
     */
    public static void registerScreenHandler() {
        // The handler uses the vanilla GENERIC_9X6 type, no registration needed.

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID id = sender.getUuid();
            Claim claim = pendingAddMember.get(id);
            if (claim == null) return true;
            String content = message.getContent().getString().trim();
            pendingAddMember.remove(id);
            if (content.equalsIgnoreCase("cancel") || content.startsWith("/")) {
                sender.sendMessage(Text.literal("§7[Claim] Add-member cancelled."), false);
                return false;
            }
            PlayerManager pm = sender.getServer().getPlayerManager();
            ServerPlayerEntity target = pm.getPlayer(content);
            if (target == null) {
                sender.sendMessage(Text.literal("§c[Claim] No online player named '" + content + "'."), false);
                return false;
            }
            if (claim.isOwner(target.getUuid())) {
                sender.sendMessage(Text.literal("§c[Claim] That player is already the owner."), false);
                return false;
            }
            claim.addMember(target.getUuid(), target.getName().getString());
            ClaimManager.getInstance().markDirty();
            ClaimManager.getInstance().saveClaims(sender.getServer());
            sender.sendMessage(Text.literal("§a[Claim] Added §b" + target.getName().getString() + "§a as a member."), false);
            target.sendMessage(Text.literal("§a[Claim] You were added as a member of §b"
                + sender.getName().getString() + "§a's claim."), false);
            // Cancel the chat broadcast so the typed name isn't shown to others
            return false;
        });
    }
}
