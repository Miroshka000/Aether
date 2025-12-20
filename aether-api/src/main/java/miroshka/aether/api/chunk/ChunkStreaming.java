package miroshka.aether.api.chunk;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ChunkStreaming {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getPreloadRadius();

    void setPreloadRadius(int radius);

    long getTimeoutMs();

    void setTimeoutMs(long timeoutMs);

    CompletableFuture<Void> preloadChunksForPlayer(UUID playerUuid, String targetServer, int spawnX, int spawnZ);

    byte[] getCachedChunk(int chunkX, int chunkZ);

    void clearCache();

    void clearCacheForRadius(int centerX, int centerZ, int radius);

    int getCacheSize();
}
