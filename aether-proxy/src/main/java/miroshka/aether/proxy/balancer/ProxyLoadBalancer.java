package miroshka.aether.proxy.balancer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProxyLoadBalancer {

    private static final Strategy DEFAULT_STRATEGY = Strategy.LEAST_CONNECTIONS;

    private final ProxyServer proxyServer;
    private final NodeRegistry nodeRegistry;
    private final AtomicInteger roundRobinCounter;
    private final Random random;

    public ProxyLoadBalancer(ProxyServer proxyServer, NodeRegistry nodeRegistry) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.roundRobinCounter = new AtomicInteger(0);
        this.random = new Random();
    }

    public Optional<ServerInfo> selectServer(UUID playerUuid, List<String> candidates) {
        return selectServer(playerUuid, candidates, DEFAULT_STRATEGY);
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
                    available.add(new ServerMetrics(info, online, tps));
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

    public enum Strategy {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        LEAST_TPS_LOAD,
        RANDOM
    }

    private record ServerMetrics(ServerInfo serverInfo, int onlinePlayers, double tps) {
    }
}
