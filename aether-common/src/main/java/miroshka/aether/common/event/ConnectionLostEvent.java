package miroshka.aether.common.event;

import java.util.Objects;

public record ConnectionLostEvent(
        String nodeId,
        String reason,
        long timestamp) implements AetherEvent {

    public ConnectionLostEvent {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(reason, "reason");
    }

    public static ConnectionLostEvent create(String nodeId, String reason) {
        return new ConnectionLostEvent(nodeId, reason, System.currentTimeMillis());
    }
}
