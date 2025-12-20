package miroshka.aether.common.protocol;

import java.util.Map;
import java.util.Objects;

public record ServerInfo(
        String name,
        int onlinePlayers,
        int maxPlayers,
        double tps,
        long lastUpdateTimestamp,
        Map<String, String> extraData) {

    public ServerInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(extraData, "extraData");
    }

    public boolean isOnline() {
        return System.currentTimeMillis() - lastUpdateTimestamp < 15000;
    }

    public double getLoadPercentage() {
        if (maxPlayers == 0)
            return 0.0;
        return (double) onlinePlayers / maxPlayers * 100.0;
    }
}
