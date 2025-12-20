package miroshka.aether.server.state;

import miroshka.aether.common.protocol.NetworkStatePacket;
import miroshka.aether.common.protocol.ServerInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class NetworkStateCache {

    private final AtomicReference<NetworkStatePacket> cachedState;
    private final AtomicLong lastUpdateTimestamp;
    private final long staleThresholdMillis;

    public NetworkStateCache(long staleThresholdMillis) {
        this.cachedState = new AtomicReference<>();
        this.lastUpdateTimestamp = new AtomicLong(0);
        this.staleThresholdMillis = staleThresholdMillis;
    }

    public void update(NetworkStatePacket state) {
        Objects.requireNonNull(state, "state");
        cachedState.set(state);
        lastUpdateTimestamp.set(System.currentTimeMillis());
    }

    public int getGlobalOnline() {
        NetworkStatePacket state = cachedState.get();
        return state != null ? state.globalOnline() : 0;
    }

    public int getServerCount() {
        NetworkStatePacket state = cachedState.get();
        return state != null ? state.serverCount() : 0;
    }

    public long getStateVersion() {
        NetworkStatePacket state = cachedState.get();
        return state != null ? state.stateVersion() : 0;
    }

    public Optional<String> getGlobalProperty(String key) {
        Objects.requireNonNull(key, "key");
        NetworkStatePacket state = cachedState.get();
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.globalProperties().get(key));
    }

    public Optional<String> getRoutingHint(String key) {
        Objects.requireNonNull(key, "key");
        NetworkStatePacket state = cachedState.get();
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.routingHints().get(key));
    }

    public boolean isStale() {
        long lastUpdate = lastUpdateTimestamp.get();
        if (lastUpdate == 0) {
            return true;
        }
        return System.currentTimeMillis() - lastUpdate > staleThresholdMillis;
    }

    public List<ServerInfo> getServers() {
        NetworkStatePacket state = cachedState.get();
        return state != null ? state.servers() : List.of();
    }

    public Optional<ServerInfo> getServer(String name) {
        return getServers().stream()
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public int getServerOnline(String name) {
        return getServer(name).map(ServerInfo::onlinePlayers).orElse(0);
    }

    public int getServerMaxPlayers(String name) {
        return getServer(name).map(ServerInfo::maxPlayers).orElse(0);
    }

    public double getServerTps(String name) {
        return getServer(name).map(ServerInfo::tps).orElse(0.0);
    }
}
