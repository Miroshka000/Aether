package miroshka.aether.api;

import java.util.Map;
import java.util.Objects;

public record NetworkStateChangedEvent(
        int globalOnline,
        int serverCount,
        long stateVersion,
        Map<String, String> globalProperties,
        Map<String, String> routingHints,
        long timestamp) implements AetherPublicEvent {

    public NetworkStateChangedEvent {
        Objects.requireNonNull(globalProperties, "globalProperties");
        Objects.requireNonNull(routingHints, "routingHints");
    }
}
