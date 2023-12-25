package ru.ijo42.uplink.api.config.display;

public class GUIDisplay {
   public GUIDisplay.MainMenu mainMenu;
   public GUIDisplay.LoadingGame loadingGame;
   public GUIDisplay.InGame inGame;

   public static class InGame {
      public GUIDisplay.InGame.MultiPlayer multiPlayer;
      public GUIDisplay.InGame.SinglePlayer singlePlayer;

      public static class SinglePlayer {
         public String state;
         public String largeImageText;
         public String details;
      }

      public static class MultiPlayer {
         public GUIDisplay.InGame.MultiPlayer.LargeImageText largeImageText;
         public String state;
         public String details;

         public static class LargeImageText {
            public String unknown;
            public String ip;
         }
      }
   }

   public static class LoadingGame {
      public String state;
      public String largeImageText;
      public String details;
   }

   public static class MainMenu {
      public String state;
      public String largeImageText;
   }
}
