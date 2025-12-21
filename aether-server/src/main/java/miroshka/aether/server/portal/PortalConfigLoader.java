package miroshka.aether.server.portal;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PortalConfigLoader {

    private static final String CONFIG_FILE = "portals.yml";

    public static PortalConfig load(Path dataFolder) {
        Path configPath = dataFolder.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            return PortalConfig.defaults();
        }

        return loadFromFile(configPath);
    }

    @SuppressWarnings("unchecked")
    private static PortalConfig loadFromFile(Path configPath) {
        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = yaml.load(is);

            boolean enabled = (Boolean) data.getOrDefault("enabled", true);
            List<Map<String, Object>> portalsList = (List<Map<String, Object>>) data.getOrDefault("portals", List.of());

            List<PortalConfig.PortalEntry> portals = new ArrayList<>();
            for (Map<String, Object> portalData : portalsList) {
                portals.add(parsePortal(portalData));
            }

            return new PortalConfig(enabled, portals);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load portal config from " + configPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static PortalConfig.PortalEntry parsePortal(Map<String, Object> data) {
        String id = (String) data.get("id");
        String targetServer = (String) data.get("target-server");
        String world = (String) data.getOrDefault("world", "world");
        boolean seamless = (Boolean) data.getOrDefault("seamless", true);

        Map<String, Object> pos1Data = (Map<String, Object>) data.get("pos1");
        Map<String, Object> pos2Data = (Map<String, Object>) data.get("pos2");
        Map<String, Object> spawnData = (Map<String, Object>) data.get("spawn");

        PortalConfig.Position pos1 = parsePosition(pos1Data);
        PortalConfig.Position pos2 = parsePosition(pos2Data);
        PortalConfig.Position spawn = spawnData != null ? parsePosition(spawnData) : null;

        return new PortalConfig.PortalEntry(id, targetServer, world, pos1, pos2, spawn, seamless);
    }

    private static PortalConfig.Position parsePosition(Map<String, Object> data) {
        int x = ((Number) data.getOrDefault("x", 0)).intValue();
        int y = ((Number) data.getOrDefault("y", 64)).intValue();
        int z = ((Number) data.getOrDefault("z", 0)).intValue();
        return new PortalConfig.Position(x, y, z);
    }

    private static void createDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }

        String configContent = """
                # ============================================================
                # Aether Portal Configuration
                # ============================================================
                # Define portal regions that transfer players to other servers
                # when they enter the specified area.
                #
                # Портала определяются двумя координатами (pos1 и pos2),
                # образующими прямоугольную область. Когда игрок входит в эту
                # область, он будет перемещён на указанный сервер.
                # ============================================================

                # Enable portal system (включить систему порталов)
                enabled: true

                # Portal definitions (определения порталов)
                portals:
                  # Example portal configuration:
                  # Пример конфигурации портала:
                  #
                  # - id: "lobby-to-survival"
                  #   target-server: "survival"
                  #   world: "world"
                  #   pos1:
                  #     x: 100
                  #     y: 60
                  #     z: 100
                  #   pos2:
                  #     x: 103      # Portal is 3 blocks wide (X)
                  #     y: 63       # Portal is 3 blocks tall (Y)
                  #     z: 100      # Portal is 1 block deep (Z)
                  #   spawn:        # Optional: where to spawn on target server
                  #     x: 0
                  #     y: 64
                  #     z: 0
                  #   seamless: true  # Use seamless transfer (no loading screen)

                  # Another example - larger portal area:
                  # - id: "hub-portal"
                  #   target-server: "minigames"
                  #   world: "world"
                  #   pos1:
                  #     x: -10
                  #     y: 50
                  #     z: -10
                  #   pos2:
                  #     x: 10
                  #     y: 70
                  #     z: 10
                  #   seamless: false
                """;

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            writer.write(configContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default portal config at " + configPath, e);
        }
    }
}
