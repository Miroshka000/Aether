package miroshka.aether.api;

public sealed interface AetherPublicEvent permits
        NetworkStateChangedEvent,
        NodeConnectedEvent,
        NodeDisconnectedEvent,
        CircuitBreakerEvent,
        ConnectionStateChangedEvent {
}
