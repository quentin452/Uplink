package com.jagrosh.discordipc;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;

public interface IPCListener {
   default void onPacketSent(IPCClient client, Packet packet) {
   }

   default void onPacketReceived(IPCClient client, Packet packet) {
   }

   default void onActivityJoin(IPCClient client, String secret) {
   }

   default void onActivitySpectate(IPCClient client, String secret) {
   }

   default void onActivityJoinRequest(IPCClient client, String secret, User user) {
   }

   default void onReady() {
   }

   default void onClose(IPCClient client, JsonObject json) {
   }

   default void onDisconnect(IPCClient client, Throwable t) {
   }
}
