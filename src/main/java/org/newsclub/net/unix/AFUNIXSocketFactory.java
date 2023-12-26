package org.newsclub.net.unix;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Objects;
import javax.net.SocketFactory;

public abstract class AFUNIXSocketFactory extends SocketFactory {
   protected abstract AFUNIXSocketAddress addressFromHost(String var1, int var2) throws IOException;

   protected boolean isHostnameSupported(String host) {
      return host != null;
   }

   protected boolean isInetAddressSupported(InetAddress address) {
      return address != null && this.isHostnameSupported(address.getHostName());
   }

   public Socket createSocket() throws IOException {
      return AFUNIXSocket.newInstance(this);
   }

   public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      if (!this.isHostnameSupported(host)) {
         throw new UnknownHostException();
      } else if (port < 0) {
         throw new IllegalArgumentException("Illegal port");
      } else {
         AFUNIXSocketAddress socketAddress = this.addressFromHost(host, port);
         return AFUNIXSocket.connectTo(socketAddress);
      }
   }

   public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
      if (!this.isHostnameSupported(host)) {
         throw new UnknownHostException();
      } else if (localPort < 0) {
         throw new IllegalArgumentException("Illegal local port");
      } else {
         return this.createSocket(host, port);
      }
   }

   public Socket createSocket(InetAddress address, int port) throws IOException {
      if (!this.isInetAddressSupported(address)) {
         throw new UnknownHostException();
      } else {
         String hostname = address.getHostName();
         if (!this.isHostnameSupported(hostname)) {
            throw new UnknownHostException();
         } else {
            return this.createSocket(hostname, port);
         }
      }
   }

   public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      if (!this.isInetAddressSupported(address)) {
         throw new UnknownHostException();
      } else {
         Objects.requireNonNull(localAddress, "Local address was null");
         if (localPort < 0) {
            throw new IllegalArgumentException("Illegal local port");
         } else {
            return this.createSocket(address, port);
         }
      }
   }

   public static final class URIScheme extends AFUNIXSocketFactory {
      private static final String FILE_SCHEME_PREFIX = "file://";
      private static final String FILE_SCHEME_PREFIX_ENCODED = "file%";
      private static final String FILE_SCHEME_LOCALHOST = "localhost";

      private static String stripBrackets(String host) {
         if (host.startsWith("[")) {
            if (host.endsWith("]")) {
               host = host.substring(1, host.length() - 1);
            } else {
               host = host.substring(1);
            }
         }

         return host;
      }

      protected boolean isHostnameSupported(String host) {
         host = stripBrackets(host);
         return host.startsWith("file://") || host.startsWith("file%");
      }

      protected AFUNIXSocketAddress addressFromHost(String host, int port) throws IOException {
         host = stripBrackets(host);
         if (host.startsWith("file%")) {
            try {
               host = URLDecoder.decode(host, "UTF-8");
            } catch (Exception var5) {
               throw (UnknownHostException)(new UnknownHostException()).initCause(var5);
            }
         }

         if (!host.startsWith("file://")) {
            throw new UnknownHostException();
         } else {
            String path = host.substring("file://".length());
            if (path.isEmpty()) {
               throw new UnknownHostException();
            } else {
               if (path.startsWith("localhost")) {
                  path = path.substring("localhost".length());
               }

               if (!path.startsWith("/")) {
                  throw new UnknownHostException();
               } else {
                  File socketFile = new File(path);
                  return new AFUNIXSocketAddress(socketFile, port);
               }
            }
         }
      }
   }

   public static final class SystemProperty extends AFUNIXSocketFactory.DefaultSocketHostnameSocketFactory {
      private static final String PROP_SOCKET_DEFAULT = "org.newsclub.net.unix.socket.default";

      public SystemProperty() {
         super(null);
      }

      protected AFUNIXSocketAddress addressFromHost(String host, int port) throws IOException {
         String path = System.getProperty("org.newsclub.net.unix.socket.default");
         if (path != null && !path.isEmpty()) {
            File socketFile = new File(path);
            return new AFUNIXSocketAddress(socketFile, port);
         } else {
            throw new IllegalStateException("Property not configured: org.newsclub.net.unix.socket.default");
         }
      }
   }

   public static final class FactoryArg extends AFUNIXSocketFactory.DefaultSocketHostnameSocketFactory {
      private final File socketFile;

      public FactoryArg(String socketPath) {
         super(null);
         Objects.requireNonNull(socketPath, "Socket path was null");
         this.socketFile = new File(socketPath);
      }

      public FactoryArg(File file) {
         super(null);
         Objects.requireNonNull(file, "File was null");
         this.socketFile = file;
      }

      protected AFUNIXSocketAddress addressFromHost(String host, int port) throws IOException {
         return new AFUNIXSocketAddress(this.socketFile, port);
      }
   }

   private abstract static class DefaultSocketHostnameSocketFactory extends AFUNIXSocketFactory {
      private static final String PROP_SOCKET_HOSTNAME = "org.newsclub.net.unix.socket.hostname";

      private DefaultSocketHostnameSocketFactory() {
      }

      protected final boolean isHostnameSupported(String host) {
         return getDefaultSocketHostname().equals(host);
      }

      private static String getDefaultSocketHostname() {
         return System.getProperty("org.newsclub.net.unix.socket.hostname", "localhost");
      }

      // $FF: synthetic method
      DefaultSocketHostnameSocketFactory(Object x0) {
         this();
      }
   }
}
