package miroshka.aether.server.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class NodeConfigLoader {

    private static final String CONFIG_FILE = "config.yml";

    public static NodeConfig load(Path dataFolder) {
        Path configPath = dataFolder.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            return NodeConfig.defaults();
        }

        return loadFromFile(configPath);
    }

    @SuppressWarnings("unchecked")
    private static NodeConfig loadFromFile(Path configPath) {
        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = yaml.load(is);

            Map<String, Object> master = (Map<String, Object>) data.getOrDefault("master", Map.of());
            Map<String, Object> server = (Map<String, Object>) data.getOrDefault("server", Map.of());
            Map<String, Object> network = (Map<String, Object>) data.getOrDefault("network", Map.of());

            return new NodeConfig(
                    (String) master.getOrDefault("host", "localhost"),
                    ((Number) master.getOrDefault("port", 3000)).intValue(),
                    (String) server.getOrDefault("name", "node-1"),
                    (String) server.getOrDefault("secret-key", "change-me-secret-key"),
                    ((Number) server.getOrDefault("bedrock-port", 19132)).intValue(),
                    ((Number) network.getOrDefault("heartbeat-interval-ms", 5000)).intValue(),
                    ((Number) network.getOrDefault("snapshot-interval-ms", 200)).intValue(),
                    ((Number) network.getOrDefault("reconnect-initial-delay-ms", 1000)).intValue(),
                    ((Number) network.getOrDefault("reconnect-max-delay-ms", 30000)).intValue());
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
                # Aether Server Plugin Configuration
                # ============================================================
                # This plugin connects to the Aether Master (running on proxy)
                # to enable cross-server communication and state synchronization.
                # ============================================================

                # Master server connection settings
                # The Master runs on your WaterdogPE proxy with aether-proxy plugin
                master:
                  # Hostname or IP address of the Master server
                  # For Docker: use the container/service name (e.g., "proxy")
                  # For local development: use "localhost"
                  host: "localhost"

                  # TCP port where Master is listening
                  # Must match the 'port' setting in aether-proxy config
                  port: 3000

                # This server's identity settings
                server:
                  # Unique name for this server node
                  # Used to identify this server in the network
                  # Examples: "lobby", "rpg", "survival-1", "minigames"
                  name: "node-1"

                  # Secret key for authentication with Master
                  # Must match one of the keys in the Master's 'secret-keys' list
                  #
                  # Recommended formats:
                  #   - UUID: "550e8400-e29b-41d4-a716-446655440000"
                  #   - Random string (32+ chars): "aX9kL2mN4pQ7rS1tU6vW8xY0zA3bC5dE"
                  #   - Base64 encoded: "c2VjdXJlLXJhbmRvbS1rZXktaGVyZQ=="
                  #
                  # Generate with: uuidgen, openssl rand -base64 32, or any password generator
                  # IMPORTANT: Change this in production! Use the same key on all servers.
                  secret-key: "change-me-secret-key"

                  # Bedrock port for this server (used by ProxyTransport)
                  # This is the port where Minecraft clients would connect if not behind proxy
                  # Used for optimized TCP/QUIC transport between proxy and server
                  bedrock-port: 19132

                # Network and timing settings
                network:
                  # How often to send heartbeat packets to Master (milliseconds)
                  # Lower values = faster disconnect detection, more network traffic
                  heartbeat-interval-ms: 5000

                  # How often to collect and send server state snapshots (milliseconds)
                  # Contains: online players, max players, TPS
                  # Lower values = more real-time data, more CPU/network usage
                  snapshot-interval-ms: 200

                  # Initial delay before first reconnection attempt (milliseconds)
                  # Uses exponential backoff: delay doubles after each failed attempt
                  reconnect-initial-delay-ms: 1000

                  # Maximum delay between reconnection attempts (milliseconds)
                  # Backoff stops increasing after reaching this value
                  reconnect-max-delay-ms: 30000
                """;

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            writer.write(configContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default config at " + configPath, e);
        }
    }
}
