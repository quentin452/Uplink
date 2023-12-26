package ru.ijo42.uplink.api.config.display;

public class SmallDisplay {
   private String uid;
   private String key;
   private String name;

   public String getUid() {
      return this.uid;
   }

   public void setUid(String uid) {
      this.uid = uid;
   }

   public String getKey() {
      return this.key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public SmallDisplay self() {
      return this;
   }

   public String toString() {
      return "SmallDisplay{uid='" + this.uid + '\'' + ", key='" + this.key + '\'' + ", name='" + this.name + '\'' + '}';
   }
}
