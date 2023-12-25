package ru.ijo42.uplink.api;

import com.jagrosh.discordipc.entities.RichPresence;
import ru.ijo42.uplink.api.config.Config;
import ru.ijo42.uplink.api.config.display.ServerDisplay;
import ru.ijo42.uplink.api.config.display.SmallDisplay;
import ru.ijo42.uplink.api.util.DisplayDataManager;

public class PresenceManager {
   public static final long startTime = System.currentTimeMillis();
   private final DisplayDataManager dataManager;
   private final Config config;
   private final RichPresence.Builder loadingGame = new RichPresence.Builder();
   private final RichPresence.Builder mainMenu = new RichPresence.Builder();
   private final RichPresence.Builder inGame = new RichPresence.Builder();
   private PresenceState curState;

   public PresenceManager(DisplayDataManager dataManager, Config config) {
      this.curState = PresenceState.INIT;
      this.dataManager = dataManager;
      this.config = config;
      this.loadingGame.setState(dataManager.getGUIDisplay().loadingGame.state).setLargeImage("state-load", dataManager.getGUIDisplay().loadingGame.largeImageText);
      this.mainMenu.setState(dataManager.getGUIDisplay().mainMenu.state).setLargeImage("state-menu", dataManager.getGUIDisplay().mainMenu.largeImageText);
      SmallDisplay smallData = (SmallDisplay)dataManager.getSmallDisplays().get(this.config.smallDataUid);
      if (smallData != null) {
         this.loadingGame.setSmallImage(smallData.getKey(), smallData.getName());
         this.mainMenu.setSmallImage(smallData.getKey(), smallData.getName());
         this.inGame.setSmallImage(smallData.getKey(), smallData.getName());
      }
   }

   public PresenceState getCurState() {
      return this.curState;
   }

   public void setCurState(PresenceState curState) {
      this.curState = curState;
   }

   public DisplayDataManager getDataManager() {
      return this.dataManager;
   }

   public Config getConfig() {
      return this.config;
   }

   public RichPresence initLoading() {
      int mods = UplinkAPI.forgeImpl.getModsCount();
      return this.loadingGame.setStartTimestamp(startTime).setDetails(String.format(this.dataManager.getGUIDisplay().loadingGame.details, mods)).build();
   }

   public RichPresence initMenu() {
      return this.mainMenu.setStartTimestamp(startTime).build();
   }

   public RichPresence initMP(String ip) {
      ServerDisplay server = (ServerDisplay)this.dataManager.getServerDisplays().get(ip);
      if (server != null) {
         this.inGame.setLargeImage(server.getKey(), String.format(this.dataManager.getGUIDisplay().inGame.multiPlayer.largeImageText.ip, server.getName()));
      } else if (this.config.hideUnknownIPs) {
         this.inGame.setLargeImage("state-unknown-server", this.dataManager.getGUIDisplay().inGame.multiPlayer.largeImageText.unknown);
      } else {
         this.inGame.setLargeImage("state-unknown-server", String.format(this.dataManager.getGUIDisplay().inGame.multiPlayer.largeImageText.ip, ip));
      }

      return this.inGame.setState(this.dataManager.getGUIDisplay().inGame.multiPlayer.state).setDetails(String.format(this.dataManager.getGUIDisplay().inGame.multiPlayer.details, UplinkAPI.forgeImpl.getIGN())).setStartTimestamp(startTime).setParty(ip, 0, 0).build();
   }

   public RichPresence updatePlayerCount(String partyID, int playerCount, int maxPlayers) {
      return this.inGame.setParty(partyID, playerCount, maxPlayers).build();
   }

   public RichPresence initSP(String world) {
      return this.inGame.setState(this.dataManager.getGUIDisplay().inGame.singlePlayer.state).setDetails(String.format(this.dataManager.getGUIDisplay().inGame.singlePlayer.details, UplinkAPI.forgeImpl.getIGN())).setStartTimestamp(startTime).setLargeImage("state-singleplayer", String.format(this.dataManager.getGUIDisplay().inGame.singlePlayer.largeImageText, world)).build();
   }
}
