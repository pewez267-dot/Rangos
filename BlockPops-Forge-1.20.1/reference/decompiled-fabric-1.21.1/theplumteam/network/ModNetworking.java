package com.theplumteam.network;

import com.theplumteam.BlockPopsMod;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.class_2960;

public class ModNetworking {
   public static final class_2960 FIGURE_POSITION = class_2960.method_60655("blockpops", "figure_position");
   public static final class_2960 CLAW_MACHINE_COLLECTION = class_2960.method_60655("blockpops", "claw_machine_collection");
   public static final class_2960 SET_FAVORITE_COLOR = class_2960.method_60655("blockpops", "set_favorite_color");

   public static void init() {
      BlockPopsMod.logDebug("Initializing BlockPops networking...");
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
      if (Platform.getEnvironment() == Env.SERVER) {
         NetworkManager.registerS2CPayloadType(SyncTokenDataPacket.ID, null);
         NetworkManager.registerS2CPayloadType(SyncDiscoveryDataPacket.ID, null);
         NetworkManager.registerS2CPayloadType(UnlockFigurePacket.ID, null);
         NetworkManager.registerS2CPayloadType(SyncDynamicCollectionsPacket.ID, null);
         NetworkManager.registerS2CPayloadType(OpenFavoriteColorScreenPacket.ID, null);
         NetworkManager.registerS2CPayloadType(SyncServerConfigPacket.ID, null);
         VersionCheckPacket.registerCommon();
      } else {
         NetworkManager.registerReceiver(NetworkManager.s2c(), SyncTokenDataPacket.ID, SyncTokenDataPacket::handleClient);
         NetworkManager.registerReceiver(NetworkManager.s2c(), SyncDiscoveryDataPacket.ID, SyncDiscoveryDataPacket::handleClient);
         NetworkManager.registerReceiver(NetworkManager.s2c(), UnlockFigurePacket.ID, UnlockFigurePacket::handleClient);
         NetworkManager.registerReceiver(NetworkManager.s2c(), SyncDynamicCollectionsPacket.ID, SyncDynamicCollectionsPacket::handleClient);
         NetworkManager.registerReceiver(NetworkManager.s2c(), OpenFavoriteColorScreenPacket.ID, OpenFavoriteColorScreenPacket::handleClient);
         NetworkManager.registerReceiver(NetworkManager.s2c(), SyncServerConfigPacket.ID, SyncServerConfigPacket::handleClient);
         VersionCheckPacket.registerClient();
      }

      BlockPopsMod.logDebug("BlockPops networking initialized");
   }

   @Deprecated
   public static void initClient() {
      BlockPopsMod.logDebug("BlockPops client networking initialization (no-op, packets registered in init())");
   }
}
