package com.fantasticpass.quest;

/**
 * The kinds of objectives a quest can track. Each maps to a server-side event
 * hook and to a translation key used to describe it on the client.
 */
public enum QuestType {
   BREAK_BLOCKS("break_blocks", 1),
   MINE_ORES("mine_ores", 2),
   KILL_MONSTERS("kill_monsters", 3),
   KILL_ANIMALS("kill_animals", 4),
   KILL_PLAYERS("kill_players", 5),
   CATCH_FISH("catch_fish", 6),
   PLAY_MINUTES("play_minutes", 7);

   private final String id;
   private final int icon;

   QuestType(String id, int icon) {
      this.id = id;
      this.icon = icon;
   }

   public String getId() {
      return this.id;
   }

   /** Default bp_icons index used to represent this quest type. */
   public int getIcon() {
      return this.icon;
   }

   public String descriptionKey() {
      return "fantasticpass.quest." + this.id;
   }

   public static QuestType byName(String name) {
      try {
         return valueOf(name);
      } catch (IllegalArgumentException e) {
         return BREAK_BLOCKS;
      }
   }
}
