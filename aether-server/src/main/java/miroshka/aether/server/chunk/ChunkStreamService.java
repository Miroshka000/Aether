package miroshka.aether.server.chunk;

import miroshka.aether.api.chunk.ChunkStreaming;
import miroshka.aether.common.event.AetherEventBus;
import miroshka.aether.common.event.ChunkDataReceivedEvent;
import miroshka.aether.common.protocol.ChunkDataPacket;
import miroshka.aether.server.network.NodeNetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ChunkStreamService implements ChunkStreaming {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkStreamService.class);

    private final NodeNetworkClient networkClient;
    private final String serverName;
    private final Map<ChunkKey, byte[]> chunkCache;
    private final Map<UUID, CompletableFuture<List<ChunkDataPacket>>> pendingRequests;
    private final Consumer<ChunkDataReceivedEvent> eventHandler;
    private volatile boolean enabled;
    private volatile int preloadRadius;
    private volatile long timeoutMs;
    private volatile boolean subscribed;

    public ChunkStreamService(NodeNetworkClient networkClient, String serverName) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.chunkCache = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.eventHandler = this::onChunkDataReceived;
        this.enabled = false;
        this.preloadRadius = 3;
        this.timeoutMs = 5000;
        this.subscribed = false;
    }

    public void init() {
        if (!subscribed) {
            AetherEventBus.instance().subscribe(ChunkDataReceivedEvent.class, eventHandler);
            subscribed = true;
            LOGGER.debug("ChunkStreamService subscribed to ChunkDataReceivedEvent");
        }
    }

    public void shutdown() {
        if (subscribed) {
            AetherEventBus.instance().unsubscribe(ChunkDataReceivedEvent.class, eventHandler);
            subscribed = false;
            LOGGER.debug("ChunkStreamService unsubscribed from ChunkDataReceivedEvent");
        }
        clearCache();
    }

    private void onChunkDataReceived(ChunkDataReceivedEvent event) {
        handleIncomingChunk(event.packet());
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            init();
            LOGGER.info("ChunkStreamService enabled (EXPERIMENTAL)");
        } else {
            LOGGER.info("ChunkStreamService disabled");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setPreloadRadius(int radius) {
        this.preloadRadius = Math.max(1, Math.min(radius, 10));
    }

    @Override
    public int getPreloadRadius() {
        return preloadRadius;
    }

    @Override
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = Math.max(1000, timeoutMs);
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public CompletableFuture<Void> preloadChunksForPlayer(UUID playerUuid, String targetServer,
            int spawnX, int spawnZ) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.debug("Preloading chunks for {} around ({}, {}) from {}",
                playerUuid, spawnX, spawnZ, targetServer);

        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<ChunkDataPacket>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        ChunkDataPacket request = new ChunkDataPacket(
                requestId,
                serverName,
                targetServer,
                ChunkDataPacket.ChunkAction.REQUEST,
                spawnX >> 4,
                spawnZ >> 4,
                new byte[0]);

        networkClient.sendPacket(request);

        return future
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .thenAccept(chunks -> {
                    for (ChunkDataPacket chunk : chunks) {
                        cacheChunk(chunk);
                    }
                    LOGGER.debug("Preloaded {} chunks for {}", chunks.size(), playerUuid);
                })
                .exceptionally(ex -> {
                    LOGGER.warn("Failed to preload chunks for {}: {}", playerUuid, ex.getMessage());
                    return null;
                })
                .whenComplete((v, ex) -> pendingRequests.remove(requestId));
    }

    public void handleIncomingChunk(ChunkDataPacket packet) {
        if (packet.action() == ChunkDataPacket.ChunkAction.RESPONSE) {
            UUID requestId = packet.requestId();
            if (requestId != null) {
                CompletableFuture<List<ChunkDataPacket>> future = pendingRequests.get(requestId);
                if (future != null) {
                    future.complete(List.of(packet));
                }
            }
        } else if (packet.action() == ChunkDataPacket.ChunkAction.PUSH) {
            cacheChunk(packet);
        }
    }

    @Override
    public byte[] getCachedChunk(int chunkX, int chunkZ) {
        return chunkCache.get(new ChunkKey(chunkX, chunkZ));
    }

    @Override
    public void clearCache() {
        chunkCache.clear();
    }

    @Override
    public void clearCacheForRadius(int centerX, int centerZ, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                chunkCache.remove(new ChunkKey(x, z));
            }
        }
    }

    private void cacheChunk(ChunkDataPacket packet) {
        ChunkKey key = new ChunkKey(packet.chunkX(), packet.chunkZ());
        chunkCache.put(key, packet.chunkData());
    }

    @Override
    public int getCacheSize() {
        return chunkCache.size();
    }

    private record ChunkKey(int x, int z) {
    }
}
