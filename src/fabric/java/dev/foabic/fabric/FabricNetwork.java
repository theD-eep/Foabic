package dev.foabic.fabric;

import dev.foabic.api.network.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric transport for {@link NetworkManager}.
 *
 * <p>All Foabic packets share one play payload id and carry their packet class
 * name plus opaque bytes. Packet registration therefore does not need a new
 * channel for every message.
 */
final class FabricNetwork implements NetworkManager.Transport {

    private static final String CHANNEL = "packet";
    private static final int MAX_TYPE = 1024;
    private static final int MAX_DATA = 1024 * 1024;
    private static final CustomPacketPayload.Type<Payload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("foabic", CHANNEL));
    private static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUtf(payload.type, MAX_TYPE);
                        buffer.writeByteArray(payload.data);
                    },
                    buffer -> new Payload(buffer.readUtf(MAX_TYPE), buffer.readByteArray(MAX_DATA)));

    private FabricNetwork() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
                context.server().execute(() ->
                        NetworkManager.receive(payload.type, payload.data)));
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientNetwork.registerReceiver();
        }
        NetworkManager.bind(new FabricNetwork());
    }

    @Override
    public void sendToServer(String type, byte[] data) {
        FabricClientNetwork.send(new Payload(type, data));
    }

    @Override
    public void sendToClient(Object player, String type, byte[] data) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            throw new IllegalArgumentException("Foabic sendToClient expects a ServerPlayer, got "
                    + (player == null ? "null" : player.getClass().getName()));
        }
        ServerPlayNetworking.send(serverPlayer, new Payload(type, data));
    }

    static final class Payload implements CustomPacketPayload {
        private final String type;
        private final byte[] data;

        private Payload(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Loaded only on a client. */
    static final class FabricClientNetwork {
        private FabricClientNetwork() {
        }

        static void registerReceiver() {
            ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
                    context.client().execute(() ->
                            NetworkManager.receive(payload.type, payload.data)));
        }

        static void send(Payload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
