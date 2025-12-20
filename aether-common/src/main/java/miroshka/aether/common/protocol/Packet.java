package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

public sealed interface Packet permits
        AuthHandshakePacket,
        AuthResultPacket,
        HeartbeatPacket,
        HeartbeatAckPacket,
        NodeSnapshotPacket,
        NetworkStatePacket,
        MetricsReportPacket,
        CircuitBreakerTrippedPacket,
        ProtocolErrorPacket,
        TransferRequestPacket,
        PortalSyncPacket,
        EventBroadcastPacket,
        PDCSyncPacket,
        ChunkDataPacket {

    int packetId();

    void encode(ByteBuf buffer);

    Priority priority();

    enum Priority {
        LOW,
        NORMAL,
        CRITICAL
    }
}
