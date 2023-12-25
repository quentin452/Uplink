package com.jagrosh.discordipc.entities.pipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.Packet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class UnixPipe extends Pipe {
   private final AFUNIXSocket socket = AFUNIXSocket.newInstance();

   UnixPipe(IPCClient ipcClient, HashMap callbacks, String location) throws IOException {
      super(ipcClient, callbacks);
      this.socket.connect(new AFUNIXSocketAddress(new File(location)));
   }

   public Packet read() throws IOException, JsonParseException {
      InputStream is = this.socket.getInputStream();

      while((this.status == PipeStatus.CONNECTED || this.status == PipeStatus.CLOSING) && is.available() == 0) {
         try {
            Thread.sleep(50L);
         } catch (InterruptedException var9) {
         }
      }

      if (this.status == PipeStatus.DISCONNECTED) {
         throw new IOException("Disconnected!");
      } else if (this.status == PipeStatus.CLOSED) {
         return new Packet(Packet.OpCode.CLOSE, (JsonObject)null, this.ipcClient.getEncoding());
      } else {
         byte[] d = new byte[8];
         int readResult = is.read(d);
         ByteBuffer bb = ByteBuffer.wrap(d);
         if (this.ipcClient.isDebugMode()) {
            System.out.printf("Read Byte Data: %s with result %s%n", new String(d), readResult);
         }

         Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(bb.getInt())];
         d = new byte[Integer.reverseBytes(bb.getInt())];
         int reversedResult = is.read(d);
         if (this.ipcClient.isDebugMode()) {
            System.out.printf("Read Reversed Byte Data: %s with result %s%n", new String(d), reversedResult);
         }

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

   public void write(byte[] b) throws IOException {
      this.socket.getOutputStream().write(b);
   }

   public void close() throws IOException {
      if (this.ipcClient.isDebugMode()) {
         System.out.println("Closing IPC pipe...");
      }

      this.status = PipeStatus.CLOSING;
      this.send(Packet.OpCode.CLOSE, new JsonObject(), (Callback)null);
      this.status = PipeStatus.CLOSED;
      this.socket.close();
   }
}
