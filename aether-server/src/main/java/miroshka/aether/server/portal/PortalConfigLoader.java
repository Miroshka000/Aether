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
            List<PortalConfig.PortalDefinition> portals = new ArrayList<>();

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
                            PortalConfig.PortalDefinition parsed = parsePortal(id, (Map<String, Object>) portalValue);
                            if (parsed != null) {
                                portals.add(parsed);
                            }
                        }
                    }
                }
            }

            return new PortalConfig(enabled, portals);
        } catch (IOException | ClassCastException e) {
            throw new RuntimeException("Failed to load portal config from " + configPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static PortalConfig.PortalDefinition parsePortal(String id, Map<String, Object> data) {
        try {
            String typeStr = (String) data.getOrDefault("type", "REGION");
            String targetServer = (String) data.get("target-server");
            boolean seamless = (Boolean) data.getOrDefault("seamless", true);
            boolean enabled = (Boolean) data.getOrDefault("enabled", true);

            if (targetServer == null) return null;

            if ("BOUNDARY".equalsIgnoreCase(typeStr)) {
                Map<String, Object> boundary = (Map<String, Object>) data.get("boundary");
                if (boundary == null) return null;

                String world = (String) boundary.getOrDefault("world", "world");
                String dirStr = (String) boundary.get("direction");
                PortalConfig.Direction direction = PortalConfig.Direction.valueOf(dirStr.toUpperCase());
                int threshold = ((Number) boundary.getOrDefault("threshold", 0)).intValue();

                return new PortalConfig.BoundaryPortalDefinition(id, targetServer, seamless, enabled, world, direction, threshold);
            } else {
                Map<String, Object> region = (Map<String, Object>) data.get("region");
                if (region != null) {
                    String world = (String) region.getOrDefault("world", "world");
                    PortalConfig.Position min = new PortalConfig.Position(
                            ((Number) region.getOrDefault("min-x", 0)).intValue(),
                            ((Number) region.getOrDefault("min-y", 0)).intValue(),
                            ((Number) region.getOrDefault("min-z", 0)).intValue());
                    PortalConfig.Position max = new PortalConfig.Position(
                            ((Number) region.getOrDefault("max-x", 0)).intValue(),
                            ((Number) region.getOrDefault("max-y", 0)).intValue(),
                            ((Number) region.getOrDefault("max-z", 0)).intValue());
                    return new PortalConfig.RegionPortalDefinition(id, targetServer, seamless, enabled, world, min, max);
                }
                
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void createDefaultConfig(Path configPath) {
        String configContent = """
                portals:
                  enabled: true
                  list:
                    example-boundary:
                      type: BOUNDARY
                      target-server: survival
                      boundary:
                        world: world
                        direction: EAST
                        threshold: 1000
                """;
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            writer.write(configContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default portal config", e);
        }
    }
}
