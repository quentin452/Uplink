package ru.ijo42.uplink;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import ru.ijo42.uplink.api.PresenceListener;
import ru.ijo42.uplink.api.PresenceState;

public class PresenceListenerImpl extends PresenceListener {
   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      super.onTick();
   }

   @SubscribeEvent
   public void onMainMenu(GuiOpenEvent event) {
      if (event.gui instanceof GuiMainMenu && this.presenceManager.getCurState() != PresenceState.MENU_MAIN) {
         super.onMainMenu();
      }

   }

   @SubscribeEvent
   public void onJoin(EntityJoinWorldEvent event) {
      if (event.entity instanceof EntityPlayerMP || event.entity instanceof EntityPlayerSP) {
         super.onJoin();
      }

   }

   @SubscribeEvent
   public void onClientDisconnect(ClientDisconnectionFromServerEvent event) {
      super.onClientDisconnect();
   }
}
