package miroshka.aether.proxy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProxyConfigLoader {

    private static final String CONFIG_FILE = "config.yml";

    public static ProxyConfig load(Path dataFolder) {
        Path configPath = dataFolder.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            return ProxyConfig.defaults();
        }

        return loadFromFile(configPath);
    }

    @SuppressWarnings("unchecked")
    private static ProxyConfig loadFromFile(Path configPath) {
        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = yaml.load(is);

            Map<String, Object> network = (Map<String, Object>) data.getOrDefault("network", Map.of());
            Map<String, Object> security = (Map<String, Object>) data.getOrDefault("security", Map.of());
            Map<String, Object> limits = (Map<String, Object>) data.getOrDefault("limits", Map.of());
            Map<String, Object> rateLimit = (Map<String, Object>) data.getOrDefault("rate-limit", Map.of());

            List<String> secretKeys = (List<String>) security.getOrDefault("secret-keys",
                    List.of("change-me-secret-key"));
            List<String> allowedIpsList = (List<String>) security.getOrDefault("allowed-ips", List.of());
            Set<String> allowedIps = new HashSet<>(allowedIpsList);

            return new ProxyConfig(
                    ((Number) network.getOrDefault("port", 3000)).intValue(),
                    ((Number) network.getOrDefault("metrics-port", 9090)).intValue(),
                    secretKeys,
                    allowedIps,
                    ((Number) limits.getOrDefault("max-nodes", 100)).intValue(),
                    ((Number) network.getOrDefault("broadcast-interval-ms", 500)).intValue(),
                    ((Number) network.getOrDefault("heartbeat-timeout-ms", 15000)).intValue(),
                    ((Number) rateLimit.getOrDefault("packets-per-second", 100)).intValue(),
                    ((Number) rateLimit.getOrDefault("burst-size", 200)).intValue(),
                    (Boolean) network.getOrDefault("compression-enabled", true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from " + configPath, e);
        }
    }

    private static void createDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }

        String configContent = """
                # ============================================================
                # Aether Proxy (Master) Plugin Configuration
                # ============================================================
                # This is the central hub that all Aether Server nodes connect to.
                # It aggregates data from all servers and broadcasts network state.
                # ============================================================

                # Network settings for the Master server
                network:
                  # TCP port for node connections
                  # All aether-server plugins must connect to this port
                  # Make sure this port is accessible from your game servers
                  port: 3000

                  # HTTP port for Prometheus metrics endpoint
                  # Access at: http://localhost:9090/metrics
                  # Set to 0 to disable metrics
                  metrics-port: 9090

                  # How often to broadcast aggregated state to all nodes (milliseconds)
                  # Lower values = more real-time updates, more network traffic
                  broadcast-interval-ms: 500

                  # Time without heartbeat before considering a node disconnected (milliseconds)
                  # Should be greater than the node's heartbeat-interval-ms * 2
                  heartbeat-timeout-ms: 15000

                  # Enable Snappy compression for network packets
                  # Recommended for production, reduces bandwidth usage
                  compression-enabled: true

                # Security settings
                security:
                  # List of valid secret keys for node authentication
                  # Nodes must provide one of these keys to connect
                  # You can have multiple keys for key rotation
                  #
                  # Recommended formats:
                  #   - UUID: "550e8400-e29b-41d4-a716-446655440000"
                  #   - Random string (32+ chars): "aX9kL2mN4pQ7rS1tU6vW8xY0zA3bC5dE"
                  #   - Base64 encoded: "c2VjdXJlLXJhbmRvbS1rZXktaGVyZQ=="
                  #
                  # Generate with: uuidgen, openssl rand -base64 32, or any password generator
                  # IMPORTANT: Change these in production!
                  secret-keys:
                    - "change-me-secret-key"

                  # List of allowed IP addresses/ranges for node connections
                  # Leave empty to allow connections from any IP
                  # Examples: ["172.20.0.0/16", "192.168.1.100"]
                  # For Docker: usually leave empty (uses internal network)
                  allowed-ips: []

                # Connection limits
                limits:
                  # Maximum number of server nodes that can connect
                  # Prevents resource exhaustion from too many connections
                  max-nodes: 100

                # Rate limiting to prevent abuse
                rate-limit:
                  # Maximum packets per second from a single node
                  packets-per-second: 100

                  # Burst size - allows temporary spikes above the rate limit
                  burst-size: 200
                """;

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            writer.write(configContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default config at " + configPath, e);
        }
    }
}
