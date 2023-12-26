package org.newsclub.net.unix;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Locale;

public final class AFUNIXSocketAddress extends InetSocketAddress {
   private static final long serialVersionUID = 1L;
   private final byte[] bytes;

   public AFUNIXSocketAddress(File socketFile) throws IOException {
      this((File)socketFile, 0);
   }

   public AFUNIXSocketAddress(File socketFile, int port) throws IOException {
      this(socketFile.getCanonicalPath().getBytes(Charset.defaultCharset()), port);
   }

   public AFUNIXSocketAddress(byte[] socketAddress) throws IOException {
      this((byte[])socketAddress, 0);
   }

   public AFUNIXSocketAddress(byte[] socketAddress, int port) throws IOException {
      super(0);
      if (port != 0) {
         NativeUnixSocket.setPort1(this, port);
      }

      if (socketAddress.length == 0) {
         throw new SocketException("Illegal address length: " + socketAddress.length);
      } else {
         this.bytes = (byte[])socketAddress.clone();
      }
   }

   public static AFUNIXSocketAddress inAbstractNamespace(String name) throws IOException {
      return inAbstractNamespace(name, 0);
   }

   public static AFUNIXSocketAddress inAbstractNamespace(String name, int port) throws IOException {
      byte[] bytes = name.getBytes(Charset.defaultCharset());
      byte[] addr = new byte[bytes.length + 1];
      System.arraycopy(bytes, 0, addr, 1, bytes.length);
      return new AFUNIXSocketAddress(addr, port);
   }

   byte[] getBytes() {
      return this.bytes;
   }

   private static String prettyPrint(byte[] data) {
      int dataLength = data.length;
      StringBuilder sb = new StringBuilder(dataLength + 16);

      for(int i = 0; i < dataLength; ++i) {
         byte c = data[i];
         if (c >= 32 && c < 127) {
            sb.append((char)c);
         } else {
            sb.append("\\x");
            sb.append(String.format(Locale.ENGLISH, "%02x", c));
         }
      }

      return sb.toString();
   }

   public String toString() {
      return this.getClass().getName() + "[port=" + this.getPort() + ";address=" + prettyPrint(this.bytes) + "]";
   }
}
