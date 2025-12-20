package miroshka.aether.api;

import java.util.Objects;

public record NodeConnectedEvent(
        String nodeId,
        long timestamp) implements AetherPublicEvent {

    public NodeConnectedEvent {
        Objects.requireNonNull(nodeId, "nodeId");
    }
}
