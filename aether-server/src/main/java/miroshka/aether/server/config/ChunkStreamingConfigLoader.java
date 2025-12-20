package miroshka.aether.server.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ChunkStreamingConfigLoader {

    private static final String CONFIG_FILE = "portals.yml";

    @SuppressWarnings("unchecked")
    public static ChunkStreamingConfig load(Path dataFolder) {
        Path configPath = dataFolder.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            return ChunkStreamingConfig.defaults();
        }

        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = yaml.load(is);
            Map<String, Object> chunkStreaming = (Map<String, Object>) data.getOrDefault("chunk-streaming", Map.of());

            boolean enabled = (boolean) chunkStreaming.getOrDefault("enabled", false);
            int preloadRadius = ((Number) chunkStreaming.getOrDefault("preload-radius", 3)).intValue();
            long timeoutMs = ((Number) chunkStreaming.getOrDefault("timeout-ms", 5000)).longValue();

            return new ChunkStreamingConfig(enabled, preloadRadius, timeoutMs);
        } catch (IOException e) {
            return ChunkStreamingConfig.defaults();
        }
    }
}
