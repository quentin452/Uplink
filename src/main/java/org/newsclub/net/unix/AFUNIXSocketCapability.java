package org.newsclub.net.unix;

public enum AFUNIXSocketCapability {
   CAPABILITY_PEER_CREDENTIALS(0),
   CAPABILITY_ANCILLARY_MESSAGES(1),
   CAPABILITY_FILE_DESCRIPTORS(2),
   CAPABILITY_ABSTRACT_NAMESPACE(3);

   private final int bitmask;

   private AFUNIXSocketCapability(int bit) {
      this.bitmask = 1 << bit;
   }

   int getBitmask() {
      return this.bitmask;
   }
}
