package miroshka.aether.common.event;

import miroshka.aether.common.protocol.Packet;

import java.util.Objects;

public record PacketReceivedEvent(
        Packet packet,
        String sourceId,
        long timestamp) implements AetherEvent {

    public PacketReceivedEvent {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(sourceId, "sourceId");
    }

    public static PacketReceivedEvent create(Packet packet, String sourceId) {
        return new PacketReceivedEvent(packet, sourceId, System.currentTimeMillis());
    }
}
