package org.newsclub.net.unix;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"})
final class NativeLibraryLoader implements Closeable {
   private static final String PROP_LIBRARY_OVERRIDE = "org.newsclub.net.unix.library.override";
   private static final String PROP_LIBRARY_TMPDIR = "org.newsclub.net.unix.library.tmpdir";
   private static final File TEMP_DIR;
   private static final String ARCHITECTURE_AND_OS = architectureAndOS();
   private static final String LIBRARY_NAME = "junixsocket-native";
   private static boolean loaded = false;

   private List tryProviderClass(String providerClassname, String artifactName) throws IOException, ClassNotFoundException {
      Class providerClass = Class.forName(providerClassname);
      String version = this.getArtifactVersion(providerClass, artifactName);
      String libraryNameAndVersion = "junixsocket-native-" + version;
      return this.findLibraryCandidates(artifactName, libraryNameAndVersion, providerClass);
   }

   private String getArtifactVersion(Class providerClass, String... artifactNames) throws IOException {
      int var4 = artifactNames.length;
      byte var5 = 0;
      if (var5 < var4) {
         String artifactName = artifactNames[var5];
         Properties p = new Properties();
         String resource = "/META-INF/maven/com.kohlschutter.junixsocket/" + artifactName + "/pom.properties";
         InputStream in = providerClass.getResourceAsStream(resource);

         String var11;
         try {
            if (in == null) {
               throw new FileNotFoundException("Could not find resource " + resource + " relative to " + providerClass);
            }

            p.load(in);
            String version = p.getProperty("version");
            Objects.requireNonNull(version, "Could not read version from pom.properties");
            var11 = version;
         } catch (Throwable var13) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (in != null) {
            in.close();
         }

         return var11;
      } else {
         throw new IllegalStateException("No artifact names specified");
      }
   }

   private synchronized void setLoaded(String library) {
      if (!loaded) {
         loaded = true;
         AFUNIXSocket.loadedLibrary = library;

         try {
            NativeUnixSocket.init();
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Exception var4) {
            throw new IllegalStateException(var4);
         }
      }

   }

   public synchronized void loadLibrary() {
      synchronized(this.getClass().getClassLoader()) {
         if (!loaded) {
            String libraryOverride = System.getProperty("org.newsclub.net.unix.library.override", "");
            if (!libraryOverride.isEmpty()) {
               System.load(libraryOverride);
               this.setLoaded(libraryOverride);
            } else {
               List candidates = new ArrayList();
               ArrayList suppressedThrowables = new ArrayList();

               try {
                  candidates.add(new NativeLibraryLoader.StandardLibraryCandidate(this.getArtifactVersion(this.getClass(), "junixsocket-common", "junixsocket-core")));
               } catch (Exception var14) {
                  suppressedThrowables.add(var14);
               }

               try {
                  candidates.addAll(this.tryProviderClass("org.newsclub.lib.junixsocket.custom.NarMetadata", "junixsocket-native-custom"));
               } catch (Exception var13) {
                  suppressedThrowables.add(var13);
               }

               try {
                  candidates.addAll(this.tryProviderClass("org.newsclub.lib.junixsocket.common.NarMetadata", "junixsocket-native-common"));
               } catch (Exception var12) {
                  suppressedThrowables.add(var12);
               }

               String loadedLibraryId = null;
               Iterator var6 = candidates.iterator();

               NativeLibraryLoader.LibraryCandidate candidate;
               while(var6.hasNext()) {
                  candidate = (NativeLibraryLoader.LibraryCandidate)var6.next();

                  try {
                     if ((loadedLibraryId = candidate.load()) != null) {
                        break;
                     }
                  } catch (LinkageError | Exception var15) {
                     suppressedThrowables.add(var15);
                  }
               }

               var6 = candidates.iterator();

               while(var6.hasNext()) {
                  candidate = (NativeLibraryLoader.LibraryCandidate)var6.next();
                  candidate.close();
               }

               if (loadedLibraryId != null) {
                  this.setLoaded(loadedLibraryId);
               } else {
                  String message = "Could not load native library junixsocket-native for architecture " + ARCHITECTURE_AND_OS;
                  String cp = System.getProperty("java.class.path", "");
                  if (cp.contains("junixsocket-native-custom/target-eclipse") || cp.contains("junixsocket-native-common/target-eclipse")) {
                     message = message + "\n\n*** ECLIPSE USERS ***\nIf you're running from within Eclipse, please close the projects \"junixsocket-native-common\" and \"junixsocket-native-custom\"\n";
                  }

                  UnsatisfiedLinkError e = new UnsatisfiedLinkError(message);
                  Iterator var9 = suppressedThrowables.iterator();

                  while(var9.hasNext()) {
                     Throwable suppressed = (Throwable)var9.next();
                     e.addSuppressed(suppressed);
                  }

                  throw e;
               }
            }
         }
      }
   }

