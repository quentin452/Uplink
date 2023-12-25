package com.jagrosh.discordipc.entities.pipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Pipe {
   private static final int VERSION = 1;
   private static final String[] unixPaths = new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
   final IPCClient ipcClient;
   private final HashMap callbacks;
   PipeStatus status;
   IPCListener listener;
   private DiscordBuild build;
   private User currentUser;

   Pipe(IPCClient ipcClient, HashMap callbacks) {
      this.status = PipeStatus.CONNECTING;
      this.ipcClient = ipcClient;
      this.callbacks = callbacks;
   }

   public static Pipe openPipe(IPCClient ipcClient, long clientId, HashMap callbacks, DiscordBuild... preferredOrder) throws NoDiscordClientException {
      if (preferredOrder == null || preferredOrder.length == 0) {
         preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};
      }

      Pipe pipe = null;
      Pipe[] open = new Pipe[DiscordBuild.values().length];

      int i;
      for(i = 0; i < 10; ++i) {
         try {
            String location = getPipeLocation(i);
            if (ipcClient.isDebugMode()) {
               System.out.printf("Searching for IPC: %s%n", location);
            }

            pipe = createPipe(ipcClient, callbacks, location);
            if (pipe != null) {
               JsonObject finalObject = new JsonObject();
               finalObject.addProperty("v", 1);
               finalObject.addProperty("client_id", Long.toString(clientId));
               pipe.send(Packet.OpCode.HANDSHAKE, finalObject, (Callback)null);
               Packet p = pipe.read();
               JsonObject parsedData = (new JsonParser()).parse(p.getJson().getAsJsonPrimitive("").getAsString()).getAsJsonObject();
               JsonObject data = parsedData.getAsJsonObject("data");
               JsonObject userData = data.getAsJsonObject("user");
               pipe.build = DiscordBuild.from(data.getAsJsonObject("config").get("api_endpoint").getAsString());
               pipe.currentUser = new User(userData.getAsJsonPrimitive("username").getAsString(), userData.getAsJsonPrimitive("discriminator").getAsString(), Long.parseLong(userData.getAsJsonPrimitive("id").getAsString()), userData.has("avatar") ? userData.getAsJsonPrimitive("avatar").getAsString() : null);
               if (ipcClient.isDebugMode()) {
                  System.out.printf("Found a valid client (%s) with packet: %s%n", pipe.build.name(), p.toString());
                  System.out.printf("Found a valid user (%s) with id: %s%n", pipe.currentUser.getName(), pipe.currentUser.getId());
               }

               if (pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0]) {
                  if (ipcClient.isDebugMode()) {
                     System.out.printf("Found preferred client: %s%n", pipe.build.name());
                  }
                  break;
               }

               open[pipe.build.ordinal()] = pipe;
               open[DiscordBuild.ANY.ordinal()] = pipe;
               pipe.build = null;
               pipe = null;
            }
         } catch (JsonParseException | IOException var15) {
            pipe = null;
         }
      }

      if (pipe == null) {
         for(i = 1; i < preferredOrder.length; ++i) {
            DiscordBuild cb = preferredOrder[i];
            if (ipcClient.isDebugMode()) {
               System.out.printf("Looking for client build: %s%n", cb.name());
            }

            if (open[cb.ordinal()] != null) {
               pipe = open[cb.ordinal()];
               open[cb.ordinal()] = null;
               if (cb == DiscordBuild.ANY) {
                  for(int k = 0; k < open.length; ++k) {
                     if (open[k] == pipe) {
                        pipe.build = DiscordBuild.values()[k];
                        open[k] = null;
                     }
                  }
               } else {
                  pipe.build = cb;
               }

               if (ipcClient.isDebugMode()) {
                  System.out.printf("Found preferred client: %s%n", pipe.build.name());
               }
               break;
            }
         }

         if (pipe == null) {
            throw new NoDiscordClientException();
         }
      }

      for(i = 0; i < open.length; ++i) {
         if (i != DiscordBuild.ANY.ordinal() && open[i] != null) {
            try {
               open[i].close();
            } catch (IOException var14) {
               if (ipcClient.isDebugMode()) {
                  System.out.printf("Failed to close an open IPC pipe: %s%n", var14);
               }
            }
         }
      }

      pipe.status = PipeStatus.CONNECTED;
      return pipe;
   }

   private static Pipe createPipe(IPCClient ipcClient, HashMap callbacks, String location) {
      String osName = System.getProperty("os.name").toLowerCase();
      if (osName.contains("win")) {
         WindowsPipe attemptedPipe = new WindowsPipe(ipcClient, callbacks, location);
         return attemptedPipe.file != null ? attemptedPipe : null;
      } else if (!osName.contains("linux") && !osName.contains("mac")) {
         throw new RuntimeException("Unsupported OS: " + osName);
      } else {
         try {
            return new UnixPipe(ipcClient, callbacks, location);
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }
      }
   }

   private static String generateNonce() {
      return UUID.randomUUID().toString();
   }

   private static String getPipeLocation(int i) {
      if (System.getProperty("os.name").contains("Win")) {
         return "\\\\?\\pipe\\discord-ipc-" + i;
      } else {
         String tmpPath = null;
         String[] var2 = unixPaths;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String str = var2[var4];
            tmpPath = System.getenv(str);
            if (tmpPath != null) {
               break;
            }
         }

         if (tmpPath == null) {
            tmpPath = "/tmp";
         }

         return tmpPath + "/discord-ipc-" + i;
      }
   }

   public void send(Packet.OpCode op, JsonObject data, Callback callback) {
      try {
         String nonce = generateNonce();
         data.addProperty("nonce", nonce);
         Packet p = new Packet(op, data, this.ipcClient.getEncoding());
         if (callback != null && !callback.isEmpty()) {
            this.callbacks.put(nonce, callback);
         }

         this.write(p.toBytes());
         if (this.ipcClient.isDebugMode()) {
            System.out.printf("Sent packet: %s%n", p.toString());
         }

         if (this.listener != null) {
            this.listener.onPacketSent(this.ipcClient, p);
         }
      } catch (IOException var6) {
         System.out.println("Encountered an IOException while sending a packet and disconnected!");
         this.status = PipeStatus.DISCONNECTED;
      }

   }

   public abstract Packet read() throws IOException, JsonParseException;

   public abstract void write(byte[] var1) throws IOException;

   public PipeStatus getStatus() {
      return this.status;
   }

   public void setStatus(PipeStatus status) {
      this.status = status;
   }

   public void setListener(IPCListener listener) {
      this.listener = listener;
   }

   public abstract void close() throws IOException;

   public DiscordBuild getDiscordBuild() {
      return this.build;
   }

   public User getCurrentUser() {
      return this.currentUser;
   }
}
