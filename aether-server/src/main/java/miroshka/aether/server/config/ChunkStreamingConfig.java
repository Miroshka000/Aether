package miroshka.aether.server.config;

public record ChunkStreamingConfig(
        boolean enabled,
        int preloadRadius,
        long timeoutMs) {

    public static ChunkStreamingConfig defaults() {
        return new ChunkStreamingConfig(false, 3, 5000);
    }
}
