package ru.ijo42.uplink.api.config.display;

public class ServerDisplay {
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

   public ServerDisplay self() {
      return this;
   }

   public String toString() {
      return "ServerDisplay{uid='" + this.uid + '\'' + ", key='" + this.key + '\'' + ", name='" + this.name + '\'' + '}';
   }
}
