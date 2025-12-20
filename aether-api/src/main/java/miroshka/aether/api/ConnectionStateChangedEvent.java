package miroshka.aether.api;

import java.util.Objects;

public record ConnectionStateChangedEvent(
        ConnectionStatus.ConnectionState previousState,
        ConnectionStatus.ConnectionState newState,
        long timestamp) implements AetherPublicEvent {

    public ConnectionStateChangedEvent {
        Objects.requireNonNull(previousState, "previousState");
        Objects.requireNonNull(newState, "newState");
    }
}
