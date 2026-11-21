package dev.foabic.neoforge;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Kept separate so a dedicated server never loads the client packet class. */
final class NeoForgeClientNetwork {

    private NeoForgeClientNetwork() {
    }

    static void send(NeoForgeNetwork.Payload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
