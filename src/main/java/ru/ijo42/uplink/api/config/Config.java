package ru.ijo42.uplink.api.config;

public class Config {
   public String clientId;
   public String smallDataUid;
   public boolean hideUnknownIPs;
   public Config.DisplayUrls displayUrls = new Config.DisplayUrls();

   public static class DisplayUrls {
      public String server;
      public String small;
      public String gui;
   }
}
