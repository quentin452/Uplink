package com.jagrosh.discordipc.entities;

import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class Packet {
   private final Packet.OpCode op;
   private final JsonObject data;
   private final String encoding;

   public Packet(Packet.OpCode op, JsonObject data, String encoding) {
      this.op = op;
      this.data = data;
      this.encoding = encoding;
   }

   /** @deprecated */
   @Deprecated
   public Packet(Packet.OpCode op, JsonObject data) {
      this(op, data, "UTF-8");
   }

   public byte[] toBytes() {
      String s = this.data.toString();

      byte[] d;
      try {
         d = s.getBytes(this.encoding);
      } catch (UnsupportedEncodingException var4) {
         d = s.getBytes();
      }

      ByteBuffer packet = ByteBuffer.allocate(d.length + 8);
      packet.putInt(Integer.reverseBytes(this.op.ordinal()));
      packet.putInt(Integer.reverseBytes(d.length));
      packet.put(d);
      return packet.array();
   }

   public Packet.OpCode getOp() {
      return this.op;
   }

   public JsonObject getJson() {
      return this.data;
   }

   public String toString() {
      return "Pkt:" + this.getOp() + this.getJson().toString();
   }

   public static enum OpCode {
      HANDSHAKE,
      FRAME,
      CLOSE,
      PING,
      PONG;
   }
}
