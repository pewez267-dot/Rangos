package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.resources.ResourceLocation;

public class ModNetworking {
   public static final ResourceLocation FIGURE_POSITION = new ResourceLocation("blockpops", "figure_position");
   public static final ResourceLocation CLAW_MACHINE_COLLECTION = new ResourceLocation("blockpops", "claw_machine_collection");
   public static final ResourceLocation SET_FAVORITE_COLOR = new ResourceLocation("blockpops", "set_favorite_color");

   public static void init() {
      BlockPopsMod.logDebug("Initializing BlockPops networking...");
      // Client -> Server receivers (handled on the logical server)
      NetworkManager.registerReceiver(NetworkManager.c2s(), FIGURE_POSITION, FigurePositionPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), CLAW_MACHINE_COLLECTION, ClawMachineCollectionPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), SET_FAVORITE_COLOR, SetFavoriteColorPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), DropBoxPacket.ID, DropBoxPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), ReloadTokensPacket.ID, ReloadTokensPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), UnlockCollectionPacket.ID, UnlockCollectionPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), UpdateGuaranteedResetHourPacket.ID, UpdateGuaranteedResetHourPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), UpdateTokenSettingsPacket.ID, UpdateTokenSettingsPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), UpdateHiddenCollectionsPacket.ID, UpdateHiddenCollectionsPacket::handleServer);
      NetworkManager.registerReceiver(NetworkManager.c2s(), UpdateRemoteCollectionsPacket.ID, UpdateRemoteCollectionsPacket::handleServer);

      // Server -> Client receivers must only be registered on the physical client.
      if (Platform.getEnvironment() == Env.CLIENT) {
         registerClientReceivers();
         VersionCheckPacket.registerClient();
      } else {
         VersionCheckPacket.registerCommon();
      }

      BlockPopsMod.logDebug("BlockPops networking initialized");
   }

   private static void registerClientReceivers() {
      NetworkManager.registerReceiver(NetworkManager.s2c(), SyncTokenDataPacket.ID, SyncTokenDataPacket::handleClient);
      NetworkManager.registerReceiver(NetworkManager.s2c(), SyncDiscoveryDataPacket.ID, SyncDiscoveryDataPacket::handleClient);
      NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockFigurePacket.ID, UnlockFigurePacket::handleClient);
      NetworkManager.registerReceiver(NetworkManager.s2c(), SyncDynamicCollectionsPacket.ID, SyncDynamicCollectionsPacket::handleClient);
      NetworkManager.registerReceiver(NetworkManager.s2c(), OpenFavoriteColorScreenPacket.ID, OpenFavoriteColorScreenPacket::handleClient);
      NetworkManager.registerReceiver(NetworkManager.s2c(), SyncServerConfigPacket.ID, SyncServerConfigPacket::handleClient);
   }

   @Deprecated
   public static void initClient() {
      BlockPopsMod.logDebug("BlockPops client networking initialization (no-op, packets registered in init())");
   }
}
