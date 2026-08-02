package com.fshop;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central permission gate for FShop.
 *
 * <p>Regular members are permission level 0: they can use every normal
 * {@code /fshop} command (create, buy, sell/edit, collect, balance, help) with
 * no restriction. Admin actions require operator level, so ONLY opped players
 * can run {@code /fshop admin ...} or trigger the admin-only network packets
 * (the packets re-check this server-side, so a modified client cannot bypass
 * the command tree).
 */
public final class Perms {
   /**
    * Operator permission level required for every FShop admin action. This is
    * the vanilla operator level: a regular member is level 0, while an opped
    * player reaches it (the server's op-permission-level defaults to 4), so
    * this cleanly means "must be OP".
    */
   public static final int OP_LEVEL = 2;

   private Perms() {
   }

   /** True when the command source is an operator (may use admin commands). */
   public static boolean isAdmin(CommandSourceStack src) {
      return src.hasPermission(OP_LEVEL);
   }

   /** True when the player is an operator (may trigger admin actions). */
   public static boolean isAdmin(ServerPlayer player) {
      return player != null && player.hasPermissions(OP_LEVEL);
   }
}
