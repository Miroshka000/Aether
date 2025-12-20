package miroshka.aether.api;

import java.util.Objects;

public record ConnectionStatus(
        boolean connected,
        int latencyMillis,
        boolean stale,
        String masterAddress,
        ConnectionState state) {

    public ConnectionStatus {
        Objects.requireNonNull(masterAddress, "masterAddress");
        Objects.requireNonNull(state, "state");
    }

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        AUTHENTICATING,
        CONNECTED,
        DEGRADED,
        RECONNECTING
    }

    public static ConnectionStatus disconnected() {
        return new ConnectionStatus(false, 0, true, "", ConnectionState.DISCONNECTED);
    }

    public static ConnectionStatus connecting(String masterAddress) {
        return new ConnectionStatus(false, 0, true, masterAddress, ConnectionState.CONNECTING);
    }

    public static ConnectionStatus connected(String masterAddress, int latencyMillis, boolean stale) {
        return new ConnectionStatus(
                true,
                latencyMillis,
                stale,
                masterAddress,
                stale ? ConnectionState.DEGRADED : ConnectionState.CONNECTED);
    }
}
