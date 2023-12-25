package ru.ijo42.uplink.api;

import com.jagrosh.discordipc.IPCClient;

public abstract class PresenceListener {
   protected PresenceManager presenceManager;
   private IPCClient rpc;
   private int curTick = 0;
   private int curPlayerCount = 0;

   protected void init(IPCClient rpc, PresenceManager presenceManager) {
      this.rpc = rpc;
      this.presenceManager = presenceManager;
   }

   public void onTick() {
      if (this.presenceManager.getCurState() != PresenceState.INGAME) {
         this.curTick = 0;
      } else {
         if (this.curTick >= 1000) {
            this.curTick = 0;

            try {
               int playerCount = UplinkAPI.forgeImpl.getPlayerCount();
               int maxPlayers = UplinkAPI.forgeImpl.getMaxPlayers();
               if (this.curPlayerCount != playerCount) {
                  this.rpc.sendRichPresence(this.presenceManager.updatePlayerCount(UplinkAPI.forgeImpl.isMP() ? UplinkAPI.forgeImpl.getServerIP() : UplinkAPI.forgeImpl.getWorldName(), playerCount, maxPlayers));
                  this.curPlayerCount = playerCount;
               }
            } catch (NullPointerException var3) {
            }
         } else {
            ++this.curTick;
         }

      }
   }

   public void onMainMenu() {
      this.presenceManager.setCurState(PresenceState.MENU_MAIN);
      this.rpc.sendRichPresence(this.presenceManager.initMenu());
   }

   public void onJoin() {
      if (UplinkAPI.forgeImpl.isMP()) {
         if (this.presenceManager.getCurState() == PresenceState.INGAME) {
            return;
         }

         this.rpc.sendRichPresence(this.presenceManager.initMP(UplinkAPI.forgeImpl.getServerIP()));
      } else {
         this.rpc.sendRichPresence(this.presenceManager.initSP(UplinkAPI.forgeImpl.getWorldName()));
      }

      this.presenceManager.setCurState(PresenceState.INGAME);
   }

   public void onClientDisconnect() {
      this.rpc.sendRichPresence(this.presenceManager.initMenu());
      this.presenceManager.setCurState(PresenceState.MENU_MAIN);
   }
}
