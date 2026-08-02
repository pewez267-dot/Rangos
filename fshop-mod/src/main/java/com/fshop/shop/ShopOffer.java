package com.fshop.shop;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * A single item entry in a player shop. The price is expressed as a whole
 * number of a chosen coin type (0=bronze, 1=silver, 2=gold) with NO conversion
 * between coin types, matching how FantasticCoins/Athens Coins actually work.
 */
public final class ShopOffer {
   private ItemStack item;
   private long unitPrice;
   private int coin;
   private int stock;
   /** Server/main-shop offers can have unlimited stock (never depletes). */
   private boolean infinite;
   /** Items delivered per purchase unit; the price is per bundle. Default 1. */
   private int bundle = 1;

   public ShopOffer(ItemStack item, long unitPrice, int coin, int stock) {
      this.item = item.copy();
      this.item.setCount(1);
      this.unitPrice = Math.max(0L, unitPrice);
      this.coin = Math.max(0, Math.min(2, coin));
      this.stock = Math.max(0, stock);
   }

   public boolean isInfinite() {
      return this.infinite;
   }

   public void setInfinite(boolean infinite) {
      this.infinite = infinite;
   }

   public int getBundle() {
      return this.bundle;
   }

   public void setBundle(int bundle) {
      this.bundle = Math.max(1, bundle);
   }

   /** True if this offer can satisfy {@code amount} (infinite always can). */
   public boolean hasStock(int amount) {
      return this.infinite || this.stock >= amount;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public ItemStack displayStack(int count) {
      ItemStack s = this.item.copy();
      s.setCount(Math.max(1, Math.min(count, this.item.getMaxStackSize())));
      return s;
   }

   /**
    * Whether two stacks are the SAME product and may be merged/stacked into one
    * offer. They must be the same item, the same durability, and carry the same
    * NBT (enchantments, custom name, lore, attributes...). So two identical full
    * vanilla swords merge, two swords with the same enchantments and durability
    * merge, but swords with different enchantments, a custom name or different
    * durability stay as separate offers.
    *
    * <p>Durability is compared via {@link ItemStack#getDamageValue()} and the
    * volatile {@code Damage}/{@code RepairCost} keys are ignored in the tag
    * comparison, so a pristine sword (no tag) and a full sword that happens to
    * carry {@code {Damage:0}} still count as identical instead of splitting.
    */
   public static boolean matchesForMerge(ItemStack a, ItemStack b) {
      if (a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b)) {
         return false;
      }
      if (a.getDamageValue() != b.getDamageValue()) {
         return false;
      }
      return identityTag(a).equals(identityTag(b));
   }

   /** NBT keys that define what an item IS to the player (visible/gameplay identity). */
   private static final String[] IDENTITY_KEYS = {
      "Enchantments", "StoredEnchantments", "display", "Potion", "CustomPotionEffects",
      "CustomModelData", "Trim", "BlockEntityTag", "BlockStateTag", "Unbreakable",
      "AttributeModifiers", "EntityTag", "pages", "author", "title", "generation"
   };

   /**
    * Builds a tag containing only the player-meaningful identity of an item
    * (enchantments, name/lore, potion, texture/model, trim, attributes...),
    * deliberately ignoring incidental or mod-added NBT that does not change what
    * the item is. Two items with the same identity tag (and same item + same
    * durability) are treated as the same product and stack into one offer.
    */
   private static CompoundTag identityTag(ItemStack stack) {
      CompoundTag out = new CompoundTag();
      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return out;
      }
      for (String key : IDENTITY_KEYS) {
         if (tag.contains(key)) {
            out.put(key, tag.get(key).copy());
         }
      }
      return out;
   }

   /**
    * Folds every duplicate offer (same product per {@link #matchesForMerge}) in
    * {@code offers} into the first one, summing stock, keeping the earliest
    * offer's price/coin/bundle. Identical items therefore always show as one
    * entry.
    */
   public static void mergeDuplicates(List<ShopOffer> offers) {
      for (int i = 0; i < offers.size(); i++) {
         ShopOffer keep = offers.get(i);
         for (int j = offers.size() - 1; j > i; j--) {
            ShopOffer dup = offers.get(j);
            if (matchesForMerge(keep.getItem(), dup.getItem())) {
               if (!keep.isInfinite()) {
                  keep.addStock(dup.getStock());
               }
               offers.remove(j);
            }
         }
      }
   }

   /**
    * True for a pristine, untouched vanilla item: a Minecraft item with no
    * enchantments, no damage/use and no custom NBT (no rename, no attributes,
    * nothing). Enchanted, damaged, renamed or NBT-tagged items are never
    * pristine, and neither are modded items.
    */
   public static boolean isPristineVanilla(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      net.minecraft.resources.ResourceLocation id =
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (id == null || !"minecraft".equals(id.getNamespace())) {
         return false;
      }
      if (stack.isEnchanted() || stack.isDamaged()) {
         return false;
      }
      CompoundTag tag = stack.getTag();
      return tag == null || tag.isEmpty();
   }

   /**
    * The largest bundle (units delivered per payment) the seller may set. Only
    * unique/used gear (a non-stackable item that is enchanted, damaged or has
    * custom NBT) is locked to 1 so it is never merged into packs. Everything
    * else -- normal stackable items AND pristine vanilla tools/weapons/armor --
    * can be bundled freely well beyond one stack (e.g. sell 66 or 128 netherite
    * per payment); the seller's real stock is what limits how much can be sold.
    */
   public static int bundleCap(ItemStack stack) {
      if (stack.getMaxStackSize() <= 1 && !isPristineVanilla(stack)) {
         return 1;
      }
      return 9999;
   }

   /**
    * A convenient "one full stack" bundle for the Stack button: the item's own
    * max stack (64 for netherite, 16 for eggs...), or 64 for a pristine vanilla
    * item that vanilla treats as non-stackable, or 1 for unique/used gear.
    */
   public static int fullStack(ItemStack stack) {
      int vanillaMax = stack.getMaxStackSize();
      if (vanillaMax > 1) {
         return vanillaMax;
      }
      return isPristineVanilla(stack) ? 64 : 1;
   }

   public long getUnitPrice() {
      return this.unitPrice;
   }

   public void setUnitPrice(long unitPrice) {
      this.unitPrice = Math.max(0L, unitPrice);
   }

   public int getCoin() {
      return this.coin;
   }

   public void setCoin(int coin) {
      this.coin = Math.max(0, Math.min(2, coin));
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
      tag.putInt("coin", this.coin);
      tag.putInt("stock", this.stock);
      tag.putBoolean("inf", this.infinite);
      tag.putInt("bundle", this.bundle);
      return tag;
   }

   public static ShopOffer fromNbt(CompoundTag tag) {
      ShopOffer offer = new ShopOffer(ItemStack.of(tag.getCompound("item")),
            tag.getLong("price"), tag.getInt("coin"), tag.getInt("stock"));
      offer.infinite = tag.getBoolean("inf");
      offer.bundle = tag.contains("bundle") ? Math.max(1, tag.getInt("bundle")) : 1;
      return offer;
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeItem(this.item);
      buf.writeVarLong(this.unitPrice);
      buf.writeVarInt(this.coin);
      buf.writeVarInt(this.stock);
      buf.writeBoolean(this.infinite);
      buf.writeVarInt(this.bundle);
   }

   public static ShopOffer fromBuf(FriendlyByteBuf buf) {
      ShopOffer offer = new ShopOffer(buf.readItem(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
      offer.infinite = buf.readBoolean();
      offer.bundle = Math.max(1, buf.readVarInt());
      return offer;
   }
}
