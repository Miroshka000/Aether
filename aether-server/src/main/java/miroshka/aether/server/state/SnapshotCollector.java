package miroshka.aether.server.state;

import miroshka.aether.common.protocol.NodeSnapshotPacket;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class SnapshotCollector {

    private final AtomicInteger onlinePlayers;
    private final AtomicInteger maxPlayers;
    private final AtomicReference<Double> tps;
    private final Map<String, String> extraData;

    private final Supplier<Integer> onlinePlayersSupplier;
    private final Supplier<Integer> maxPlayersSupplier;
    private final Supplier<Double> tpsSupplier;

    public SnapshotCollector(
            Supplier<Integer> onlinePlayersSupplier,
            Supplier<Integer> maxPlayersSupplier,
            Supplier<Double> tpsSupplier) {
        this.onlinePlayersSupplier = Objects.requireNonNull(onlinePlayersSupplier, "onlinePlayersSupplier");
        this.maxPlayersSupplier = Objects.requireNonNull(maxPlayersSupplier, "maxPlayersSupplier");
        this.tpsSupplier = Objects.requireNonNull(tpsSupplier, "tpsSupplier");
        this.onlinePlayers = new AtomicInteger(0);
        this.maxPlayers = new AtomicInteger(20);
        this.tps = new AtomicReference<>(20.0);
        this.extraData = new ConcurrentHashMap<>();
    }

    private void refresh() {
        int newOnline = onlinePlayersSupplier.get();
        int newMax = maxPlayersSupplier.get();
        double newTps = tpsSupplier.get();

        onlinePlayers.set(newOnline);
        maxPlayers.set(newMax);
        tps.set(newTps);
    }

    public void setExtraData(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        extraData.put(key, value);
    }

    public NodeSnapshotPacket captureSnapshot() {
        refresh();
        return new NodeSnapshotPacket(
                onlinePlayers.get(),
                maxPlayers.get(),
                tps.get(),
                System.currentTimeMillis(),
                Map.copyOf(extraData));
    }
}
