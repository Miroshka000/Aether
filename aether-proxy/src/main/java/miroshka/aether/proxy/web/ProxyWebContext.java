package miroshka.aether.proxy.web;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;
import miroshka.aether.web.WebServer.AetherWebContext;
import miroshka.aether.web.WebServer.BalancerConfigDto;
import miroshka.aether.web.WebServer.EventDto;
import miroshka.aether.web.WebServer.PlayerDto;
import miroshka.aether.web.WebServer.PortalDto;
import miroshka.aether.web.WebServer.ServerDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ProxyWebContext implements AetherWebContext {

    private final NodeRegistry nodeRegistry;
    private final ProxyServer proxyServer;
    private final Map<String, PortalDto> portals;
    private final Map<String, EventCounter> eventCounters;
    private volatile String balancerStrategy = "LEAST_CONNECTIONS";
    private volatile boolean vipPriority = false;

    public ProxyWebContext(NodeRegistry nodeRegistry, ProxyServer proxyServer) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.proxyServer = proxyServer;
        this.portals = new ConcurrentHashMap<>();
        this.eventCounters = new ConcurrentHashMap<>();
        initDefaultEventCounters();
    }

    public ProxyWebContext(NodeRegistry nodeRegistry) {
        this(nodeRegistry, null);
    }

    private void initDefaultEventCounters() {
        eventCounters.put("PlayerJoin", new EventCounter(true));
        eventCounters.put("PlayerQuit", new EventCounter(true));
        eventCounters.put("PlayerTransfer", new EventCounter(true));
        eventCounters.put("ServerConnect", new EventCounter(true));
        eventCounters.put("ServerDisconnect", new EventCounter(true));
    }

    public void trackEvent(String eventType) {
        eventCounters.computeIfAbsent(eventType, k -> new EventCounter(true)).increment();
    }

    public void registerPortal(String id, String sourceServer, String targetServer, String type, boolean enabled) {
        portals.put(id, new PortalDto(id, sourceServer, targetServer, type, enabled));
    }

    public void unregisterPortal(String id) {
        portals.remove(id);
    }

    @Override
    public int getGlobalOnline() {
        return nodeRegistry.getTotalOnlinePlayers();
    }

    @Override
    public int getGlobalMaxPlayers() {
        return nodeRegistry.getAllSessions().stream()
                .mapToInt(session -> session.state().getMaxPlayers())
                .sum();
    }

    @Override
    public int getServerCount() {
        return nodeRegistry.getNodeCount();
    }

    @Override
    public List<ServerDto> getServers() {
        List<ServerDto> servers = new ArrayList<>();
        for (NodeSession session : nodeRegistry.getAllSessions()) {
            var state = session.state();
            servers.add(new ServerDto(
                    session.nodeId(),
                    state.getOnlinePlayers(),
                    state.getMaxPlayers(),
                    state.getTps(),
                    session.isActive()));
        }
        return servers;
    }

    @Override
    public List<PlayerDto> getPlayers() {
        List<PlayerDto> players = new ArrayList<>();

        if (proxyServer != null) {
            for (ProxiedPlayer player : proxyServer.getPlayers().values()) {
                String serverName = player.getServerInfo() != null
                        ? player.getServerInfo().getServerName()
                        : "Unknown";
                players.add(new PlayerDto(
                        player.getName(),
                        player.getUniqueId().toString(),
                        serverName,
                        System.currentTimeMillis(),
                        (int) player.getPing()));
            }
        }

        return players;
    }

    @Override
    public List<PortalDto> getPortals() {
        return new ArrayList<>(portals.values());
    }

    @Override
    public List<EventDto> getEvents() {
        List<EventDto> events = new ArrayList<>();
        for (var entry : eventCounters.entrySet()) {
            EventCounter counter = entry.getValue();
            events.add(new EventDto(
                    entry.getKey(),
                    counter.getCount(),
                    counter.isActive(),
                    counter.getLastTriggered()));
        }
        return events;
    }

    @Override
    public Map<String, Object> getMetrics() {
        long totalEvents = eventCounters.values().stream()
                .mapToLong(EventCounter::getCount)
                .sum();

        return Map.of(
                "totalServers", getServerCount(),
                "totalPlayers", getGlobalOnline(),
                "averageTps", calculateAverageTps(),
                "totalEvents", totalEvents,
                "portalsCount", portals.size());
    }

    @Override
    public BalancerConfigDto getBalancerConfig() {
        List<String> serverGroups = new ArrayList<>();
        for (NodeSession session : nodeRegistry.getAllSessions()) {
            serverGroups.add(session.nodeId());
        }
        return new BalancerConfigDto(balancerStrategy, vipPriority, serverGroups);
    }

    @Override
    public void setBalancerConfig(BalancerConfigDto config) {
        if (config.strategy() != null) {
            this.balancerStrategy = config.strategy();
        }
        this.vipPriority = config.vipPriority();
    }

    private double calculateAverageTps() {
        var sessions = nodeRegistry.getAllSessions();
        if (sessions.isEmpty()) {
            return 20.0;
        }
        return sessions.stream()
                .mapToDouble(s -> s.state().getTps())
                .average()
                .orElse(20.0);
    }

    private static final class EventCounter {
        private final AtomicLong count = new AtomicLong(0);
        private volatile long lastTriggered = 0;
        private volatile boolean active;

        EventCounter(boolean active) {
            this.active = active;
        }

        void increment() {
            count.incrementAndGet();
            lastTriggered = System.currentTimeMillis();
        }

        long getCount() {
            return count.get();
        }

        long getLastTriggered() {
            return lastTriggered;
        }

        boolean isActive() {
            return active;
        }
    }
}
