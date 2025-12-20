package miroshka.aether.server.balancer;

import miroshka.aether.api.balancer.LoadBalancer;
import miroshka.aether.common.protocol.ServerInfo;
import miroshka.aether.server.state.NetworkStateCache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LoadBalancerService implements LoadBalancer {

    private final NetworkStateCache stateCache;
    private final Map<UUID, Integer> playerPriorities;
    private final Map<String, Map<UUID, Long>> reservedSlots;
    private final AtomicInteger roundRobinCounter;
    private final Random random;
    private volatile BalancingStrategy defaultStrategy;
    private volatile WeightProvider weightProvider;

    public LoadBalancerService(NetworkStateCache stateCache) {
        this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
        this.playerPriorities = new ConcurrentHashMap<>();
        this.reservedSlots = new ConcurrentHashMap<>();
        this.roundRobinCounter = new AtomicInteger(0);
        this.random = new Random();
        this.defaultStrategy = BalancingStrategy.LEAST_CONNECTIONS;
        this.weightProvider = WeightProvider.byAvailableSlots();
    }

    @Override
    public Optional<String> selectServer(UUID playerUuid, List<String> candidates) {
        return selectServer(playerUuid, candidates, defaultStrategy);
    }

    @Override
    public Optional<String> selectServer(UUID playerUuid, List<String> candidates, BalancingStrategy strategy) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        List<ServerMetrics> available = candidates.stream()
                .map(this::getServerMetrics)
                .filter(ServerMetrics::hasSpace)
                .toList();

        if (available.isEmpty()) {
            return Optional.empty();
        }

        return switch (strategy) {
            case ROUND_ROBIN -> selectRoundRobin(available);
            case LEAST_CONNECTIONS -> selectLeastConnections(available);
            case LEAST_TPS_LOAD -> selectLeastTpsLoad(available);
            case WEIGHTED -> selectWeighted(available);
            case RANDOM -> selectRandom(available);
            case PRIORITY_QUEUE -> selectWithPriority(playerUuid, available);
        };
    }

    private Optional<String> selectRoundRobin(List<ServerMetrics> available) {
        int index = roundRobinCounter.getAndIncrement() % available.size();
        return Optional.of(available.get(index).serverName());
    }

    private Optional<String> selectLeastConnections(List<ServerMetrics> available) {
        return available.stream()
                .min(Comparator.comparingInt(ServerMetrics::onlinePlayers))
                .map(ServerMetrics::serverName);
    }

    private Optional<String> selectLeastTpsLoad(List<ServerMetrics> available) {
        return available.stream()
                .max(Comparator.comparingDouble(ServerMetrics::tps))
                .map(ServerMetrics::serverName);
    }

    private Optional<String> selectWeighted(List<ServerMetrics> available) {
        int totalWeight = available.stream()
                .mapToInt(m -> weightProvider.getWeight(m.serverName(), m))
                .sum();

        if (totalWeight <= 0) {
            return selectRandom(available);
        }

        int randomWeight = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (ServerMetrics metrics : available) {
            cumulativeWeight += weightProvider.getWeight(metrics.serverName(), metrics);
            if (randomWeight < cumulativeWeight) {
                return Optional.of(metrics.serverName());
            }
        }

        return Optional.of(available.getFirst().serverName());
    }

    private Optional<String> selectRandom(List<ServerMetrics> available) {
        int index = random.nextInt(available.size());
        return Optional.of(available.get(index).serverName());
    }

    private Optional<String> selectWithPriority(UUID playerUuid, List<ServerMetrics> available) {
        int priority = getPlayerPriority(playerUuid);

        if (priority > 0) {
            List<ServerMetrics> sorted = new ArrayList<>(available);
            sorted.sort(Comparator.comparingDouble(ServerMetrics::loadFactor));
            return Optional.of(sorted.getFirst().serverName());
        }

        return selectLeastConnections(available);
    }

    @Override
    public void setDefaultStrategy(BalancingStrategy strategy) {
        this.defaultStrategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public BalancingStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    @Override
    public void setPlayerPriority(UUID playerUuid, int priority) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (priority <= 0) {
            playerPriorities.remove(playerUuid);
        } else {
            playerPriorities.put(playerUuid, priority);
        }
    }

    @Override
    public int getPlayerPriority(UUID playerUuid) {
        return playerPriorities.getOrDefault(playerUuid, 0);
    }

    @Override
    public void reserveSlot(String serverName, UUID playerUuid, long durationMs) {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(playerUuid, "playerUuid");

        long expiration = System.currentTimeMillis() + durationMs;
        reservedSlots.computeIfAbsent(serverName, k -> new ConcurrentHashMap<>())
                .put(playerUuid, expiration);
    }

    @Override
    public void releaseSlot(String serverName, UUID playerUuid) {
        Map<UUID, Long> slots = reservedSlots.get(serverName);
        if (slots != null) {
            slots.remove(playerUuid);
        }
    }

    @Override
    public ServerMetrics getServerMetrics(String serverName) {
        Optional<ServerInfo> info = stateCache.getServer(serverName);
        if (info.isEmpty()) {
            return new ServerMetrics(serverName, 0, 0, 0, 1, 0, false);
        }

        ServerInfo server = info.get();
        int reservedCount = getReservedSlotCount(serverName);

        return new ServerMetrics(
                server.name(),
                server.onlinePlayers() + reservedCount,
                server.maxPlayers(),
                server.tps(),
                1,
                server.lastUpdateTimestamp(),
                server.isOnline());
    }

    @Override
    public List<ServerMetrics> getAllServerMetrics() {
        return stateCache.getServers().stream()
                .map(server -> getServerMetrics(server.name()))
                .toList();
    }

    @Override
    public void setWeightProvider(WeightProvider provider) {
        this.weightProvider = Objects.requireNonNull(provider, "provider");
    }

    private int getReservedSlotCount(String serverName) {
        Map<UUID, Long> slots = reservedSlots.get(serverName);
        if (slots == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        slots.entrySet().removeIf(entry -> entry.getValue() < now);
        return slots.size();
    }
}
