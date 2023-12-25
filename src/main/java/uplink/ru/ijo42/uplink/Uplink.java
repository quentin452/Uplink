package ru.ijo42.uplink;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLFingerprintViolationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import ru.ijo42.uplink.api.ForgeAPI;
import ru.ijo42.uplink.api.PresenceListener;
import ru.ijo42.uplink.api.UplinkAPI;

@Mod(
   modid = "uplink",
   name = "Uplink",
   version = "1.2.3",
   certificateFingerprint = "ae2668515138eceb53d9e8c984322de3c34f9e21"
)
public class Uplink {
   @EventHandler
   public void preInit(final FMLPreInitializationEvent event) {
      PresenceListenerImpl listener = new PresenceListenerImpl();
      UplinkAPI.init(new ForgeAPI() {
         public int getModsCount() {
            return Loader.instance().getModList().size();
         }

         public int getPlayerCount() {
            return Minecraft.getMinecraft().getNetHandler().playerInfoList.size();
         }

         public int getMaxPlayers() {
            return Minecraft.getMinecraft().getNetHandler().currentServerMaxPlayers;
         }

         public String getIGN() {
            return Minecraft.getMinecraft().getSession().getUsername();
         }

         public Path getConfigDir() {
            return event.getModConfigurationDirectory().toPath();
         }

         public boolean isMP() {
            return Minecraft.getMinecraft().func_147104_D() != null;
         }

         public String getServerIP() {
            return Minecraft.getMinecraft().func_147104_D().serverIP;
         }

         public String getWorldName() {
            return Minecraft.getMinecraft().getIntegratedServer().getWorldName();
         }

         public void afterInit(PresenceListener listener) {
            MinecraftForge.EVENT_BUS.register(listener);
         }
      }, listener);
   }

   @EventHandler
   public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
      System.err.println("Invalid fingerprint detected! The file " + event.source.getName() + " may have been tampered with. This version will NOT be supported by the author!");
   }
}
