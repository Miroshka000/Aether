package miroshka.aether.common.event;

import miroshka.aether.common.protocol.EventBroadcastPacket;

import java.util.Objects;

public record EventBroadcastReceivedEvent(EventBroadcastPacket packet) implements AetherEvent {

    public EventBroadcastReceivedEvent {
        Objects.requireNonNull(packet, "packet");
    }

    public static EventBroadcastReceivedEvent of(EventBroadcastPacket packet) {
        return new EventBroadcastReceivedEvent(packet);
    }
}
