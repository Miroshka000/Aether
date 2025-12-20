package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public record CircuitBreakerTrippedPacket(
        int dropPriorityThreshold,
        long durationMillis,
        String reason) implements Packet {

    public CircuitBreakerTrippedPacket {
        Objects.requireNonNull(reason, "reason");
    }

    @Override
    public int packetId() {
        return PacketIds.CIRCUIT_BREAKER_TRIPPED;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeInt(buffer, dropPriorityThreshold);
        PacketHelper.writeLong(buffer, durationMillis);
        PacketHelper.writeString(buffer, reason);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static CircuitBreakerTrippedPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int dropPriorityThreshold = PacketHelper.readInt(buffer);
        long durationMillis = PacketHelper.readLong(buffer);
        String reason = PacketHelper.readString(buffer);
        return new CircuitBreakerTrippedPacket(dropPriorityThreshold, durationMillis, reason);
    }
}
