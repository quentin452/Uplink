package com.jagrosh.discordipc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.pipe.Pipe;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

public final class IPCClient implements Closeable {
   private final long clientId;
   private final boolean debugMode;
   private final HashMap callbacks = new HashMap();
   private volatile Pipe pipe;
   private IPCListener listener = null;
   private Thread readThread = null;
   private String encoding = "UTF-8";

   public IPCClient(long clientId) {
      this.clientId = clientId;
      this.debugMode = false;
   }

   public IPCClient(long clientId, boolean debugMode) {
      this.clientId = clientId;
      this.debugMode = debugMode;
   }

   private static int getPID() {
      String pr = ManagementFactory.getRuntimeMXBean().getName();
      return Integer.parseInt(pr.substring(0, pr.indexOf(64)));
   }

   public void setListener(IPCListener listener) {
      this.listener = listener;
      if (this.pipe != null) {
         this.pipe.setListener(listener);
      }

   }

   public String getEncoding() {
      return this.encoding;
   }

   public void setEncoding(String encoding) {
      this.encoding = encoding;
   }

   public long getClientID() {
      return this.clientId;
   }

   public boolean isDebugMode() {
      return this.debugMode;
   }

   public void connect(DiscordBuild... preferredOrder) throws NoDiscordClientException {
      this.checkConnected(false);
      this.callbacks.clear();
      this.pipe = null;
      this.pipe = Pipe.openPipe(this, this.clientId, this.callbacks, preferredOrder);
      if (this.debugMode) {
         System.out.println("Client is now connected and ready!");
      }

      if (this.listener != null) {
         this.listener.onReady();
      }

      this.startReading();
   }

   public void sendRichPresence(RichPresence presence) {
      this.sendRichPresence(presence, (Callback)null);
   }

   public void sendRichPresence(RichPresence presence, Callback callback) {
      this.checkConnected(true);
      if (this.debugMode) {
         System.out.println("Sending RichPresence to discord: " + (presence == null ? null : presence.toJson().toString()));
      }

      JsonObject finalObject = new JsonObject();
      JsonObject args = new JsonObject();
      finalObject.addProperty("cmd", "SET_ACTIVITY");
      args.addProperty("pid", getPID());
      args.add("activity", presence == null ? new JsonObject() : presence.toJson());
      finalObject.add("args", args);
      this.pipe.send(Packet.OpCode.FRAME, finalObject, callback);
   }

   public void subscribe(IPCClient.Event sub) {
      this.subscribe(sub, (Callback)null);
   }

   public void subscribe(IPCClient.Event sub, Callback callback) {
      this.checkConnected(true);
      if (!sub.isSubscribable()) {
         throw new IllegalStateException("Cannot subscribe to " + sub + " event!");
      } else {
         if (this.debugMode) {
            System.out.printf("Subscribing to Event: %s%n", sub.name());
         }

         JsonObject pipeData = new JsonObject();
         pipeData.addProperty("cmd", "SUBSCRIBE");
         pipeData.addProperty("evt", sub.name());
         this.pipe.send(Packet.OpCode.FRAME, pipeData, callback);
      }
   }

   public void respondToJoinRequest(User user, IPCClient.ApprovalMode approvalMode, Callback callback) {
      this.checkConnected(true);
      if (user != null) {
         if (this.debugMode) {
            System.out.printf("Sending response to %s as %s%n", user.getName(), approvalMode.name());
         }

         JsonObject pipeData = new JsonObject();
         pipeData.addProperty("cmd", approvalMode == IPCClient.ApprovalMode.ACCEPT ? "SEND_ACTIVITY_JOIN_INVITE" : "CLOSE_ACTIVITY_REQUEST");
         JsonObject args = new JsonObject();
         args.addProperty("user_id", user.getId());
         pipeData.add("args", args);
         this.pipe.send(Packet.OpCode.FRAME, pipeData, callback);
      }

   }

   public PipeStatus getStatus() {
      return this.pipe == null ? PipeStatus.UNINITIALIZED : this.pipe.getStatus();
   }

   public void close() {
      this.checkConnected(true);

      try {
         this.pipe.close();
      } catch (IOException var2) {
         if (this.debugMode) {
            System.out.printf("Failed to close pipe: %s%n", var2);
         }
      }

   }

   public DiscordBuild getDiscordBuild() {
      return this.pipe == null ? null : this.pipe.getDiscordBuild();
   }

   public User getCurrentUser() {
      return this.pipe == null ? null : this.pipe.getCurrentUser();
   }

   private void checkConnected(boolean connected) {
      if (connected && this.getStatus() != PipeStatus.CONNECTED) {
         throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", this.clientId));
      } else if (!connected && this.getStatus() == PipeStatus.CONNECTED) {
         throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", this.clientId));
      }
   }

