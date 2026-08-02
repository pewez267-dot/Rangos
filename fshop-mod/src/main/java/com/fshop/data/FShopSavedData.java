package com.fshop.data;

import com.fshop.shop.PlayerShop;
import com.fshop.zone.MarketZone;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Central persistence for all market zones and player shops. */
public final class FShopSavedData extends SavedData {
   public static final String DATA_NAME = "fshop_data";
   /** Fixed singleton id for the main server shop ("La Moneda de Oro"). */
   public static final UUID MAIN_SHOP_ID = new UUID(0xF5A00A11F5A00A11L, 0x0DED0DED0DED0DEDL);

   private final Map<String, MarketZone> zones = new LinkedHashMap<>();
   private final Map<UUID, PlayerShop> shops = new LinkedHashMap<>();

   public static FShopSavedData get(MinecraftServer server) {
      ServerLevel overworld = server.overworld();
      DimensionDataStorage storage = overworld.getDataStorage();
      return storage.computeIfAbsent(FShopSavedData::load, FShopSavedData::new, DATA_NAME);
   }

   public static FShopSavedData get(ServerLevel level) {
      return get(level.getServer());
   }

   // Zones ------------------------------------------------------------------
   public Map<String, MarketZone> getZones() {
      return this.zones;
   }

   public void putZone(MarketZone zone) {
      this.zones.put(zone.getName().toLowerCase(), zone);
      this.setDirty();
   }

   public boolean removeZone(String name) {
      boolean removed = this.zones.remove(name.toLowerCase()) != null;
      if (removed) {
         this.setDirty();
      }
      return removed;
   }

   public boolean isInsideAnyZone(ResourceKey<Level> dim, double x, double y, double z) {
      for (MarketZone zone : this.zones.values()) {
         if (zone.contains(dim, x, y, z)) {
            return true;
         }
      }
      return false;
   }

   public boolean isInsideAnyZone(Entity entity) {
      return isInsideAnyZone(entity.level().dimension(), entity.getX(), entity.getY(), entity.getZ());
   }

   // Shops ------------------------------------------------------------------
   public Map<UUID, PlayerShop> getShops() {
      return this.shops;
   }

   @Nullable
   public PlayerShop getShop(UUID id) {
      return this.shops.get(id);
   }

   @Nullable
   public PlayerShop getMainShop() {
      return this.shops.get(MAIN_SHOP_ID);
   }

   public List<PlayerShop> getShopsByOwner(UUID owner) {
      List<PlayerShop> result = new ArrayList<>();
      for (PlayerShop shop : this.shops.values()) {
         if (shop.getOwner().equals(owner)) {
            result.add(shop);
         }
      }
      return result;
   }

   public void putShop(PlayerShop shop) {
      this.shops.put(shop.getId(), shop);
      this.setDirty();
   }

   public boolean removeShop(UUID id) {
      boolean removed = this.shops.remove(id) != null;
      if (removed) {
         this.setDirty();
      }
      return removed;
   }

   public int removeShopsByOwner(UUID owner) {
      int count = 0;
      var it = this.shops.values().iterator();
      while (it.hasNext()) {
         if (it.next().getOwner().equals(owner)) {
            it.remove();
            count++;
         }
      }
      if (count > 0) {
         this.setDirty();
      }
      return count;
   }

   // Persistence ------------------------------------------------------------
   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag zoneList = new ListTag();
      for (MarketZone zone : this.zones.values()) {
         zoneList.add(zone.toNbt());
      }
      tag.put("zones", zoneList);

      ListTag shopList = new ListTag();
      for (PlayerShop shop : this.shops.values()) {
         shopList.add(shop.toNbt());
      }
      tag.put("shops", shopList);
      return tag;
   }

   public static FShopSavedData load(CompoundTag tag) {
      FShopSavedData data = new FShopSavedData();
      ListTag zoneList = tag.getList("zones", Tag.TAG_COMPOUND);
      for (int i = 0; i < zoneList.size(); i++) {
         MarketZone zone = MarketZone.fromNbt(zoneList.getCompound(i));
         data.zones.put(zone.getName().toLowerCase(), zone);
      }
      ListTag shopList = tag.getList("shops", Tag.TAG_COMPOUND);
      for (int i = 0; i < shopList.size(); i++) {
         PlayerShop shop = PlayerShop.fromNbt(shopList.getCompound(i));
         data.shops.put(shop.getId(), shop);
      }
      return data;
   }
}
