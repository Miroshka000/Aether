package miroshka.aether.common.event;

import java.util.Objects;

public record ConnectionEstablishedEvent(
        String nodeId,
        String remoteAddress,
        long timestamp) implements AetherEvent {

    public ConnectionEstablishedEvent {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(remoteAddress, "remoteAddress");
    }

    public static ConnectionEstablishedEvent create(String nodeId, String remoteAddress) {
        return new ConnectionEstablishedEvent(nodeId, remoteAddress, System.currentTimeMillis());
    }
}
