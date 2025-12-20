package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public record AuthHandshakePacket(
        int protocolVersion,
        String serverName,
        String secretKey,
        long timestamp) implements Packet {

    public AuthHandshakePacket {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(secretKey, "secretKey");
    }

    @Override
    public int packetId() {
        return PacketIds.AUTH_HANDSHAKE;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeInt(buffer, protocolVersion);
        PacketHelper.writeString(buffer, serverName);
        PacketHelper.writeString(buffer, secretKey);
        PacketHelper.writeLong(buffer, timestamp);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static AuthHandshakePacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int protocolVersion = PacketHelper.readInt(buffer);
        String serverName = PacketHelper.readString(buffer);
        String secretKey = PacketHelper.readString(buffer);
        long timestamp = PacketHelper.readLong(buffer);
        return new AuthHandshakePacket(protocolVersion, serverName, secretKey, timestamp);
    }
}