   private void startReading() {
      this.readThread = new Thread(() -> {
         while(true) {
            try {
               Packet p;
               if ((p = thisx.pipe.read()).getOp() != Packet.OpCode.CLOSE) {
                  JsonObject json = p.getJson();
                  if (json == null) {
                     continue;
                  }

                  IPCClient.Event event = IPCClient.Event.of(json.has("evt") && !json.get("evt").isJsonNull() ? json.getAsJsonPrimitive("evt").getAsString() : null);
                  String nonce = json.has("nonce") && !json.get("nonce").isJsonNull() ? json.getAsJsonPrimitive("nonce").getAsString() : null;
                  switch(event) {
                  case NULL:
                     if (nonce != null && thisx.callbacks.containsKey(nonce)) {
                        ((Callback)thisx.callbacks.remove(nonce)).succeed(p);
                     }
                     break;
                  case ERROR:
                     if (nonce != null && thisx.callbacks.containsKey(nonce)) {
                        ((Callback)thisx.callbacks.remove(nonce)).fail(json.has("data") && json.getAsJsonObject("data").has("message") ? json.getAsJsonObject("data").getAsJsonObject("message").getAsString() : null);
                     }
                     break;
                  case ACTIVITY_JOIN:
                     if (thisx.debugMode) {
                        System.out.println("Reading thread received a 'join' event.");
                     }
                     break;
                  case ACTIVITY_SPECTATE:
                     if (thisx.debugMode) {
                        System.out.println("Reading thread received a 'spectate' event.");
                     }
                     break;
                  case ACTIVITY_JOIN_REQUEST:
                     if (thisx.debugMode) {
                        System.out.println("Reading thread received a 'join request' event.");
                     }
                     break;
                  case UNKNOWN:
                     if (thisx.debugMode) {
                        System.out.println("Reading thread encountered an event with an unknown type: " + json.getAsJsonPrimitive("evt").getAsString());
                     }
                  }

                  if (thisx.listener == null || !json.has("cmd") || !json.getAsJsonPrimitive("cmd").getAsString().equals("DISPATCH")) {
                     continue;
                  }

                  try {
                     JsonObject data = json.getAsJsonObject("data");
                     switch(IPCClient.Event.of(json.getAsJsonPrimitive("evt").getAsString())) {
                     case ACTIVITY_JOIN:
                        thisx.listener.onActivityJoin(this, data.getAsJsonObject("secret").getAsString());
                        continue;
                     case ACTIVITY_SPECTATE:
                        thisx.listener.onActivitySpectate(this, data.getAsJsonObject("secret").getAsString());
                        continue;
                     case ACTIVITY_JOIN_REQUEST:
                        JsonObject u = data.getAsJsonObject("user");
                        User user = new User(u.getAsJsonPrimitive("username").getAsString(), u.getAsJsonPrimitive("discriminator").getAsString(), Long.parseLong(u.getAsJsonPrimitive("id").getAsString()), u.has("avatar") ? u.getAsJsonPrimitive("avatar").getAsString() : null);
                        thisx.listener.onActivityJoinRequest(this, data.has("secret") ? data.getAsJsonObject("secret").getAsString() : null, user);
                     }
                  } catch (Exception var9) {
                     System.out.printf("Exception when handling event: %s%n", var9);
                  }
                  continue;
               }

               thisx.pipe.setStatus(PipeStatus.DISCONNECTED);
               if (thisx.listener != null) {
                  thisx.listener.onClose(this, p.getJson());
               }
            } catch (JsonParseException | IOException var10) {
               if (var10 instanceof IOException) {
                  System.out.printf("Reading thread encountered an IOException: %s%n", var10);
               } else {
                  System.out.printf("Reading thread encountered an JSONException: %s%n", var10);
               }

               thisx.pipe.setStatus(PipeStatus.DISCONNECTED);
               if (thisx.listener != null) {
                  thisx.listener.onDisconnect(this, var10);
               }
            }

            return;
         }
      }, "IPCClient-Reader");
      if (this.debugMode) {
         System.out.println("Starting IPCClient reading thread!");
      }

      this.readThread.start();
   }

   public static enum Event {
      NULL(false),
      READY(false),
      ERROR(false),
      ACTIVITY_JOIN(true),
      ACTIVITY_SPECTATE(true),
      ACTIVITY_JOIN_REQUEST(true),
      UNKNOWN(false);

      private final boolean subscribable;

      private Event(boolean subscribable) {
         this.subscribable = subscribable;
      }

      static IPCClient.Event of(String str) {
         if (str == null) {
            return NULL;
         } else {
            IPCClient.Event[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               IPCClient.Event s = var1[var3];
               if (s != UNKNOWN && s.name().equalsIgnoreCase(str)) {
                  return s;
               }
            }

            return UNKNOWN;
         }
      }

      public boolean isSubscribable() {
         return this.subscribable;
      }
   }

   public static enum ApprovalMode {
      ACCEPT,
      DENY;
   }
}
