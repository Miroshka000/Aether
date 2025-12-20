package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.Objects;

public record NodeSnapshotPacket(
        int onlinePlayers,
        int maxPlayers,
        double tps,
        long captureTimestamp,
        Map<String, String> extraData) implements Packet {

    public NodeSnapshotPacket {
        Objects.requireNonNull(extraData, "extraData");
    }

    @Override
    public int packetId() {
        return PacketIds.NODE_SNAPSHOT;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeInt(buffer, onlinePlayers);
        PacketHelper.writeInt(buffer, maxPlayers);
        PacketHelper.writeDouble(buffer, tps);
        PacketHelper.writeLong(buffer, captureTimestamp);
        PacketHelper.writePropertyMap(buffer, extraData);
    }

    @Override
    public Priority priority() {
        return Priority.NORMAL;
    }

    public static NodeSnapshotPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int onlinePlayers = PacketHelper.readInt(buffer);
        int maxPlayers = PacketHelper.readInt(buffer);
        double tps = PacketHelper.readDouble(buffer);
        long captureTimestamp = PacketHelper.readLong(buffer);
        Map<String, String> extraData = PacketHelper.readPropertyMap(buffer);
        return new NodeSnapshotPacket(onlinePlayers, maxPlayers, tps, captureTimestamp, extraData);
    }
}
