package com.jagrosh.discordipc.entities.pipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.Packet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe {
   public RandomAccessFile file;

   WindowsPipe(IPCClient ipcClient, HashMap callbacks, String location) {
      super(ipcClient, callbacks);

      try {
         this.file = new RandomAccessFile(location, "rw");
      } catch (FileNotFoundException var5) {
         this.file = null;
      }

   }

   public void write(byte[] b) throws IOException {
      this.file.write(b);
   }

   public Packet read() throws IOException, JsonParseException {
      while((this.status == PipeStatus.CONNECTED || this.status == PipeStatus.CLOSING) && this.file.length() == 0L) {
         try {
            Thread.sleep(50L);
         } catch (InterruptedException var6) {
         }
      }

      if (this.status == PipeStatus.DISCONNECTED) {
         throw new IOException("Disconnected!");
      } else if (this.status == PipeStatus.CLOSED) {
         return new Packet(Packet.OpCode.CLOSE, (JsonObject)null, this.ipcClient.getEncoding());
      } else {
         Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(this.file.readInt())];
         int len = Integer.reverseBytes(this.file.readInt());
         byte[] d = new byte[len];
         this.file.readFully(d);
         JsonObject packetData = new JsonObject();
         packetData.addProperty("", new String(d));
         Packet p = new Packet(op, packetData, this.ipcClient.getEncoding());
         if (this.ipcClient.isDebugMode()) {
            System.out.printf("Received packet: %s%n", p.toString());
         }

         if (this.listener != null) {
            this.listener.onPacketReceived(this.ipcClient, p);
         }

         return p;
      }
   }

   public void close() throws IOException {
      if (this.ipcClient.isDebugMode()) {
         System.out.println("Closing IPC pipe...");
      }

      this.status = PipeStatus.CLOSING;
      this.send(Packet.OpCode.CLOSE, new JsonObject(), (Callback)null);
      this.status = PipeStatus.CLOSED;
      this.file.close();
   }
}
