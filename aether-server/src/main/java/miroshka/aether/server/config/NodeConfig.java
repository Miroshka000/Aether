package miroshka.aether.server.config;

import java.util.Objects;

public record NodeConfig(
        String masterHost,
        int masterPort,
        String serverName,
        String secretKey,
        int bedrockPort,
        int heartbeatIntervalMillis,
        int snapshotIntervalMillis,
        int reconnectionInitialDelayMillis,
        int reconnectionMaxDelayMillis) {

    public NodeConfig {
        Objects.requireNonNull(masterHost, "masterHost");
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(secretKey, "secretKey");
        if (bedrockPort <= 0) {
            throw new IllegalArgumentException("bedrockPort must be positive");
        }
    }

    public static NodeConfig defaults() {
        return new NodeConfig(
                "localhost",
                3000,
                "node-1",
                "change-me-secret-key",
                19132,
                5000,
                200,
                1000,
                30000);
    }

    public String masterAddress() {
        return masterHost + ":" + masterPort;
    }
}
