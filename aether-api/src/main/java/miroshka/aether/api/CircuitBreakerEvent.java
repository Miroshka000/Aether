package miroshka.aether.api;

import java.util.Objects;

public record CircuitBreakerEvent(
        boolean tripped,
        String reason,
        long durationMillis,
        long timestamp) implements AetherPublicEvent {

    public CircuitBreakerEvent {
        Objects.requireNonNull(reason, "reason");
    }

    public static CircuitBreakerEvent tripped(String reason, long durationMillis) {
        return new CircuitBreakerEvent(true, reason, durationMillis, System.currentTimeMillis());
    }

    public static CircuitBreakerEvent reset() {
        return new CircuitBreakerEvent(false, "Reset", 0, System.currentTimeMillis());
    }
}
