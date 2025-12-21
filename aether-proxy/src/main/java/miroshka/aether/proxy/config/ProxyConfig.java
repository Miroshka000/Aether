package miroshka.aether.proxy.config;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProxyConfig(
        NetworkConfig network,
        List<String> secretKeys,
        String webJwtSecret,
        Set<String> allowedIpRanges,
        int maxNodesCount,
        int broadcastIntervalMillis,
        int heartbeatTimeoutMillis,
        int rateLimitPacketsPerSecond,
        int rateLimitBurstSize,
        boolean compressionEnabled,
        TransportConfig transport) {

    public ProxyConfig {
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(secretKeys, "secretKeys");
        Objects.requireNonNull(webJwtSecret, "webJwtSecret");
        Objects.requireNonNull(allowedIpRanges, "allowedIpRanges");
        Objects.requireNonNull(transport, "transport");
        if (secretKeys.isEmpty()) {
            throw new IllegalArgumentException("At least one secret key is required");
        }
    }

    public static ProxyConfig defaults() {
        return new ProxyConfig(
                new NetworkConfig(3000, 9090, 8080),
                List.of("change-me-secret-key"),
                "aether-web-jwt-secret-change-me",
                Set.of(),
                100,
                500,
                15000,
                100,
                200,
                true,
                TransportConfig.defaults());
    }

    public record NetworkConfig(int port, int metricsPort, int webPort) {
        public NetworkConfig {
            if (port <= 0)
                throw new IllegalArgumentException("port must be positive");
            if (webPort < 0)
                throw new IllegalArgumentException("webPort cannot be negative");
        }
    }
}