   private static String architectureAndOS() {
      return System.getProperty("os.arch") + "-" + System.getProperty("os.name").replaceAll(" ", "");
   }

   private List findLibraryCandidates(String artifactName, String libraryNameAndVersion, Class providerClass) {
      String mappedName = System.mapLibraryName(libraryNameAndVersion);
      List list = new ArrayList();
      String[] var6 = new String[]{"gpp", "g++", "linker", "clang", "gcc", "cc", "CC", "icpc", "icc", "xlC", "xlC_r", "msvc", "icl", "ecpc", "ecc"};
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String compiler = var6[var8];
         String path = "/lib/" + ARCHITECTURE_AND_OS + "-" + compiler + "/jni/" + mappedName;
         InputStream in = providerClass.getResourceAsStream(path);
         if (in != null) {
            list.add(new NativeLibraryLoader.ClasspathLibraryCandidate(artifactName, libraryNameAndVersion, path, in));
         }

         String nodepsPath = this.nodepsPath(path);
         if (nodepsPath != null) {
            in = providerClass.getResourceAsStream(nodepsPath);
            if (in != null) {
               list.add(new NativeLibraryLoader.ClasspathLibraryCandidate(artifactName, libraryNameAndVersion, nodepsPath, in));
            }
         }
      }

      return list;
   }

   private String nodepsPath(String path) {
      int lastDot = path.lastIndexOf(46);
      return lastDot == -1 ? null : path.substring(0, lastDot) + ".nodeps" + path.substring(lastDot);
   }

   private static File createTempFile(String prefix, String suffix) throws IOException {
      return File.createTempFile(prefix, suffix, TEMP_DIR);
   }

   public void close() {
   }

   static {
      String dir = System.getProperty("org.newsclub.net.unix.library.tmpdir", (String)null);
      TEMP_DIR = dir == null ? null : new File(dir);
   }

   private static final class ClasspathLibraryCandidate extends NativeLibraryLoader.LibraryCandidate {
      private final String artifactName;
      private final InputStream libraryIn;
      private final String path;

      ClasspathLibraryCandidate(String artifactName, String libraryNameAndVersion, String path, InputStream libraryIn) {
         super(libraryNameAndVersion);
         this.artifactName = artifactName;
         this.path = path;
         this.libraryIn = libraryIn;
      }

      synchronized String load() throws IOException, LinkageError {
         if (this.libraryNameAndVersion == null) {
            return null;
         } else {
            File libFile;
            try {
               libFile = NativeLibraryLoader.createTempFile("libtmp", System.mapLibraryName(this.libraryNameAndVersion));

               try {
                  FileOutputStream out = new FileOutputStream(libFile);

                  try {
                     byte[] buf = new byte[4096];

                     int read;
                     while((read = this.libraryIn.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                     }
                  } catch (Throwable var11) {
                     try {
                        out.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }

                     throw var11;
                  }

                  out.close();
               } finally {
                  this.libraryIn.close();
               }
            } catch (IOException var13) {
               throw var13;
            }

            System.load(libFile.getAbsolutePath());
            if (!libFile.delete()) {
               libFile.deleteOnExit();
            }

            return this.artifactName + "/" + this.libraryNameAndVersion;
         }
      }

      public void close() {
         try {
            this.libraryIn.close();
         } catch (IOException var2) {
         }

      }

      public String toString() {
         return super.toString() + "(" + this.artifactName + ":" + this.path + ")";
      }
   }

   private static final class StandardLibraryCandidate extends NativeLibraryLoader.LibraryCandidate {
      StandardLibraryCandidate(String version) {
         super(version == null ? null : "junixsocket-native-" + version);
      }

      String load() throws Exception, LinkageError {
         if (this.libraryNameAndVersion != null) {
            System.loadLibrary(this.libraryNameAndVersion);
            return this.libraryNameAndVersion;
         } else {
            return null;
         }
      }

      public void close() {
      }

      public String toString() {
         return super.toString() + "(standard library path)";
      }
   }

   private abstract static class LibraryCandidate implements Closeable {
      protected final String libraryNameAndVersion;

      protected LibraryCandidate(String libraryNameAndVersion) {
         this.libraryNameAndVersion = libraryNameAndVersion;
      }

      abstract String load() throws Exception;

      public abstract void close();

      public String toString() {
         return super.toString() + "[" + this.libraryNameAndVersion + "]";
      }
   }
}
