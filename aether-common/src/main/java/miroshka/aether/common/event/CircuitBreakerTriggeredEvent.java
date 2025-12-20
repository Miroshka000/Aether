package miroshka.aether.common.event;

import java.util.Objects;

public record CircuitBreakerTriggeredEvent(
        int dropPriorityThreshold,
        long durationMillis,
        String reason,
        long timestamp) implements AetherEvent {

    public CircuitBreakerTriggeredEvent {
        Objects.requireNonNull(reason, "reason");
    }

    public static CircuitBreakerTriggeredEvent create(int threshold, long duration, String reason) {
        return new CircuitBreakerTriggeredEvent(threshold, duration, reason, System.currentTimeMillis());
    }
}
