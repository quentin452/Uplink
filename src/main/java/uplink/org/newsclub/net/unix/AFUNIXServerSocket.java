package org.newsclub.net.unix;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;

public class AFUNIXServerSocket extends ServerSocket {
   private final AFUNIXSocketImpl implementation;
   private AFUNIXSocketAddress boundEndpoint;

   protected AFUNIXServerSocket() throws IOException {
      this.setReuseAddress(true);
      this.implementation = new AFUNIXSocketImpl();
      NativeUnixSocket.initServerImpl(this, this.implementation);
      NativeUnixSocket.setCreatedServer(this);
   }

   public static AFUNIXServerSocket newInstance() throws IOException {
      AFUNIXServerSocket instance = new AFUNIXServerSocket();
      return instance;
   }

   public static AFUNIXServerSocket bindOn(AFUNIXSocketAddress addr) throws IOException {
      AFUNIXServerSocket socket = newInstance();
      socket.bind(addr);
      return socket;
   }

   public void bind(SocketAddress endpoint, int backlog) throws IOException {
      if (this.isClosed()) {
         throw new SocketException("Socket is closed");
      } else if (this.isBound()) {
         throw new SocketException("Already bound");
      } else if (!(endpoint instanceof AFUNIXSocketAddress)) {
         throw new IOException("Can only bind to endpoints of type " + AFUNIXSocketAddress.class.getName());
      } else {
         this.implementation.bind(endpoint, this.getReuseAddress() ? -1 : 0);
         this.boundEndpoint = (AFUNIXSocketAddress)endpoint;
         this.implementation.listen(backlog);
      }
   }

   public boolean isBound() {
      return this.boundEndpoint != null;
   }

   public boolean isClosed() {
      return super.isClosed() || this.isBound() && !this.implementation.getFD().valid();
   }

   public AFUNIXSocket accept() throws IOException {
      if (this.isClosed()) {
         throw new SocketException("Socket is closed");
      } else {
         AFUNIXSocket as = AFUNIXSocket.newInstance();
         this.implementation.accept(as.impl);
         as.addr = this.boundEndpoint;
         NativeUnixSocket.setConnected(as);
         return as;
      }
   }

   public String toString() {
      return !this.isBound() ? "AFUNIXServerSocket[unbound]" : "AFUNIXServerSocket[" + this.boundEndpoint.toString() + "]";
   }

   public void close() throws IOException {
      if (!this.isClosed()) {
         super.close();
         this.implementation.close();
      }
   }

   public static boolean isSupported() {
      return NativeUnixSocket.isLoaded();
   }
}
