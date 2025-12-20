package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.Objects;

public record AuthResultPacket(
        boolean success,
        int negotiatedVersion,
        String reason,
        Map<String, String> serverConfig) implements Packet {

    public AuthResultPacket {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(serverConfig, "serverConfig");
    }

    @Override
    public int packetId() {
        return PacketIds.AUTH_RESULT;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeBoolean(buffer, success);
        PacketHelper.writeInt(buffer, negotiatedVersion);
        PacketHelper.writeString(buffer, reason);
        PacketHelper.writePropertyMap(buffer, serverConfig);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static AuthResultPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        boolean success = PacketHelper.readBoolean(buffer);
        int negotiatedVersion = PacketHelper.readInt(buffer);
        String reason = PacketHelper.readString(buffer);
        Map<String, String> serverConfig = PacketHelper.readPropertyMap(buffer);
        return new AuthResultPacket(success, negotiatedVersion, reason, serverConfig);
    }

    public static AuthResultPacket success(int negotiatedVersion, Map<String, String> serverConfig) {
        return new AuthResultPacket(true, negotiatedVersion, "OK", serverConfig);
    }

    public static AuthResultPacket failure(String reason) {
        return new AuthResultPacket(false, 0, reason, Map.of());
    }
}
