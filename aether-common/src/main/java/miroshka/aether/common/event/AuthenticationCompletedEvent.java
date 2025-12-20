package miroshka.aether.common.event;

import java.util.Objects;

public record AuthenticationCompletedEvent(
        String nodeId,
        boolean success,
        String reason,
        long timestamp) implements AetherEvent {

    public AuthenticationCompletedEvent {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(reason, "reason");
    }

    public static AuthenticationCompletedEvent success(String nodeId) {
        return new AuthenticationCompletedEvent(nodeId, true, "OK", System.currentTimeMillis());
    }

    public static AuthenticationCompletedEvent failure(String nodeId, String reason) {
        return new AuthenticationCompletedEvent(nodeId, false, reason, System.currentTimeMillis());
    }
}
