package org.newsclub.net.unix;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class AFUNIXSocketImpl extends SocketImpl {
   private static final int SHUT_RD = 0;
   private static final int SHUT_WR = 1;
   private static final int SHUT_RD_WR = 2;
   private org.newsclub.net.unix.AFUNIXSocketAddress socketAddress;
   private long inode = -1L;
   private volatile boolean closed = false;
   private volatile boolean bound = false;
   private boolean connected = false;
   private volatile boolean closedInputStream = false;
   private volatile boolean closedOutputStream = false;
   private final AFUNIXSocketImpl.AFUNIXInputStream in = new AFUNIXSocketImpl.AFUNIXInputStream();
   private final AFUNIXSocketImpl.AFUNIXOutputStream out = new AFUNIXSocketImpl.AFUNIXOutputStream();
   private final AtomicInteger pendingAccepts = new AtomicInteger(0);
   private boolean reuseAddr = true;
   private ByteBuffer ancillaryReceiveBuffer = ByteBuffer.allocateDirect(0);
   private final List receivedFileDescriptors = Collections.synchronizedList(new LinkedList());
   private int[] pendingFileDescriptors = null;
   private final Map closeableFileDescriptors = Collections.synchronizedMap(new HashMap());
   private int timeout = 0;

   protected AFUNIXSocketImpl() {
      this.fd = new FileDescriptor();
   }

   FileDescriptor getFD() {
      return this.fd;
   }

   protected final void finalize() {
      try {
         this.close();
      } catch (Throwable var5) {
      }

      try {
         synchronized(this.closeableFileDescriptors) {
            Iterator var2 = this.closeableFileDescriptors.keySet().iterator();

            while(var2.hasNext()) {
               FileDescriptor fd = (FileDescriptor)var2.next();
               org.newsclub.net.unix.NativeUnixSocket.close(fd);
            }
         }
      } catch (Throwable var7) {
      }

   }

   protected void accept(SocketImpl socket) throws IOException {
      FileDescriptor fdesc = this.validFdOrException();
      AFUNIXSocketImpl si = (AFUNIXSocketImpl)socket;

      try {
         if (this.pendingAccepts.incrementAndGet() >= Integer.MAX_VALUE) {
            throw new SocketException("Too many pending accepts");
         }

         if (!this.bound || this.closed) {
            throw new SocketException("Socket is closed");
         }

         org.newsclub.net.unix.NativeUnixSocket.accept(this.socketAddress.getBytes(), fdesc, si.fd, this.inode, this.timeout);
         if (!this.bound || this.closed) {
            try {
               org.newsclub.net.unix.NativeUnixSocket.shutdown(si.fd, 2);
            } catch (Exception var10) {
            }

            try {
               org.newsclub.net.unix.NativeUnixSocket.close(si.fd);
            } catch (Exception var9) {
            }

            throw new SocketException("Socket is closed");
         }
      } finally {
         this.pendingAccepts.decrementAndGet();
      }

      si.socketAddress = this.socketAddress;
      si.connected = true;
   }

   protected int available() throws IOException {
      FileDescriptor fdesc = this.validFdOrException();
      return org.newsclub.net.unix.NativeUnixSocket.available(fdesc);
   }

   protected void bind(SocketAddress addr) throws IOException {
      this.bind((SocketAddress)addr, -1);
   }

   protected void bind(SocketAddress addr, int options) throws IOException {
      if (!(addr instanceof org.newsclub.net.unix.AFUNIXSocketAddress)) {
         throw new SocketException("Cannot bind to this type of address: " + addr.getClass());
      } else {
         this.socketAddress = (org.newsclub.net.unix.AFUNIXSocketAddress)addr;
         this.inode = org.newsclub.net.unix.NativeUnixSocket.bind(this.socketAddress.getBytes(), this.fd, options);
         this.validFdOrException();
         this.bound = true;
         this.localport = this.socketAddress.getPort();
      }
   }

   protected void bind(InetAddress host, int port) throws IOException {
      throw new SocketException("Cannot bind to this type of address: " + InetAddress.class);
   }

   private void checkClose() throws IOException {
      if (this.closedInputStream && this.closedOutputStream) {
         this.close();
      }

   }

   private void unblockAccepts() {
      while(this.pendingAccepts.get() > 0) {
         try {
            FileDescriptor tmpFd = new FileDescriptor();

            try {
               org.newsclub.net.unix.NativeUnixSocket.connect(this.socketAddress.getBytes(), tmpFd, this.inode);
            } catch (IOException var5) {
               return;
            }

            try {
               org.newsclub.net.unix.NativeUnixSocket.shutdown(tmpFd, 2);
            } catch (Exception var4) {
            }

            try {
               org.newsclub.net.unix.NativeUnixSocket.close(tmpFd);
            } catch (Exception var3) {
            }
         } catch (Exception var6) {
         }
      }

   }

   protected final synchronized void close() throws IOException {
      boolean wasBound = this.bound;
      this.bound = false;
      FileDescriptor fdesc = this.validFd();
      if (fdesc != null) {
         org.newsclub.net.unix.NativeUnixSocket.shutdown(fdesc, 2);
         this.closed = true;
         if (wasBound && this.socketAddress != null && this.socketAddress.getBytes() != null && this.inode >= 0L) {
            this.unblockAccepts();
         }

         org.newsclub.net.unix.NativeUnixSocket.close(fdesc);
      }

      this.closed = true;
   }

   protected void connect(String host, int port) throws IOException {
      throw new SocketException("Cannot bind to this type of address: " + InetAddress.class);
   }

   protected void connect(InetAddress address, int port) throws IOException {
      throw new SocketException("Cannot bind to this type of address: " + InetAddress.class);
   }

   protected void connect(SocketAddress addr, int connectTimeout) throws IOException {
      if (!(addr instanceof org.newsclub.net.unix.AFUNIXSocketAddress)) {
         throw new SocketException("Cannot bind to this type of address: " + addr.getClass());
      } else {
         this.socketAddress = (org.newsclub.net.unix.AFUNIXSocketAddress)addr;
         org.newsclub.net.unix.NativeUnixSocket.connect(this.socketAddress.getBytes(), this.fd, -1L);
         this.validFdOrException();
         this.address = this.socketAddress.getAddress();
         this.port = this.socketAddress.getPort();
         this.localport = 0;
         this.connected = true;
      }
   }

   protected void create(boolean stream) throws IOException {
   }

   protected InputStream getInputStream() throws IOException {
      if (!this.connected && !this.bound) {
         throw new IOException("Not connected/not bound");
      } else {
         this.validFdOrException();
         return this.in;
      }
   }

   protected OutputStream getOutputStream() throws IOException {
      if (!this.connected && !this.bound) {
         throw new IOException("Not connected/not bound");
      } else {
         this.validFdOrException();
         return this.out;
      }
   }

   protected void listen(int backlog) throws IOException {
      FileDescriptor fdesc = this.validFdOrException();
      if (backlog <= 0) {
         backlog = 50;
      }

      org.newsclub.net.unix.NativeUnixSocket.listen(fdesc, backlog);
   }

   protected void sendUrgentData(int data) throws IOException {
      FileDescriptor fdesc = this.validFdOrException();
      org.newsclub.net.unix.NativeUnixSocket.write(this, fdesc, new byte[]{(byte)(data & 255)}, 0, 1, this.pendingFileDescriptors);
   }

   private FileDescriptor validFdOrException() throws SocketException {
      FileDescriptor fdesc = this.validFd();
      if (fdesc == null) {
         throw new SocketException("Not open");
      } else {
         return fdesc;
      }
   }

   private synchronized FileDescriptor validFd() {
      if (this.closed) {
         return null;
      } else {
         FileDescriptor descriptor = this.fd;
         return descriptor != null && descriptor.valid() ? descriptor : null;
      }
   }

   public String toString() {
      return super.toString() + "[fd=" + this.fd + "; addr=" + this.socketAddress + "; connected=" + this.connected + "; bound=" + this.bound + "]";
   }

   private static int expectInteger(Object value) throws SocketException {
      try {
         return (Integer)value;
      } catch (ClassCastException var2) {
         throw (SocketException)(new SocketException("Unsupported value: " + value)).initCause(var2);
      } catch (NullPointerException var3) {
         throw (SocketException)(new SocketException("Value must not be null")).initCause(var3);
      }
   }

   private static int expectBoolean(Object value) throws SocketException {
      try {
         return (Boolean)value ? 1 : 0;
      } catch (ClassCastException var2) {
         throw (SocketException)(new SocketException("Unsupported value: " + value)).initCause(var2);
      } catch (NullPointerException var3) {
         throw (SocketException)(new SocketException("Value must not be null")).initCause(var3);
      }
   }

   public Object getOption(int optID) throws SocketException {
      if (optID == 4) {
         return this.reuseAddr;
      } else {
         FileDescriptor fdesc = this.validFdOrException();

         try {
            switch(optID) {
            case 1:
            case 8:
               return org.newsclub.net.unix.NativeUnixSocket.getSocketOptionInt(fdesc, optID) != 0;
            case 128:
            case 4097:
            case 4098:
               return org.newsclub.net.unix.NativeUnixSocket.getSocketOptionInt(fdesc, optID);
            case 4102:
               return Math.max(this.timeout, Math.max(org.newsclub.net.unix.NativeUnixSocket.getSocketOptionInt(fdesc, 4101), org.newsclub.net.unix.NativeUnixSocket.getSocketOptionInt(fdesc, 4102)));
            default:
               throw new SocketException("Unsupported option: " + optID);
            }
         } catch (SocketException var4) {
            throw var4;
         } catch (Exception var5) {
            throw (SocketException)(new SocketException("Error while getting option")).initCause(var5);
         }
      }
   }

   public void setOption(int optID, Object value) throws SocketException {
      if (optID == 4) {
         this.reuseAddr = expectBoolean(value) != 0;
      } else {
         FileDescriptor fdesc = this.validFdOrException();

         try {
            switch(optID) {
            case 1:
            case 8:
               org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, optID, expectBoolean(value));
               return;
            case 128:
               if (value instanceof Boolean) {
                  boolean b = (Boolean)value;
                  if (b) {
                     throw new SocketException("Only accepting Boolean.FALSE here");
                  }

                  org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, optID, -1);
                  return;
               }

               org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, optID, expectInteger(value));
               return;
            case 4097:
            case 4098:
               org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, optID, expectInteger(value));
               return;
            case 4102:
               this.timeout = expectInteger(value);
               org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, 4101, this.timeout);
               org.newsclub.net.unix.NativeUnixSocket.setSocketOptionInt(fdesc, 4102, this.timeout);
               return;
            default:
               throw new SocketException("Unsupported option: " + optID);
            }
         } catch (SocketException var5) {
            throw var5;
         } catch (Exception var6) {
            throw (SocketException)(new SocketException("Error while setting option")).initCause(var6);
         }
      }
   }

   protected void shutdownInput() throws IOException {
      FileDescriptor fdesc = this.validFd();
      if (fdesc != null) {
         org.newsclub.net.unix.NativeUnixSocket.shutdown(fdesc, 0);
      }

   }

   protected void shutdownOutput() throws IOException {
      FileDescriptor fdesc = this.validFd();
      if (fdesc != null) {
         org.newsclub.net.unix.NativeUnixSocket.shutdown(fdesc, 1);
      }

   }

   org.newsclub.net.unix.AFUNIXSocketCredentials getPeerCredentials() throws IOException {
      return org.newsclub.net.unix.NativeUnixSocket.peerCredentials(this.fd, new org.newsclub.net.unix.AFUNIXSocketCredentials());
   }

   int getAncillaryReceiveBufferSize() {
      return this.ancillaryReceiveBuffer.capacity();
   }

   void setAncillaryReceiveBufferSize(int size) {
      this.ancillaryReceiveBuffer = ByteBuffer.allocateDirect(size);
   }

   FileDescriptor[] getReceivedFileDescriptors() {
      if (this.receivedFileDescriptors.isEmpty()) {
         return null;
      } else {
         List copy = new ArrayList(this.receivedFileDescriptors);
         if (copy.isEmpty()) {
            return null;
         } else {
            this.receivedFileDescriptors.removeAll(copy);
            int count = 0;

            FileDescriptor[] fds;
            for(Iterator var3 = copy.iterator(); var3.hasNext(); count += fds.length) {
               fds = (FileDescriptor[])var3.next();
            }

            if (count == 0) {
               return null;
            } else {
               FileDescriptor[] oneArray = new FileDescriptor[count];
               int offset = 0;

               for(Iterator var5 = copy.iterator(); var5.hasNext(); offset += fds.length) {
                  fds = (FileDescriptor[])var5.next();
                  System.arraycopy(fds, 0, oneArray, offset, fds.length);
               }

               return oneArray;
            }
         }
      }
   }

   void clearReceivedFileDescriptors() {
      this.receivedFileDescriptors.clear();
   }

   void receiveFileDescriptors(int[] fds) throws IOException {
      if (fds != null && fds.length != 0) {
         int fdsLength = fds.length;
         FileDescriptor[] descriptors = new FileDescriptor[fdsLength];

         for(int i = 0; i < fdsLength; ++i) {
            final FileDescriptor fdesc = new FileDescriptor();
            org.newsclub.net.unix.NativeUnixSocket.initFD(fdesc, fds[i]);
            descriptors[i] = fdesc;
            this.closeableFileDescriptors.put(fdesc, fds[i]);
            Closeable cleanup = new Closeable() {
               public void close() throws IOException {
                  AFUNIXSocketImpl.this.closeableFileDescriptors.remove(fdesc);
               }
            };
            org.newsclub.net.unix.NativeUnixSocket.attachCloseable(fdesc, cleanup);
         }

         this.receivedFileDescriptors.add(descriptors);
      }
   }

   void setOutboundFileDescriptors(int... fds) {
      this.pendingFileDescriptors = fds != null && fds.length != 0 ? fds : null;
   }

   static final class Lenient extends AFUNIXSocketImpl {
      public void setOption(int optID, Object value) throws SocketException {
         try {
            super.setOption(optID, value);
         } catch (SocketException var4) {
            switch(optID) {
            case 1:
               return;
            default:
               throw var4;
            }
         }
      }

      public Object getOption(int optID) throws SocketException {
         try {
            return super.getOption(optID);
         } catch (SocketException var3) {
            switch(optID) {
            case 1:
            case 8:
               return false;
            default:
               throw var3;
            }
         }
      }
   }

   private final class AFUNIXOutputStream extends OutputStream {
      private volatile boolean streamClosed;

      private AFUNIXOutputStream() {
         this.streamClosed = false;
      }

      public void write(int oneByte) throws IOException {
         byte[] buf1 = new byte[]{(byte)oneByte};
         this.write(buf1, 0, 1);
      }

      public void write(byte[] buf, int off, int len) throws IOException {
         if (this.streamClosed) {
            throw new SocketException("This OutputStream has already been closed.");
         } else if (len >= 0 && off >= 0 && len <= buf.length - off) {
            FileDescriptor fdesc = AFUNIXSocketImpl.this.validFdOrException();

            int written;
            for(int writtenTotal = 0; len > 0; writtenTotal += written) {
               if (Thread.interrupted()) {
                  InterruptedIOException ex = new InterruptedIOException("Thread interrupted during write");
                  ex.bytesTransferred = writtenTotal;
                  Thread.currentThread().interrupt();
                  throw ex;
               }

               written = org.newsclub.net.unix.NativeUnixSocket.write(AFUNIXSocketImpl.this, fdesc, buf, off, len, AFUNIXSocketImpl.this.pendingFileDescriptors);
               if (written < 0) {
                  throw new IOException("Unspecific error while writing");
               }

               len -= written;
               off += written;
            }

         } else {
            throw new IndexOutOfBoundsException();
         }
      }

      public synchronized void close() throws IOException {
         if (!this.streamClosed) {
            this.streamClosed = true;
            FileDescriptor fdesc = AFUNIXSocketImpl.this.validFd();
            if (fdesc != null) {
               org.newsclub.net.unix.NativeUnixSocket.shutdown(fdesc, 1);
            }

            AFUNIXSocketImpl.this.closedOutputStream = true;
            AFUNIXSocketImpl.this.checkClose();
         }
      }

      // $FF: synthetic method
      AFUNIXOutputStream(Object x1) {
         this();
      }
   }

   private final class AFUNIXInputStream extends InputStream {
      private volatile boolean streamClosed;

      private AFUNIXInputStream() {
         this.streamClosed = false;
      }

      public int read(byte[] buf, int off, int len) throws IOException {
         if (this.streamClosed) {
            throw new IOException("This InputStream has already been closed.");
         } else {
            FileDescriptor fdesc = AFUNIXSocketImpl.this.validFdOrException();
            if (len == 0) {
               return 0;
            } else if (off >= 0 && len >= 0 && len <= buf.length - off) {
               return org.newsclub.net.unix.NativeUnixSocket.read(AFUNIXSocketImpl.this, fdesc, buf, off, len, AFUNIXSocketImpl.this.ancillaryReceiveBuffer);
            } else {
               throw new IndexOutOfBoundsException();
            }
         }
      }

      public int read() throws IOException {
         byte[] buf1 = new byte[1];
         int numRead = this.read(buf1, 0, 1);
         return numRead <= 0 ? -1 : buf1[0] & 255;
      }

      public synchronized void close() throws IOException {
         this.streamClosed = true;
         FileDescriptor fdesc = AFUNIXSocketImpl.this.validFd();
         if (fdesc != null) {
            org.newsclub.net.unix.NativeUnixSocket.shutdown(fdesc, 0);
         }

         AFUNIXSocketImpl.this.closedInputStream = true;
         AFUNIXSocketImpl.this.checkClose();
      }

      public int available() throws IOException {
         if (this.streamClosed) {
            throw new IOException("This InputStream has already been closed.");
         } else {
            FileDescriptor fdesc = AFUNIXSocketImpl.this.validFdOrException();
            return org.newsclub.net.unix.NativeUnixSocket.available(fdesc);
         }
      }

      // $FF: synthetic method
      AFUNIXInputStream(Object x1) {
         this();
      }
   }
}
