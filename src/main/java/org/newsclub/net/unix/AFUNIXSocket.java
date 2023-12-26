package org.newsclub.net.unix;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public final class AFUNIXSocket extends Socket {
   static String loadedLibrary;
   private static Integer capabilities = null;
   AFUNIXSocketImpl impl;
   AFUNIXSocketAddress addr;
   private final AFUNIXSocketFactory socketFactory;

   private AFUNIXSocket(AFUNIXSocketImpl impl, AFUNIXSocketFactory factory) throws IOException {
      super(impl);
      this.socketFactory = factory;
      if (factory == null) {
         this.setIsCreated();
      }

   }

   private void setIsCreated() throws IOException {
      try {
         NativeUnixSocket.setCreated(this);
      } catch (LinkageError var2) {
         throw new IOException("Couldn't load native library", var2);
      }
   }

   public static AFUNIXSocket newInstance() throws IOException {
      return newInstance((AFUNIXSocketFactory)null);
   }

   static AFUNIXSocket newInstance(AFUNIXSocketFactory factory) throws IOException {
      AFUNIXSocketImpl impl = new AFUNIXSocketImpl.Lenient();
      AFUNIXSocket instance = new AFUNIXSocket(impl, factory);
      instance.impl = impl;
      return instance;
   }

   public static AFUNIXSocket newStrictInstance() throws IOException {
      AFUNIXSocketImpl impl = new AFUNIXSocketImpl();
      AFUNIXSocket instance = new AFUNIXSocket(impl, (AFUNIXSocketFactory)null);
      instance.impl = impl;
      return instance;
   }

   public static AFUNIXSocket connectTo(AFUNIXSocketAddress addr) throws IOException {
      AFUNIXSocket socket = newInstance();
      socket.connect(addr);
      return socket;
   }

   public void bind(SocketAddress bindpoint) throws IOException {
      super.bind(bindpoint);
      this.addr = (AFUNIXSocketAddress)bindpoint;
   }

   public void connect(SocketAddress endpoint) throws IOException {
      this.connect(endpoint, 0);
   }

   public void connect(SocketAddress endpoint, int timeout) throws IOException {
      if (!(endpoint instanceof AFUNIXSocketAddress)) {
         if (this.socketFactory != null && endpoint instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress)endpoint;
            String hostname = isa.getHostString();
            if (this.socketFactory.isHostnameSupported(hostname)) {
               endpoint = this.socketFactory.addressFromHost(hostname, isa.getPort());
            }
         }

         if (!(endpoint instanceof AFUNIXSocketAddress)) {
            throw new IllegalArgumentException("Can only connect to endpoints of type " + AFUNIXSocketAddress.class.getName() + ", got: " + endpoint);
         }
      }

      this.impl.connect((SocketAddress)endpoint, timeout);
      this.addr = (AFUNIXSocketAddress)endpoint;
      NativeUnixSocket.setBound(this);
      NativeUnixSocket.setConnected(this);
   }

   public String toString() {
      return this.isConnected() ? "AFUNIXSocket[fd=" + this.impl.getFD() + ";addr=" + this.addr.toString() + "]" : "AFUNIXSocket[unconnected]";
   }

   public static boolean isSupported() {
      return NativeUnixSocket.isLoaded();
   }

   public static String getLoadedLibrary() {
      return loadedLibrary;
   }

   public AFUNIXSocketCredentials getPeerCredentials() throws IOException {
      if (!this.isClosed() && this.isConnected()) {
         return this.impl.getPeerCredentials();
      } else {
         throw new SocketException("Not connected");
      }
   }

   public boolean isClosed() {
      return super.isClosed() || this.isConnected() && !this.impl.getFD().valid();
   }

   public int getAncillaryReceiveBufferSize() {
      return this.impl.getAncillaryReceiveBufferSize();
   }

   public void setAncillaryReceiveBufferSize(int size) {
      this.impl.setAncillaryReceiveBufferSize(size);
   }

   public FileDescriptor[] getReceivedFileDescriptors() throws IOException {
      return this.impl.getReceivedFileDescriptors();
   }

   public void clearReceivedFileDescriptors() {
      this.impl.clearReceivedFileDescriptors();
   }

   public void setOutboundFileDescriptors(FileDescriptor... fdescs) throws IOException {
      if (fdescs != null && fdescs.length != 0) {
         int numFdescs = fdescs.length;
         int[] fds = new int[numFdescs];

         for(int i = 0; i < numFdescs; ++i) {
            FileDescriptor fdesc = fdescs[i];
            fds[i] = NativeUnixSocket.getFD(fdesc);
         }

         this.impl.setOutboundFileDescriptors(fds);
      } else {
         this.impl.setOutboundFileDescriptors((int[])null);
      }

   }

   private static synchronized int getCapabilities() {
      if (capabilities == null) {
         if (!isSupported()) {
            capabilities = 0;
         } else {
            capabilities = NativeUnixSocket.capabilities();
         }
      }

      return capabilities;
   }

   public static boolean supports(AFUNIXSocketCapability capability) {
      return (getCapabilities() & capability.getBitmask()) != 0;
   }
}
