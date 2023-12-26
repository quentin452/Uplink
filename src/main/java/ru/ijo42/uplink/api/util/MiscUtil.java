package ru.ijo42.uplink.api.util;

import ru.ijo42.uplink.api.config.Config;

public class MiscUtil {
   public static long epochSecond() {
      return System.currentTimeMillis() / 1000L;
   }

   public static Config verifyConfig(Config config) {
      if (config.displayUrls.server == null) {
         config.displayUrls.server = "null";
      }

      if (config.displayUrls.small == null) {
         config.displayUrls.small = "null";
      }

      if (config.displayUrls.gui == null) {
         config.displayUrls.gui = "null";
      }

      return config;
   }
}
