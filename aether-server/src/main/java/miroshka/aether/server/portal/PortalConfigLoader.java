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
            if (data == null) {
                return PortalConfig.defaults();
            }

            boolean enabled = true;
            List<PortalConfig.PortalEntry> portals = new ArrayList<>();

            Object portalsObj = data.get("portals");
            if (portalsObj instanceof Map) {
                Map<String, Object> portalsMap = (Map<String, Object>) portalsObj;
                enabled = (Boolean) portalsMap.getOrDefault("enabled", true);

                Object listObj = portalsMap.get("list");
                if (listObj instanceof Map) {
                    Map<String, Object> listMap = (Map<String, Object>) listObj;
                    for (Map.Entry<String, Object> entry : listMap.entrySet()) {
                        String id = entry.getKey();
                        Object portalValue = entry.getValue();
                        if (portalValue instanceof Map) {
                            PortalConfig.PortalEntry parsed = parsePortal(id, (Map<String, Object>) portalValue);
                            if (parsed != null) {
                                portals.add(parsed);
                            }
                        }
                    }
                }
            } else if (portalsObj instanceof List) {
                enabled = (Boolean) data.getOrDefault("enabled", true);
                for (Object item : (List<?>) portalsObj) {
                    if (item instanceof Map) {
                        PortalConfig.PortalEntry parsed = parsePortal(null, (Map<String, Object>) item);
                        if (parsed != null) {
                            portals.add(parsed);
                        }
                    }
                }
            } else {
                enabled = (Boolean) data.getOrDefault("enabled", true);
            }

            return new PortalConfig(enabled, portals);
        } catch (IOException | ClassCastException e) {
            throw new RuntimeException("Failed to load portal config from " + configPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static PortalConfig.PortalEntry parsePortal(String idFromKey, Map<String, Object> data) {
        try {
            String id = (String) data.getOrDefault("id", idFromKey);
            String targetServer = (String) data.get("target-server");
            if (id == null || targetServer == null)
                return null;

            boolean seamless = (Boolean) data.getOrDefault("seamless", true);

            if (data.containsKey("region")) {
                Map<String, Object> region = (Map<String, Object>) data.get("region");
                String world = (String) region.getOrDefault("world", "world");
                PortalConfig.Position pos1 = new PortalConfig.Position(
                        ((Number) region.getOrDefault("min-x", 0)).intValue(),
                        ((Number) region.getOrDefault("min-y", 0)).intValue(),
                        ((Number) region.getOrDefault("min-z", 0)).intValue());
                PortalConfig.Position pos2 = new PortalConfig.Position(
                        ((Number) region.getOrDefault("max-x", 0)).intValue(),
                        ((Number) region.getOrDefault("max-y", 0)).intValue(),
                        ((Number) region.getOrDefault("max-z", 0)).intValue());
                return new PortalConfig.PortalEntry(id, targetServer, world, pos1, pos2, null, seamless);
            }

            String world = (String) data.getOrDefault("world", "world");
            Map<String, Object> pos1Data = (Map<String, Object>) data.get("pos1");
            Map<String, Object> pos2Data = (Map<String, Object>) data.get("pos2");
            Map<String, Object> spawnData = (Map<String, Object>) data.get("spawn");

            if (pos1Data == null || pos2Data == null)
                return null;

            PortalConfig.Position pos1 = parsePosition(pos1Data);
            PortalConfig.Position pos2 = parsePosition(pos2Data);
            PortalConfig.Position spawn = spawnData != null ? parsePosition(spawnData) : null;

            return new PortalConfig.PortalEntry(id, targetServer, world, pos1, pos2, spawn, seamless);
        } catch (Exception e) {
            return null;
        }
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
                # Enable portal system (включить систему порталов)
                enabled: true

                # Portal definitions (определения порталов)
                portals:
                  - id: "example-portal"
                    target-server: "survival"
                    world: "world"
                    pos1: {x: 100, y: 60, z: 100}
                    pos2: {x: 103, y: 63, z: 100}
                    seamless: true
                """;

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            writer.write(configContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default portal config at " + configPath, e);
        }
    }
}
