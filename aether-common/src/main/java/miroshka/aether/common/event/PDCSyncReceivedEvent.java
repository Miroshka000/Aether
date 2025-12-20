package miroshka.aether.common.event;

import miroshka.aether.common.protocol.PDCSyncPacket;

import java.util.Objects;

public record PDCSyncReceivedEvent(PDCSyncPacket packet) implements AetherEvent {

    public PDCSyncReceivedEvent {
        Objects.requireNonNull(packet, "packet");
    }

    public static PDCSyncReceivedEvent of(PDCSyncPacket packet) {
        return new PDCSyncReceivedEvent(packet);
    }
}
