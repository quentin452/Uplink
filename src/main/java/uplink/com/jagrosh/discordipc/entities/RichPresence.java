package com.jagrosh.discordipc.entities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RichPresence {
   private final String state;
   private final String details;
   private final long startTimestamp;
   private final long endTimestamp;
   private final String largeImageKey;
   private final String largeImageText;
   private final String smallImageKey;
   private final String smallImageText;
   private final String partyId;
   private final int partySize;
   private final int partyMax;
   private final String matchSecret;
   private final String joinSecret;
   private final String spectateSecret;
   private final boolean instance;

   public RichPresence(String state, String details, long startTimestamp, long endTimestamp, String largeImageKey, String largeImageText, String smallImageKey, String smallImageText, String partyId, int partySize, int partyMax, String matchSecret, String joinSecret, String spectateSecret, boolean instance) {
      this.state = state;
      this.details = details;
      this.startTimestamp = startTimestamp;
      this.endTimestamp = endTimestamp;
      this.largeImageKey = largeImageKey;
      this.largeImageText = largeImageText;
      this.smallImageKey = smallImageKey;
      this.smallImageText = smallImageText;
      this.partyId = partyId;
      this.partySize = partySize;
      this.partyMax = partyMax;
      this.matchSecret = matchSecret;
      this.joinSecret = joinSecret;
      this.spectateSecret = spectateSecret;
      this.instance = instance;
   }

   public JsonObject toJson() {
      JsonObject timestamps = new JsonObject();
      JsonObject assets = new JsonObject();
      JsonObject party = new JsonObject();
      JsonObject secrets = new JsonObject();
      JsonObject finalObject = new JsonObject();
      if (this.startTimestamp > 0L) {
         timestamps.addProperty("start", this.startTimestamp);
         if (this.endTimestamp > this.startTimestamp) {
            timestamps.addProperty("end", this.endTimestamp);
         }
      }

      if (this.largeImageKey != null && !this.largeImageKey.isEmpty()) {
         assets.addProperty("large_image", this.largeImageKey);
         if (this.largeImageText != null && !this.largeImageText.isEmpty()) {
            assets.addProperty("large_text", this.largeImageText);
         }
      }

      if (this.smallImageKey != null && !this.smallImageKey.isEmpty()) {
         assets.addProperty("small_image", this.smallImageKey);
         if (this.smallImageText != null && !this.smallImageText.isEmpty()) {
            assets.addProperty("small_text", this.smallImageText);
         }
      }

      if (this.partyId != null) {
         party.addProperty("id", this.partyId);
         JsonArray partyData = new JsonArray();
         if (this.partySize > 0) {
            partyData.add(new JsonPrimitive(this.partySize));
            if (this.partyMax >= this.partySize) {
               partyData.add(new JsonPrimitive(this.partyMax));
            }
         }

         party.add("size", partyData);
      }

      if (this.joinSecret != null && !this.joinSecret.isEmpty()) {
         secrets.addProperty("join", this.joinSecret);
      }

      if (this.spectateSecret != null && !this.spectateSecret.isEmpty()) {
         secrets.addProperty("spectate", this.spectateSecret);
      }

      if (this.matchSecret != null && !this.matchSecret.isEmpty()) {
         secrets.addProperty("match", this.matchSecret);
      }

      if (this.state != null && !this.state.isEmpty()) {
         finalObject.addProperty("state", this.state);
      }

      if (this.details != null && !this.details.isEmpty()) {
         finalObject.addProperty("details", this.details);
      }

      if (timestamps.has("start")) {
         finalObject.add("timestamps", timestamps);
      }

      if (assets.has("large_image")) {
         finalObject.add("assets", assets);
      }

      if (party.has("id")) {
         finalObject.add("party", party);
      }

      if (secrets.has("join") || secrets.has("spectate") || secrets.has("match")) {
         finalObject.add("secrets", secrets);
      }

      finalObject.addProperty("instance", this.instance);
      return finalObject;
   }

   public static class Builder {
      private String state;
      private String details;
      private long startTimestamp;
      private long endTimestamp;
      private String largeImageKey;
      private String largeImageText;
      private String smallImageKey;
      private String smallImageText;
      private String partyId;
      private int partySize;
      private int partyMax;
      private String matchSecret;
      private String joinSecret;
      private String spectateSecret;
      private boolean instance;

      public RichPresence build() {
         return new RichPresence(this.state, this.details, this.startTimestamp, this.endTimestamp, this.largeImageKey, this.largeImageText, this.smallImageKey, this.smallImageText, this.partyId, this.partySize, this.partyMax, this.matchSecret, this.joinSecret, this.spectateSecret, this.instance);
      }

      public RichPresence.Builder setState(String state) {
         this.state = state;
         return this;
      }

      public RichPresence.Builder setDetails(String details) {
         this.details = details;
         return this;
      }

      public RichPresence.Builder setStartTimestamp(long startTimestamp) {
         this.startTimestamp = startTimestamp;
         return this;
      }

      public RichPresence.Builder setEndTimestamp(long endTimestamp) {
         this.endTimestamp = endTimestamp;
         return this;
      }

      public RichPresence.Builder setLargeImage(String largeImageKey, String largeImageText) {
         this.largeImageKey = largeImageKey;
         this.largeImageText = largeImageText;
         return this;
      }

      public RichPresence.Builder setLargeImage(String largeImageKey) {
         return this.setLargeImage(largeImageKey, (String)null);
      }

      public RichPresence.Builder setSmallImage(String smallImageKey, String smallImageText) {
         this.smallImageKey = smallImageKey;
         this.smallImageText = smallImageText;
         return this;
      }

      public RichPresence.Builder setSmallImage(String smallImageKey) {
         return this.setSmallImage(smallImageKey, (String)null);
      }

      public RichPresence.Builder setParty(String partyId, int partySize, int partyMax) {
         this.partyId = partyId;
         this.partySize = partySize;
         this.partyMax = partyMax;
         return this;
      }

      public RichPresence.Builder setMatchSecret(String matchSecret) {
         this.matchSecret = matchSecret;
         return this;
      }

      public RichPresence.Builder setJoinSecret(String joinSecret) {
         this.joinSecret = joinSecret;
         return this;
      }

      public RichPresence.Builder setSpectateSecret(String spectateSecret) {
         this.spectateSecret = spectateSecret;
         return this;
      }

      public RichPresence.Builder setInstance(boolean instance) {
         this.instance = instance;
         return this;
      }
   }
}
