package miroshka.aether.common.event;

import miroshka.aether.common.protocol.NetworkStatePacket;

import java.util.Objects;

public record NetworkStateUpdatedEvent(
        NetworkStatePacket state,
        long timestamp) implements AetherEvent {

    public NetworkStateUpdatedEvent {
        Objects.requireNonNull(state, "state");
    }

    public static NetworkStateUpdatedEvent create(NetworkStatePacket state) {
        return new NetworkStateUpdatedEvent(state, System.currentTimeMillis());
    }
}
