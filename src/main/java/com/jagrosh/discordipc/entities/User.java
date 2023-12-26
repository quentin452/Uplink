package com.jagrosh.discordipc.entities;

public class User {
   private final String name;
   private final String discriminator;
   private final long id;
   private final String avatar;

   public User(String name, String discriminator, long id, String avatar) {
      this.name = name;
      this.discriminator = discriminator;
      this.id = id;
      this.avatar = avatar;
   }

   public String getName() {
      return this.name;
   }

   public String getDiscriminator() {
      return this.discriminator;
   }

   public String getId() {
      return Long.toString(this.id);
   }

   public String getAvatarId() {
      return this.avatar;
   }

   public boolean equals(Object o) {
      if (!(o instanceof User)) {
         return false;
      } else {
         User oUser = (User)o;
         return this == oUser || this.id == oUser.id;
      }
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public String toString() {
      return "U:" + this.getName() + '(' + this.id + ')';
   }
}
