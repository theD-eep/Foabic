package dev.foabic.neoforge;

import dev.foabic.api.network.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge payload-handler transport for {@link NetworkManager}. */
final class NeoForgeNetwork implements NetworkManager.Transport {

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

    private NeoForgeNetwork() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, NeoForgeNetwork::onRegisterPayloads);
        NetworkManager.bind(new NeoForgeNetwork());
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(TYPE, CODEC, (payload, context) ->
                context.enqueueWork(() -> NetworkManager.receive(payload.type, payload.data)));
    }

    @Override
    public void sendToServer(String type, byte[] data) {
        NeoForgeClientNetwork.send(new Payload(type, data));
    }

    @Override
    public void sendToClient(Object player, String type, byte[] data) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            throw new IllegalArgumentException("Foabic sendToClient expects a ServerPlayer, got "
                    + (player == null ? "null" : player.getClass().getName()));
        }
        PacketDistributor.sendToPlayer(serverPlayer, new Payload(type, data));
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
}
