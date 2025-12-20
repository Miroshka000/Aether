package miroshka.aether.proxy.balancer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;

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

public final class ProxyLoadBalancer {

    private final ProxyServer proxyServer;
    private final NodeRegistry nodeRegistry;
    private final Map<UUID, Integer> playerPriorities;
    private final AtomicInteger roundRobinCounter;
    private final Random random;
    private volatile Strategy defaultStrategy;

    public ProxyLoadBalancer(ProxyServer proxyServer, NodeRegistry nodeRegistry) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.playerPriorities = new ConcurrentHashMap<>();
        this.roundRobinCounter = new AtomicInteger(0);
        this.random = new Random();
        this.defaultStrategy = Strategy.LEAST_CONNECTIONS;
    }

    public Optional<ServerInfo> selectServer(UUID playerUuid, List<String> candidates) {
        return selectServer(playerUuid, candidates, defaultStrategy);
    }

    public Optional<ServerInfo> selectServer(UUID playerUuid, List<String> candidates, Strategy strategy) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        List<ServerMetrics> available = new ArrayList<>();
        for (String name : candidates) {
            ServerInfo info = proxyServer.getServerInfo(name);
            if (info != null) {
                Optional<NodeSession> sessionOpt = nodeRegistry.getByNodeId(name);
                double tps = sessionOpt.map(s -> s.state().getTps()).orElse(20.0);
                int online = sessionOpt.map(s -> s.state().getOnlinePlayers()).orElse(0);
                int maxPlayers = sessionOpt.map(s -> s.state().getMaxPlayers()).orElse(100);

                if (online < maxPlayers) {
                    available.add(new ServerMetrics(info, online, maxPlayers, tps));
                }
            }
        }

        if (available.isEmpty()) {
            return Optional.empty();
        }

        return switch (strategy) {
            case ROUND_ROBIN -> selectRoundRobin(available);
            case LEAST_CONNECTIONS -> selectLeastConnections(available);
            case LEAST_TPS_LOAD -> selectLeastTpsLoad(available);
            case RANDOM -> selectRandom(available);
            case PRIORITY_QUEUE -> selectWithPriority(playerUuid, available);
        };
    }

    private Optional<ServerInfo> selectRoundRobin(List<ServerMetrics> available) {
        int index = roundRobinCounter.getAndIncrement() % available.size();
        return Optional.of(available.get(index).serverInfo);
    }

    private Optional<ServerInfo> selectLeastConnections(List<ServerMetrics> available) {
        return available.stream()
                .min(Comparator.comparingInt(m -> m.onlinePlayers))
                .map(m -> m.serverInfo);
    }

    private Optional<ServerInfo> selectLeastTpsLoad(List<ServerMetrics> available) {
        return available.stream()
                .max(Comparator.comparingDouble(m -> m.tps))
                .map(m -> m.serverInfo);
    }

    private Optional<ServerInfo> selectRandom(List<ServerMetrics> available) {
        int index = random.nextInt(available.size());
        return Optional.of(available.get(index).serverInfo);
    }

    private Optional<ServerInfo> selectWithPriority(UUID playerUuid, List<ServerMetrics> available) {
        int priority = playerPriorities.getOrDefault(playerUuid, 0);

        if (priority > 0) {
            return available.stream()
                    .min(Comparator.comparingDouble(m -> (double) m.onlinePlayers / m.maxPlayers))
                    .map(m -> m.serverInfo);
        }

        return selectLeastConnections(available);
    }

    public void setDefaultStrategy(Strategy strategy) {
        this.defaultStrategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void setPlayerPriority(UUID playerUuid, int priority) {
        if (priority <= 0) {
            playerPriorities.remove(playerUuid);
        } else {
            playerPriorities.put(playerUuid, priority);
        }
    }

    public enum Strategy {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        LEAST_TPS_LOAD,
        RANDOM,
        PRIORITY_QUEUE
    }

    private record ServerMetrics(ServerInfo serverInfo, int onlinePlayers, int maxPlayers, double tps) {
    }
}
