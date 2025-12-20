package miroshka.aether.server.pdc;

import miroshka.aether.api.pdc.DistributedPDC;
import miroshka.aether.common.protocol.PDCSyncPacket;
import miroshka.aether.server.network.NodeNetworkClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DistributedPDCService implements DistributedPDC {

    private final NodeNetworkClient networkClient;
    private final String serverName;
    private final Map<UUID, PlayerDataCache> cache;
    private volatile ConflictResolver conflictResolver;

    public DistributedPDCService(NodeNetworkClient networkClient, String serverName) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.cache = new ConcurrentHashMap<>();
        this.conflictResolver = ConflictResolver.lastWriteWins();
    }

    @Override
    public <T> void set(UUID playerUuid, String key, T value, PDCSerializer<T> serializer) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(serializer, "serializer");

        byte[] data = serializer.serialize(value);
        PlayerDataCache playerCache = cache.computeIfAbsent(playerUuid, k -> new PlayerDataCache());
        playerCache.set(key, data);

        Map<String, byte[]> updateData = new HashMap<>();
        updateData.put(key, data);
        PDCSyncPacket packet = PDCSyncPacket.partialUpdate(playerUuid, serverName, updateData,
                playerCache.getVersion());
        networkClient.sendPacket(packet);
    }

    @Override
    public <T> Optional<T> get(UUID playerUuid, String key, PDCSerializer<T> serializer) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(serializer, "serializer");

        PlayerDataCache playerCache = cache.get(playerUuid);
        if (playerCache == null) {
            return Optional.empty();
        }

        byte[] data = playerCache.get(key);
        if (data == null || data.length == 0) {
            return Optional.empty();
        }

        return Optional.of(serializer.deserialize(data));
    }

    @Override
    public <T> T getOrDefault(UUID playerUuid, String key, T defaultValue, PDCSerializer<T> serializer) {
        return get(playerUuid, key, serializer).orElse(defaultValue);
    }

    @Override
    public boolean has(UUID playerUuid, String key) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(key, "key");

        PlayerDataCache playerCache = cache.get(playerUuid);
        return playerCache != null && playerCache.has(key);
    }

    @Override
    public void remove(UUID playerUuid, String key) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(key, "key");

        PlayerDataCache playerCache = cache.get(playerUuid);
        if (playerCache != null) {
            playerCache.remove(key);
        }

        PDCSyncPacket packet = PDCSyncPacket.delete(playerUuid, serverName, key);
        networkClient.sendPacket(packet);
    }

    @Override
    public Map<String, byte[]> getAll(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        PlayerDataCache playerCache = cache.get(playerUuid);
        if (playerCache == null) {
            return Map.of();
        }

        return playerCache.getAll();
    }

    @Override
    public CompletableFuture<Void> sync(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        PlayerDataCache playerCache = cache.get(playerUuid);
        if (playerCache == null) {
            return CompletableFuture.completedFuture(null);
        }

        PDCSyncPacket packet = PDCSyncPacket.fullSync(playerUuid, serverName, playerCache.getAll());
        networkClient.sendPacket(packet);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> load(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");

        Map<String, byte[]> requestData = new HashMap<>();
        PDCSyncPacket packet = new PDCSyncPacket(playerUuid, serverName, PDCSyncPacket.SyncOperation.REQUEST,
                requestData, System.currentTimeMillis());
        networkClient.sendPacket(packet);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void invalidateCache(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    @Override
    public void setConflictResolver(ConflictResolver resolver) {
        this.conflictResolver = Objects.requireNonNull(resolver, "resolver");
    }

    public void handleIncomingSync(PDCSyncPacket packet) {
        UUID playerUuid = packet.playerUuid();
        PlayerDataCache playerCache = cache.computeIfAbsent(playerUuid, k -> new PlayerDataCache());

        switch (packet.operation()) {
            case FULL_SYNC -> {
                for (Map.Entry<String, byte[]> entry : packet.data().entrySet()) {
                    byte[] localData = playerCache.get(entry.getKey());
                    if (localData != null) {
                        byte[] resolved = conflictResolver.resolve(
                                entry.getKey(), localData, entry.getValue(),
                                playerCache.getVersion(), packet.version());
                        playerCache.set(entry.getKey(), resolved);
                    } else {
                        playerCache.set(entry.getKey(), entry.getValue());
                    }
                }
                playerCache.updateVersionIfNewer(packet.version());
            }
            case PARTIAL_UPDATE -> {
                for (Map.Entry<String, byte[]> entry : packet.data().entrySet()) {
                    byte[] localData = playerCache.get(entry.getKey());
                    if (localData != null && packet.version() <= playerCache.getVersion()) {
                        byte[] resolved = conflictResolver.resolve(
                                entry.getKey(), localData, entry.getValue(),
                                playerCache.getVersion(), packet.version());
                        playerCache.set(entry.getKey(), resolved);
                    } else {
                        playerCache.set(entry.getKey(), entry.getValue());
                    }
                }
                playerCache.updateVersionIfNewer(packet.version());
            }
            case DELETE -> {
                for (String key : packet.data().keySet()) {
                    playerCache.remove(key);
                }
            }
            case REQUEST -> {
            }
        }
    }

    private static final class PlayerDataCache {
        private final Map<String, byte[]> data = new ConcurrentHashMap<>();
        private final AtomicLong version = new AtomicLong(System.currentTimeMillis());

        void set(String key, byte[] value) {
            data.put(key, value);
            version.set(System.currentTimeMillis());
        }

        byte[] get(String key) {
            return data.get(key);
        }

        boolean has(String key) {
            return data.containsKey(key);
        }

        void remove(String key) {
            data.remove(key);
            version.set(System.currentTimeMillis());
        }

        Map<String, byte[]> getAll() {
            return new HashMap<>(data);
        }

        long getVersion() {
            return version.get();
        }

        void updateVersionIfNewer(long newVersion) {
            version.updateAndGet(current -> Math.max(current, newVersion));
        }
    }
}
