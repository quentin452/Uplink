package ru.ijo42.uplink.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import ru.ijo42.uplink.api.config.Config;
import ru.ijo42.uplink.api.util.DisplayDataManager;
import ru.ijo42.uplink.api.util.MiscUtil;

public class UplinkAPI {
   public static ForgeAPI forgeImpl;
   private static IPCClient RPC;

   public static void init(ForgeAPI forgeImpl, PresenceListener presenceListener) {
      UplinkAPI.forgeImpl = forgeImpl;
      setupPresenceManager(forgeImpl.getConfigDir().resolve("Uplink.json"), presenceListener);
   }

   private static void setupPresenceManager(Path configPath, final PresenceListener presenceListener) {
      if (Files.notExists(configPath, new LinkOption[0])) {
         try {
            Files.copy(getResource("Uplink.json"), configPath, new CopyOption[0]);
         } catch (Exception var10) {
            System.err.println("[Uplink] Could not copy default config to " + configPath);
            System.err.println(var10.toString());
            return;
         }
      }

      Gson gson = (new GsonBuilder()).create();

      Config config;
      try {
         config = MiscUtil.verifyConfig((Config)gson.fromJson(Files.newBufferedReader(configPath), Config.class));
      } catch (Exception var9) {
         System.err.println("[Uplink] Could not load config");
         System.err.println(var9.toString());
         return;
      }

      DisplayDataManager dataManager = new DisplayDataManager(config, forgeImpl.getConfigDir().resolve("Uplink\\"));
      final PresenceManager manager = new PresenceManager(dataManager, config);
      RPC = new IPCClient(Long.parseLong(manager.getConfig().clientId));
      Thread callbackHandler = new Thread(() -> {
         while(!Thread.currentThread().isInterrupted()) {
            RPC.getStatus();

            try {
               Thread.sleep(2000L);
            } catch (InterruptedException var1) {
               RPC.close();
            }
         }

         RPC.close();
      }, "RPC-Callback-Handler");
      callbackHandler.start();
      Runtime var10000 = Runtime.getRuntime();
      callbackHandler.getClass();
      var10000.addShutdownHook(new Thread(callbackHandler::interrupt));

      try {
         RPC.setListener(new IPCListener() {
            public void onReady() {
               UplinkAPI.RPC.sendRichPresence(manager.initLoading());
               presenceListener.init(UplinkAPI.RPC, manager);
               UplinkAPI.forgeImpl.afterInit(presenceListener);
            }
         });
         RPC.connect();
      } catch (NoDiscordClientException var8) {
         System.err.println(var8.toString());
         var8.printStackTrace();
      }

   }

   public static InputStream getResource(String name) {
      return UplinkAPI.class.getResourceAsStream(name);
   }
}
