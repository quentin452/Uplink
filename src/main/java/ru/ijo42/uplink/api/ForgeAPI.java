package ru.ijo42.uplink.api;

import java.nio.file.Path;

public interface ForgeAPI {
   int getModsCount();

   int getPlayerCount();

   int getMaxPlayers();

   String getIGN();

   Path getConfigDir();

   boolean isMP();

   String getServerIP();

   String getWorldName();

   void afterInit(PresenceListener var1);
}
