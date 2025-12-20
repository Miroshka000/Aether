package miroshka.aether.proxy.config;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProxyConfig(
        int port,
        int metricsPort,
        List<String> secretKeys,
        Set<String> allowedIpRanges,
        int maxNodesCount,
        int broadcastIntervalMillis,
        int heartbeatTimeoutMillis,
        int rateLimitPacketsPerSecond,
        int rateLimitBurstSize,
        boolean compressionEnabled) {

    public ProxyConfig {
        Objects.requireNonNull(secretKeys, "secretKeys");
        Objects.requireNonNull(allowedIpRanges, "allowedIpRanges");
        if (secretKeys.isEmpty()) {
            throw new IllegalArgumentException("At least one secret key is required");
        }
    }

    public static ProxyConfig defaults() {
        return new ProxyConfig(
                3000,
                9090,
                List.of("change-me-secret-key"),
                Set.of(),
                100,
                500,
                15000,
                100,
                200,
                true);
    }
}
