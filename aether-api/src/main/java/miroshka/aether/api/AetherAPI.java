package miroshka.aether.api;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface AetherAPI {

    int getGlobalOnline();

    int getGlobalMaxPlayers();

    int getServerCount();

    Optional<String> getGlobalProperty(String key);

    Optional<String> getRoutingHint(String gameType);

    ConnectionStatus getConnectionStatus();

    void sendCustomProperty(String key, String value);

    <E extends AetherPublicEvent> void subscribe(Class<E> eventType, Consumer<E> handler);

    <E extends AetherPublicEvent> void unsubscribe(Class<E> eventType, Consumer<E> handler);

    long getLastStateVersion();

    boolean isStateStale();

    int getLatencyMillis();

    List<String> getServerNames();

    int getServerOnline(String serverName);

    int getServerMaxPlayers(String serverName);

    double getServerTps(String serverName);

    boolean isServerOnline(String serverName);

    static Optional<AetherAPI> getInstance() {
        return AetherAPIProvider.getInstance();
    }
}
