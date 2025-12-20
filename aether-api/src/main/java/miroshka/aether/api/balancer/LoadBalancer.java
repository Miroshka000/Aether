package miroshka.aether.api.balancer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadBalancer {

    Optional<String> selectServer(UUID playerUuid, List<String> candidates);

    Optional<String> selectServer(UUID playerUuid, List<String> candidates, BalancingStrategy strategy);

    void setDefaultStrategy(BalancingStrategy strategy);

    BalancingStrategy getDefaultStrategy();

    void setPlayerPriority(UUID playerUuid, int priority);

    int getPlayerPriority(UUID playerUuid);

    void reserveSlot(String serverName, UUID playerUuid, long durationMs);

    void releaseSlot(String serverName, UUID playerUuid);

    ServerMetrics getServerMetrics(String serverName);

    List<ServerMetrics> getAllServerMetrics();

    enum BalancingStrategy {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        LEAST_TPS_LOAD,
        WEIGHTED,
        RANDOM,
        PRIORITY_QUEUE
    }

    record ServerMetrics(
            String serverName,
            int onlinePlayers,
            int maxPlayers,
            double tps,
            int weight,
            long lastUpdate,
            boolean available) {

        public double loadFactor() {
            if (maxPlayers == 0) {
                return 1.0;
            }
            return (double) onlinePlayers / maxPlayers;
        }

        public double tpsLoadFactor() {
            return Math.max(0, 1.0 - (tps / 20.0));
        }

        public boolean hasSpace() {
            return available && onlinePlayers < maxPlayers;
        }

        public int availableSlots() {
            return Math.max(0, maxPlayers - onlinePlayers);
        }
    }

    interface WeightProvider {
        int getWeight(String serverName, ServerMetrics metrics);

        static WeightProvider fixed(int weight) {
            return (name, metrics) -> weight;
        }

        static WeightProvider byAvailableSlots() {
            return (name, metrics) -> metrics.availableSlots();
        }

        static WeightProvider byTps() {
            return (name, metrics) -> (int) (metrics.tps() * 5);
        }
    }

    void setWeightProvider(WeightProvider provider);
}
