package miroshka.aether.proxy.network;

import miroshka.aether.common.protocol.NetworkStatePacket;
import miroshka.aether.common.protocol.ProtocolConstants;
import miroshka.aether.common.protocol.ServerInfo;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class StateBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateBroadcaster.class);

    private final NodeRegistry nodeRegistry;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong stateVersion;
    private final int broadcastIntervalMillis;

    private volatile boolean running;

    public StateBroadcaster(NodeRegistry nodeRegistry, int broadcastIntervalMillis) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.broadcastIntervalMillis = broadcastIntervalMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Aether-StateBroadcaster");
            t.setDaemon(true);
            return t;
        });
        this.stateVersion = new AtomicLong(0);
        this.running = false;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        scheduler.scheduleAtFixedRate(
                this::broadcastIfNeeded,
                broadcastIntervalMillis,
                broadcastIntervalMillis,
                TimeUnit.MILLISECONDS);
        LOGGER.info("StateBroadcaster started with {}ms interval", broadcastIntervalMillis);
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("StateBroadcaster stopped");
    }

    public void triggerEmergencyBroadcast() {
        if (running) {
            scheduler.execute(this::broadcast);
        }
    }

    private void broadcastIfNeeded() {
        if (nodeRegistry.hasAnyDirtyState() || nodeRegistry.getNodeCount() > 0) {
            broadcast();
        }
    }

    private void broadcast() {
        try {
            NetworkStatePacket state = buildNetworkState();
            var sessions = nodeRegistry.getAllSessions();

            for (NodeSession session : sessions) {
                if (session.isActive()) {
                    session.channel().writeAndFlush(state);
                }
            }

            nodeRegistry.clearAllDirtyFlags();
            LOGGER.debug("Broadcast state v{} to {} nodes: {} global players",
                    state.stateVersion(), sessions.size(), state.globalOnline());
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast state", e);
        }
    }

    private NetworkStatePacket buildNetworkState() {
        int globalOnline = nodeRegistry.getTotalOnlinePlayers();
        int serverCount = nodeRegistry.getNodeCount();
        long version = stateVersion.incrementAndGet();

        Map<String, String> globalProperties = new HashMap<>();
        Map<String, String> routingHints = buildRoutingHints();
        List<ServerInfo> servers = buildServerList();

        return new NetworkStatePacket(
                globalOnline,
                serverCount,
                version,
                ProtocolConstants.BROADCAST_INTERVAL_MILLIS / 1000 * 2,
                globalProperties,
                routingHints,
                servers);
    }

    private List<ServerInfo> buildServerList() {
        List<ServerInfo> servers = new ArrayList<>();
        for (NodeSession session : nodeRegistry.getAllSessions()) {
            var state = session.state();
            servers.add(new ServerInfo(
                    session.nodeId(),
                    state.getOnlinePlayers(),
                    state.getMaxPlayers(),
                    state.getTps(),
                    state.getLastUpdateTimestamp(),
                    state.getAllExtraData()));
        }
        return servers;
    }

    private Map<String, String> buildRoutingHints() {
        Map<String, String> hints = new HashMap<>();
        Map<String, NodeSession> lowestLoadByType = new HashMap<>();

        for (NodeSession session : nodeRegistry.getAllSessions()) {
            var extraData = session.state().getAllExtraData();
            String gameType = extraData.get("game_type");
            if (gameType != null) {
                NodeSession current = lowestLoadByType.get(gameType);
                if (current == null || session.state().getOnlinePlayers() < current.state().getOnlinePlayers()) {
                    lowestLoadByType.put(gameType, session);
                }
            }
        }

        lowestLoadByType.forEach((type, session) -> hints.put(type + "_least_loaded", session.nodeId()));

        return hints;
    }
}
