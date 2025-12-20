package miroshka.aether.server;

import miroshka.aether.api.*;
import miroshka.aether.api.balancer.LoadBalancer;
import miroshka.aether.api.chunk.ChunkStreaming;
import miroshka.aether.api.event.AetherEventBridge;
import miroshka.aether.api.pdc.DistributedPDC;
import miroshka.aether.api.portal.PortalManager;
import miroshka.aether.common.protocol.NetworkStatePacket;
import miroshka.aether.common.protocol.ServerInfo;
import miroshka.aether.server.network.NodeNetworkClient;
import miroshka.aether.server.state.NetworkStateCache;
import miroshka.aether.server.state.SnapshotCollector;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class AetherServerAPI implements AetherAPI {

    private final NodeNetworkClient networkClient;
    private final NetworkStateCache stateCache;
    private final SnapshotCollector snapshotCollector;
    private final Map<Class<? extends AetherPublicEvent>, List<Consumer<? extends AetherPublicEvent>>> eventHandlers;

    private volatile PortalManager portalManager;
    private volatile AetherEventBridge eventBridge;
    private volatile DistributedPDC distributedPDC;
    private volatile LoadBalancer loadBalancer;
    private volatile ChunkStreaming chunkStreaming;

    public AetherServerAPI(
            NodeNetworkClient networkClient,
            NetworkStateCache stateCache,
            SnapshotCollector snapshotCollector) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
        this.snapshotCollector = Objects.requireNonNull(snapshotCollector, "snapshotCollector");
        this.eventHandlers = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    public int getGlobalOnline() {
        return stateCache.getGlobalOnline();
    }

    @Override
    public int getGlobalMaxPlayers() {
        return stateCache.getServers().stream()
                .mapToInt(ServerInfo::maxPlayers)
                .sum();
    }

    @Override
    public int getServerCount() {
        return stateCache.getServerCount();
    }

    @Override
    public Optional<String> getGlobalProperty(String key) {
        Objects.requireNonNull(key, "key");
        return stateCache.getGlobalProperty(key);
    }

    @Override
    public Optional<String> getRoutingHint(String gameType) {
        Objects.requireNonNull(gameType, "gameType");
        return stateCache.getRoutingHint(gameType + "_least_loaded");
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return networkClient.getConnectionStatus();
    }

    @Override
    public void sendCustomProperty(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        snapshotCollector.setExtraData(key, value);
    }

    @Override
    public <E extends AetherPublicEvent> void subscribe(Class<E> eventType, Consumer<E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        eventHandlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    @Override
    public <E extends AetherPublicEvent> void unsubscribe(Class<E> eventType, Consumer<E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        List<Consumer<? extends AetherPublicEvent>> handlers = eventHandlers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    @Override
    public long getLastStateVersion() {
        return stateCache.getStateVersion();
    }

    @Override
    public boolean isStateStale() {
        return stateCache.isStale();
    }

    @Override
    public int getLatencyMillis() {
        return networkClient.getLatencyMillis();
    }

    @Override
    public List<String> getServerNames() {
        return stateCache.getServers().stream()
                .map(ServerInfo::name)
                .toList();
    }

    @Override
    public int getServerOnline(String serverName) {
        return stateCache.getServerOnline(serverName);
    }

    @Override
    public int getServerMaxPlayers(String serverName) {
        return stateCache.getServerMaxPlayers(serverName);
    }

    @Override
    public double getServerTps(String serverName) {
        return stateCache.getServerTps(serverName);
    }

    @Override
    public boolean isServerOnline(String serverName) {
        return stateCache.getServer(serverName)
                .map(ServerInfo::isOnline)
                .orElse(false);
    }

    @Override
    public Optional<PortalManager> getPortalManager() {
        return Optional.ofNullable(portalManager);
    }

    @Override
    public Optional<AetherEventBridge> getEventBridge() {
        return Optional.ofNullable(eventBridge);
    }

    @Override
    public Optional<DistributedPDC> getDistributedPDC() {
        return Optional.ofNullable(distributedPDC);
    }

    @Override
    public Optional<LoadBalancer> getLoadBalancer() {
        return Optional.ofNullable(loadBalancer);
    }

    @Override
    public Optional<ChunkStreaming> getChunkStreaming() {
        return Optional.ofNullable(chunkStreaming);
    }

    public void setPortalManager(PortalManager portalManager) {
        this.portalManager = portalManager;
    }

    public void setEventBridge(AetherEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }

    public void setDistributedPDC(DistributedPDC distributedPDC) {
        this.distributedPDC = distributedPDC;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public void setChunkStreaming(ChunkStreaming chunkStreaming) {
        this.chunkStreaming = chunkStreaming;
    }

    public void notifyStateChanged(NetworkStatePacket state) {
        NetworkStateChangedEvent event = new NetworkStateChangedEvent(
                state.globalOnline(),
                state.serverCount(),
                state.stateVersion(),
                state.globalProperties(),
                state.routingHints(),
                System.currentTimeMillis());
        dispatchEvent(NetworkStateChangedEvent.class, event);
    }

    public void notifyCircuitBreaker(CircuitBreakerEvent event) {
        dispatchEvent(CircuitBreakerEvent.class, event);
    }

    @SuppressWarnings("unchecked")
    private <E extends AetherPublicEvent> void dispatchEvent(Class<E> eventType, E event) {
        List<Consumer<? extends AetherPublicEvent>> handlers = eventHandlers.get(eventType);
        if (handlers != null) {
            for (Consumer<? extends AetherPublicEvent> handler : handlers) {
                ((Consumer<E>) handler).accept(event);
            }
        }
    }
}
