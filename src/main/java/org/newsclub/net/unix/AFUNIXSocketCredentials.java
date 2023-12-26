package org.newsclub.net.unix;

import java.util.Arrays;
import java.util.UUID;

public final class AFUNIXSocketCredentials {
   private long pid = -1L;
   private long uid = -1L;
   private long[] gids = null;
   private UUID uuid = null;

   AFUNIXSocketCredentials() {
   }

   public long getPid() {
      return this.pid;
   }

   public long getUid() {
      return this.uid;
   }

   public long getGid() {
      return this.gids == null ? -1L : (this.gids.length == 0 ? -1L : this.gids[0]);
   }

   public long[] getGids() {
      return this.gids == null ? null : (long[])this.gids.clone();
   }

   public UUID getUUID() {
      return this.uuid;
   }

   void setUUID(String uuidStr) {
      this.uuid = UUID.fromString(uuidStr);
   }

   void setGids(long[] gids) {
      this.gids = (long[])gids.clone();
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append('[');
      if (this.uid != -1L) {
         sb.append("uid=" + this.uid + ";");
      }

      if (this.gids != null) {
         sb.append("gids=" + Arrays.toString(this.gids) + ";");
      }

      if (this.pid != -1L) {
         sb.append("pid=" + this.pid + ";");
      }

      if (this.uuid != null) {
         sb.append("uuid=" + this.uuid + ";");
      }

      if (sb.charAt(sb.length() - 1) == ';') {
         sb.setLength(sb.length() - 1);
      }

      sb.append(']');
      return sb.toString();
   }

   public int hashCode() {
      int result = 1;
      result = 31 * result + Arrays.hashCode(this.gids);
      result = 31 * result + (int)(this.pid ^ this.pid >>> 32);
      result = 31 * result + (int)(this.uid ^ this.uid >>> 32);
      result = 31 * result + (this.uuid == null ? 0 : this.uuid.hashCode());
      return result;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         AFUNIXSocketCredentials other = (AFUNIXSocketCredentials)obj;
         if (!Arrays.equals(this.gids, other.gids)) {
            return false;
         } else if (this.pid != other.pid) {
            return false;
         } else if (this.uid != other.uid) {
            return false;
         } else {
            if (this.uuid == null) {
               if (other.uuid != null) {
                  return false;
               }
            } else if (!this.uuid.equals(other.uuid)) {
               return false;
            }

            return true;
         }
      }
   }
}
