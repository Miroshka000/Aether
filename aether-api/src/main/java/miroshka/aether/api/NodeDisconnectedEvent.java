package miroshka.aether.api;

import java.util.Objects;

public record NodeDisconnectedEvent(
        String nodeId,
        String reason,
        long timestamp) implements AetherPublicEvent {

    public NodeDisconnectedEvent {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(reason, "reason");
    }
}
