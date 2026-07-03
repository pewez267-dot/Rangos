package com.fshop.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * A single item entry inside a player shop. The item template is stored as a
 * single-count stack (preserving NBT); {@code unitPrice} is the price in coins
 * per one item and {@code stock} is how many individual items are available.
 */
public final class ShopOffer {
   private ItemStack item;
   private long unitPrice;
   private int stock;

   public ShopOffer(ItemStack item, long unitPrice, int stock) {
      this.item = item.copy();
      this.item.setCount(1);
      this.unitPrice = Math.max(0L, unitPrice);
      this.stock = Math.max(0, stock);
   }

   public ItemStack getItem() {
      return this.item;
   }

   /** A display copy with the given count (never mutates the template). */
   public ItemStack displayStack(int count) {
      ItemStack s = this.item.copy();
      s.setCount(Math.max(1, Math.min(count, this.item.getMaxStackSize())));
      return s;
   }

   public long getUnitPrice() {
      return this.unitPrice;
   }

   public void setUnitPrice(long unitPrice) {
      this.unitPrice = Math.max(0L, unitPrice);
   }

   public int getStock() {
      return this.stock;
   }

   public void setStock(int stock) {
      this.stock = Math.max(0, stock);
   }

   public void addStock(int amount) {
      this.stock = Math.max(0, this.stock + amount);
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.put("item", this.item.save(new CompoundTag()));
      tag.putLong("price", this.unitPrice);
      tag.putInt("stock", this.stock);
      return tag;
   }

   public static ShopOffer fromNbt(CompoundTag tag) {
      ItemStack item = ItemStack.of(tag.getCompound("item"));
      return new ShopOffer(item, tag.getLong("price"), tag.getInt("stock"));
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeItem(this.item);
      buf.writeVarLong(this.unitPrice);
      buf.writeVarInt(this.stock);
   }

   public static ShopOffer fromBuf(FriendlyByteBuf buf) {
      ItemStack item = buf.readItem();
      long price = buf.readVarLong();
      int stock = buf.readVarInt();
      return new ShopOffer(item, price, stock);
   }
}
