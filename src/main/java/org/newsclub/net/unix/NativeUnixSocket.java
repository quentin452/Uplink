package org.newsclub.net.unix;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

final class NativeUnixSocket {
   private static boolean loaded = false;

   private NativeUnixSocket() {
      throw new UnsupportedOperationException("No instances");
   }

   static boolean isLoaded() {
      return loaded;
   }

   static void checkSupported() {
   }

   static native void init() throws Exception;

   static native void destroy() throws Exception;

   static native int capabilities();

   static native long bind(byte[] var0, FileDescriptor var1, int var2) throws IOException;

   static native void listen(FileDescriptor var0, int var1) throws IOException;

   static native void accept(byte[] var0, FileDescriptor var1, FileDescriptor var2, long var3, int var5) throws IOException;

   static native void connect(byte[] var0, FileDescriptor var1, long var2) throws IOException;

   static native int read(AFUNIXSocketImpl var0, FileDescriptor var1, byte[] var2, int var3, int var4, ByteBuffer var5) throws IOException;

   static native int write(AFUNIXSocketImpl var0, FileDescriptor var1, byte[] var2, int var3, int var4, int[] var5) throws IOException;

   static native void close(FileDescriptor var0) throws IOException;

   static native void shutdown(FileDescriptor var0, int var1) throws IOException;

   static native int getSocketOptionInt(FileDescriptor var0, int var1) throws IOException;

   static native void setSocketOptionInt(FileDescriptor var0, int var1, int var2) throws IOException;

   static native int available(FileDescriptor var0) throws IOException;

   static native AFUNIXSocketCredentials peerCredentials(FileDescriptor var0, AFUNIXSocketCredentials var1) throws IOException;

   static native void initServerImpl(AFUNIXServerSocket var0, AFUNIXSocketImpl var1) throws IOException;

   static native void setCreated(AFUNIXSocket var0);

   static native void setConnected(AFUNIXSocket var0);

   static native void setBound(AFUNIXSocket var0);

   static native void setCreatedServer(AFUNIXServerSocket var0);

   static native void setBoundServer(AFUNIXServerSocket var0);

   static native void setPort(AFUNIXSocketAddress var0, int var1);

   static native void initFD(FileDescriptor var0, int var1) throws IOException;

   static native int getFD(FileDescriptor var0) throws IOException;

   static native void attachCloseable(FileDescriptor var0, Closeable var1);

   static native int maxAddressLength();

   static void setPort1(AFUNIXSocketAddress addr, int port) throws IOException {
      if (port < 0) {
         throw new IllegalArgumentException("port out of range:" + port);
      } else {
         try {
            setPort(addr, port);
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Exception var4) {
            throw new IOException("Could not set port", var4);
         }
      }
   }

   static {
      NativeLibraryLoader nll = new NativeLibraryLoader();

      try {
         nll.loadLibrary();
      } catch (Throwable var4) {
         try {
            nll.close();
         } catch (Throwable var3) {
            var4.addSuppressed(var3);
         }

         throw var4;
      }

      nll.close();
      loaded = true;
   }
}
