package com.fshop.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/** A shop owned and stocked by a single player. */
public final class PlayerShop {
   private final UUID id;
   private UUID owner;
   private String ownerName;
   private String name;
   private final List<ShopOffer> offers = new ArrayList<>();
   private final long[] pendingEarnings = new long[3]; // per coin type (bronze/silver/gold)

   public PlayerShop(UUID id, UUID owner, String ownerName, String name) {
      this.id = id;
      this.owner = owner;
      this.ownerName = ownerName == null ? "" : ownerName;
      this.name = name == null ? "" : name;
   }

   public UUID getId() {
      return this.id;
   }

   public UUID getOwner() {
      return this.owner;
   }

   public void setOwner(UUID owner) {
      this.owner = owner;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public void setOwnerName(String ownerName) {
      this.ownerName = ownerName == null ? "" : ownerName;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name == null ? "" : name;
   }

   public List<ShopOffer> getOffers() {
      return this.offers;
   }

   public long getPendingEarnings(int coin) {
      return this.pendingEarnings[Math.max(0, Math.min(2, coin))];
   }

   public long totalPendingEarnings() {
      return this.pendingEarnings[0] + this.pendingEarnings[1] + this.pendingEarnings[2];
   }

   public void addEarnings(int coin, long amount) {
      this.pendingEarnings[Math.max(0, Math.min(2, coin))] += Math.max(0L, amount);
   }

   public void clearEarnings() {
      this.pendingEarnings[0] = 0L;
      this.pendingEarnings[1] = 0L;
      this.pendingEarnings[2] = 0L;
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putUUID("id", this.id);
      tag.putUUID("owner", this.owner);
      tag.putString("ownerName", this.ownerName);
      tag.putString("name", this.name);
      tag.putLongArray("earnings3", this.pendingEarnings);
      ListTag list = new ListTag();
      for (ShopOffer offer : this.offers) {
         list.add(offer.toNbt());
      }
      tag.put("offers", list);
      return tag;
   }

   public static PlayerShop fromNbt(CompoundTag tag) {
      PlayerShop shop = new PlayerShop(tag.getUUID("id"), tag.getUUID("owner"),
            tag.getString("ownerName"), tag.getString("name"));
      long[] e = tag.getLongArray("earnings3");
      for (int i = 0; i < 3 && i < e.length; i++) {
         shop.pendingEarnings[i] = e[i];
      }
      ListTag list = tag.getList("offers", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         shop.offers.add(ShopOffer.fromNbt(list.getCompound(i)));
      }
      return shop;
   }

   /** Full serialization used to push a shop to a client screen. */
   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUUID(this.id);
      buf.writeUUID(this.owner);
      buf.writeUtf(this.ownerName);
      buf.writeUtf(this.name);
      buf.writeVarLong(this.pendingEarnings[0]);
      buf.writeVarLong(this.pendingEarnings[1]);
      buf.writeVarLong(this.pendingEarnings[2]);
      buf.writeVarInt(this.offers.size());
      for (ShopOffer offer : this.offers) {
         offer.toBuf(buf);
      }
   }

   public static PlayerShop fromBuf(FriendlyByteBuf buf) {
      PlayerShop shop = new PlayerShop(buf.readUUID(), buf.readUUID(), buf.readUtf(), buf.readUtf());
      shop.pendingEarnings[0] = buf.readVarLong();
      shop.pendingEarnings[1] = buf.readVarLong();
      shop.pendingEarnings[2] = buf.readVarLong();
      int n = buf.readVarInt();
      for (int i = 0; i < n; i++) {
         shop.offers.add(ShopOffer.fromBuf(buf));
      }
      return shop;
   }
}
