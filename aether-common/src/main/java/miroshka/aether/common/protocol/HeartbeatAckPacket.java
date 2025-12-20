package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public record HeartbeatAckPacket(
        long originalTimestamp,
        int sequenceNumber,
        long processingDelayMicros) implements Packet {

    @Override
    public int packetId() {
        return PacketIds.HEARTBEAT_ACK;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeLong(buffer, originalTimestamp);
        PacketHelper.writeInt(buffer, sequenceNumber);
        PacketHelper.writeLong(buffer, processingDelayMicros);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static HeartbeatAckPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        long originalTimestamp = PacketHelper.readLong(buffer);
        int sequenceNumber = PacketHelper.readInt(buffer);
        long processingDelayMicros = PacketHelper.readLong(buffer);
        return new HeartbeatAckPacket(originalTimestamp, sequenceNumber, processingDelayMicros);
    }

    public static HeartbeatAckPacket fromHeartbeat(HeartbeatPacket heartbeat, long processingDelayMicros) {
        Objects.requireNonNull(heartbeat, "heartbeat");
        return new HeartbeatAckPacket(heartbeat.timestamp(), heartbeat.sequenceNumber(), processingDelayMicros);
    }
}
