package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class PacketRegistry {

    private static final PacketRegistry INSTANCE = new PacketRegistry();

    private final Map<Integer, Function<ByteBuf, ? extends Packet>> decoders;
    private final Map<Class<? extends Packet>, Integer> packetIdMap;

    private PacketRegistry() {
        this.decoders = new HashMap<>();
        this.packetIdMap = new HashMap<>();
        registerDefaults();
    }

    public static PacketRegistry instance() {
        return INSTANCE;
    }

    private void registerDefaults() {
        register(PacketIds.AUTH_HANDSHAKE, AuthHandshakePacket.class, AuthHandshakePacket::decode);
        register(PacketIds.AUTH_RESULT, AuthResultPacket.class, AuthResultPacket::decode);
        register(PacketIds.HEARTBEAT, HeartbeatPacket.class, HeartbeatPacket::decode);
        register(PacketIds.HEARTBEAT_ACK, HeartbeatAckPacket.class, HeartbeatAckPacket::decode);
        register(PacketIds.NODE_SNAPSHOT, NodeSnapshotPacket.class, NodeSnapshotPacket::decode);
        register(PacketIds.NETWORK_STATE, NetworkStatePacket.class, NetworkStatePacket::decode);
        register(PacketIds.METRICS_REPORT, MetricsReportPacket.class, MetricsReportPacket::decode);
        register(PacketIds.CIRCUIT_BREAKER_TRIPPED, CircuitBreakerTrippedPacket.class,
                CircuitBreakerTrippedPacket::decode);
        register(PacketIds.PROTOCOL_ERROR, ProtocolErrorPacket.class, ProtocolErrorPacket::decode);
        register(PacketIds.TRANSFER_REQUEST, TransferRequestPacket.class, TransferRequestPacket::decode);
        register(PacketIds.PORTAL_SYNC, PortalSyncPacket.class, PortalSyncPacket::decode);
        register(PacketIds.EVENT_BROADCAST, EventBroadcastPacket.class, EventBroadcastPacket::decode);
        register(PacketIds.PDC_SYNC, PDCSyncPacket.class, PDCSyncPacket::decode);
        register(PacketIds.CHUNK_DATA, ChunkDataPacket.class, ChunkDataPacket::decode);
    }

    private <T extends Packet> void register(int packetId, Class<T> packetClass, Function<ByteBuf, T> decoder) {
        decoders.put(packetId, decoder);
        packetIdMap.put(packetClass, packetId);
    }

    public Optional<Packet> decode(int packetId, ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        Function<ByteBuf, ? extends Packet> decoder = decoders.get(packetId);
        if (decoder == null) {
            return Optional.empty();
        }
        return Optional.of(decoder.apply(buffer));
    }

    public Optional<Integer> getPacketId(Class<? extends Packet> packetClass) {
        Objects.requireNonNull(packetClass, "packetClass");
        return Optional.ofNullable(packetIdMap.get(packetClass));
    }

    public boolean isRegistered(int packetId) {
        return decoders.containsKey(packetId);
    }
}
