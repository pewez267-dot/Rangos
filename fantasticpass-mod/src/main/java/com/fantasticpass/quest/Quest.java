package com.fantasticpass.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * An immutable quest definition: a tracked objective worth a number of pass
 * points when completed. Parameterized types ({@link QuestType.ParamKind}) also
 * carry a registry id ({@code param}) so a quest can target any entity, block
 * or item from vanilla OR any installed mod. The human description is built from
 * the type, target and param so it can be localized on the client.
 */
public final class Quest {
   private final String id;
   private final QuestType type;
   private final String param;
   private final int target;
   private final int points;

   public Quest(String id, QuestType type, int target, int points) {
      this(id, type, "", target, points);
   }

   public Quest(String id, QuestType type, String param, int target, int points) {
      this.id = id;
      this.type = type;
      this.param = param == null ? "" : param;
      this.target = Math.max(1, target);
      this.points = Math.max(0, points);
   }

   public String getId() {
      return this.id;
   }

   public QuestType getType() {
      return this.type;
   }

   public String getParam() {
      return this.param;
   }

   public int getTarget() {
      return this.target;
   }

   public int getPoints() {
      return this.points;
   }

   public Component getDescription() {
      if (this.param.isEmpty()) {
         return Component.translatable(this.type.descriptionKey(), this.target);
      }
      return Component.translatable(this.type.descriptionKey(), this.target, paramName(this.type, this.param));
   }

   /** Resolve a friendly, localized name for a parameterized target id. */
   public static Component paramName(QuestType type, String param) {
      ResourceLocation rl = ResourceLocation.tryParse(param);
      if (rl == null) {
         return Component.literal(param);
      }
      switch (type.getParamKind()) {
         case ENTITY:
            return BuiltInRegistries.ENTITY_TYPE.containsKey(rl)
               ? BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription()
               : Component.literal(param);
         case BLOCK:
            return BuiltInRegistries.BLOCK.containsKey(rl)
               ? BuiltInRegistries.BLOCK.get(rl).getName()
               : Component.literal(param);
         case ITEM:
            Item item = BuiltInRegistries.ITEM.get(rl);
            return item != null ? item.getDescription() : Component.literal(param);
         default:
            return Component.literal(param);
      }
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", this.id);
      tag.putString("type", this.type.name());
      tag.putString("param", this.param);
      tag.putInt("target", this.target);
      tag.putInt("points", this.points);
      return tag;
   }

   public static Quest fromNbt(CompoundTag tag) {
      return new Quest(
         tag.getString("id"),
         QuestType.byName(tag.getString("type")),
         tag.getString("param"),
         tag.getInt("target"),
         tag.getInt("points"));
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUtf(this.id);
      buf.writeUtf(this.type.name());
      buf.writeUtf(this.param);
      buf.writeVarInt(this.target);
      buf.writeVarInt(this.points);
   }

   public static Quest fromBuf(FriendlyByteBuf buf) {
      return new Quest(buf.readUtf(), QuestType.byName(buf.readUtf()), buf.readUtf(), buf.readVarInt(), buf.readVarInt());
   }
}
