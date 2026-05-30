package com.theplumteam.figure;

import java.util.List;

public class BuiltInCollections {
   public static final List<String> COLLECTION_IDS = List.of(
      "jojos",
      "jujutsukaisen",
      "adventuretime",
      "supermario",
      "starwars",
      "fnaf",
      "onepiece",
      "deltarune",
      "alienstage",
      "strangerthings",
      "dispatch",
      "ultrakill",
      "doom",
      "theamazingdigitalcircus",
      "dragonballz",
      "winx"
   );

   public static String getDisplayName(String collectionId) {
      return switch (collectionId) {
         case "jojos" -> "JoJos";
         case "jujutsukaisen" -> "Jujutsu Kaisen";
         case "adventuretime" -> "Adventure Time";
         case "supermario" -> "Super Mario";
         case "starwars" -> "Star Wars";
         case "fnaf" -> "FNAF";
         case "onepiece" -> "One Piece";
         case "deltarune" -> "Deltarune";
         case "alienstage" -> "Alien Stage";
         case "strangerthings" -> "Stranger Things";
         case "dispatch" -> "Dispatch";
         case "ultrakill" -> "ULTRAKILL";
         case "doom" -> "DOOM";
         case "theamazingdigitalcircus" -> "The Amazing Digital Circus";
         case "dragonballz" -> "Dragon Ball Z";
         case "winx" -> "Winx Club";
         default -> collectionId;
      };
   }
}
