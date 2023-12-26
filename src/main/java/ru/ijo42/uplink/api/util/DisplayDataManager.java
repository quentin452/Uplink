package ru.ijo42.uplink.api.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import ru.ijo42.uplink.api.config.Config;
import ru.ijo42.uplink.api.config.display.GUIDisplay;
import ru.ijo42.uplink.api.config.display.ServerDisplay;
import ru.ijo42.uplink.api.config.display.SmallDisplay;

public class DisplayDataManager {
   private final Map smallDisplays;
   private final Map serverDisplays;
   private GUIDisplay guiDisplay;

   public DisplayDataManager(Config config, Path configPath) {
      ServerDisplay[] serverArr = new ServerDisplay[0];
      SmallDisplay[] smallArr = new SmallDisplay[0];
      LoadingUtils.init(configPath);

      try {
         serverArr = LoadingUtils.load(new ServerDisplay(), config);
      } catch (MalformedURLException var10) {
         System.err.println("[Uplink->ServerDisplay] URL is broken => Using default");
      } catch (IOException var11) {
         System.err.println(var11.toString());
         System.err.println("[Uplink->ServerDisplay] Load from local File is not working => Using default");
      }

      try {
         smallArr = LoadingUtils.load(new SmallDisplay(), config);
      } catch (MalformedURLException var8) {
         System.err.println("[Uplink->SmallDisplay] URL is broken => Using default");
      } catch (IOException var9) {
         System.out.println(var9.toString());
         System.err.println("[Uplink->SmallDisplay] Load from local File is not working => Using default");
      }

      try {
         this.guiDisplay = LoadingUtils.load(new GUIDisplay(), config);
      } catch (MalformedURLException var6) {
         System.err.println("[Uplink->GUIDisplay] URL is broken => Using default");
      } catch (IOException var7) {
         System.out.println(var7.toString());
         System.err.println("[Uplink->GUIDisplay] Load from local File is not working => Using default");
      }

      if (smallArr == null) {
         smallArr = LoadingUtils.loadFromDefault(new SmallDisplay());
      }

      if (serverArr == null) {
         serverArr = LoadingUtils.loadFromDefault(new ServerDisplay());
      }

      if (this.guiDisplay == null) {
         this.guiDisplay = LoadingUtils.loadFromDefault(new GUIDisplay());
      }

      this.smallDisplays = (Map)Arrays.stream(smallArr).collect(Collectors.toMap(SmallDisplay::getUid, SmallDisplay::self));
      this.serverDisplays = (Map)Arrays.stream(serverArr).collect(Collectors.toMap(ServerDisplay::getUid, ServerDisplay::self));
      System.out.println("Loaded Small Data: " + this.smallDisplays.keySet());
      System.out.println("Loaded Servers: " + this.serverDisplays.keySet());
   }

   public Map getSmallDisplays() {
      return this.smallDisplays;
   }

   public Map getServerDisplays() {
      return this.serverDisplays;
   }

   public GUIDisplay getGUIDisplay() {
      return this.guiDisplay;
   }
}
