package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public record HeartbeatPacket(
        long timestamp,
        int sequenceNumber) implements Packet {

    @Override
    public int packetId() {
        return PacketIds.HEARTBEAT;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeLong(buffer, timestamp);
        PacketHelper.writeInt(buffer, sequenceNumber);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static HeartbeatPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        long timestamp = PacketHelper.readLong(buffer);
        int sequenceNumber = PacketHelper.readInt(buffer);
        return new HeartbeatPacket(timestamp, sequenceNumber);
    }

    public static HeartbeatPacket create(int sequenceNumber) {
        return new HeartbeatPacket(System.currentTimeMillis(), sequenceNumber);
    }
}
