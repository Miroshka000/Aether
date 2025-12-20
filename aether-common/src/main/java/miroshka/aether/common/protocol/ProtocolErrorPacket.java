package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public record ProtocolErrorPacket(
        int failedPacketId,
        String errorCode,
        String details) implements Packet {

    public ProtocolErrorPacket {
        Objects.requireNonNull(errorCode, "errorCode");
        Objects.requireNonNull(details, "details");
    }

    @Override
    public int packetId() {
        return PacketIds.PROTOCOL_ERROR;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeInt(buffer, failedPacketId);
        PacketHelper.writeString(buffer, errorCode);
        PacketHelper.writeString(buffer, details);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static ProtocolErrorPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int failedPacketId = PacketHelper.readInt(buffer);
        String errorCode = PacketHelper.readString(buffer);
        String details = PacketHelper.readString(buffer);
        return new ProtocolErrorPacket(failedPacketId, errorCode, details);
    }

    public static ProtocolErrorPacket malformedPacket(int packetId, String details) {
        return new ProtocolErrorPacket(packetId, "MALFORMED_PACKET", details);
    }

    public static ProtocolErrorPacket unknownPacket(int packetId) {
        return new ProtocolErrorPacket(packetId, "UNKNOWN_PACKET", "Unknown packet ID: " + packetId);
    }

    public static ProtocolErrorPacket internalError(int packetId, String details) {
        return new ProtocolErrorPacket(packetId, "INTERNAL_ERROR", details);
    }
}
