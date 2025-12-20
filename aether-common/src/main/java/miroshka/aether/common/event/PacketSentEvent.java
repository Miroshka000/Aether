package miroshka.aether.common.event;

import miroshka.aether.common.protocol.Packet;

import java.util.Objects;

public record PacketSentEvent(
        Packet packet,
        String targetId,
        long timestamp) implements AetherEvent {

    public PacketSentEvent {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(targetId, "targetId");
    }

    public static PacketSentEvent create(Packet packet, String targetId) {
        return new PacketSentEvent(packet, targetId, System.currentTimeMillis());
    }
}
