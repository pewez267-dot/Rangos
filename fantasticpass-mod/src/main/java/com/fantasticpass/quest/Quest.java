package com.fantasticpass.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * An immutable quest definition: a tracked objective worth a number of pass
 * points when completed. The human description is built on the client from the
 * type and target so it can be localized.
 */
public final class Quest {
   private final String id;
   private final QuestType type;
   private final int target;
   private final int points;

   public Quest(String id, QuestType type, int target, int points) {
      this.id = id;
      this.type = type;
      this.target = Math.max(1, target);
      this.points = Math.max(0, points);
   }

   public String getId() {
      return this.id;
   }

   public QuestType getType() {
      return this.type;
   }

   public int getTarget() {
      return this.target;
   }

   public int getPoints() {
      return this.points;
   }

   public Component getDescription() {
      return Component.translatable(this.type.descriptionKey(), this.target);
   }

   public CompoundTag toNbt() {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", this.id);
      tag.putString("type", this.type.name());
      tag.putInt("target", this.target);
      tag.putInt("points", this.points);
      return tag;
   }

   public static Quest fromNbt(CompoundTag tag) {
      return new Quest(tag.getString("id"), QuestType.byName(tag.getString("type")), tag.getInt("target"), tag.getInt("points"));
   }

   public void toBuf(FriendlyByteBuf buf) {
      buf.writeUtf(this.id);
      buf.writeUtf(this.type.name());
      buf.writeVarInt(this.target);
      buf.writeVarInt(this.points);
   }

   public static Quest fromBuf(FriendlyByteBuf buf) {
      return new Quest(buf.readUtf(), QuestType.byName(buf.readUtf()), buf.readVarInt(), buf.readVarInt());
   }
}
