package miroshka.aether.server;

import lombok.Getter;
import miroshka.aether.api.AetherAPIProvider;
import miroshka.aether.common.protocol.ProtocolConstants;
import miroshka.aether.server.config.NodeConfig;
import miroshka.aether.server.config.NodeConfigLoader;
import miroshka.aether.server.network.NodeNetworkClient;
import miroshka.aether.server.placeholder.AetherPlaceholders;
import miroshka.aether.server.state.NetworkStateCache;
import miroshka.aether.server.state.SnapshotCollector;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;

public final class AetherServerPlugin extends Plugin {

    @Getter
    private NodeNetworkClient networkClient;
    @Getter
    private NetworkStateCache stateCache;

    @Getter
    private AetherServerAPI api;
    private AetherPlaceholders placeholders;

    @Override
    public void onEnable() {
        getPluginLogger().info("Aether Server starting...");

        NodeConfig config = loadConfig();

        stateCache = new NetworkStateCache(ProtocolConstants.STATE_STALE_THRESHOLD_MILLIS);

        SnapshotCollector snapshotCollector = new SnapshotCollector(
                () -> Server.getInstance().getPlayerManager().getPlayerCount(),
                () -> Server.getInstance().getPlayerManager().getMaxPlayerCount(),
                this::getAverageTps);

        networkClient = new NodeNetworkClient(config, stateCache, snapshotCollector);

        api = new AetherServerAPI(networkClient, stateCache, snapshotCollector);
        AetherAPIProvider.register(api);

        networkClient.start();

        Server.getInstance().getScheduler().scheduleDelayed(this, () -> {
            placeholders = new AetherPlaceholders(this, api);
            placeholders.register();
            return false;
        }, 100);

        getPluginLogger().info("Aether Server enabled successfully");
    }

    @Override
    public void onDisable() {
        getPluginLogger().info("Aether Server shutting down...");

        if (placeholders != null) {
            placeholders.unregister();
        }

        if (networkClient != null) {
            networkClient.stop();
        }

        AetherAPIProvider.unregister();

        getPluginLogger().info("Aether Server disabled");
    }

    private double getAverageTps() {
        var worlds = Server.getInstance().getWorldPool().getWorlds().values();
        if (worlds.isEmpty()) {
            return 20.0;
        }
        return worlds.stream()
                .mapToDouble(org.allaymc.api.world.World::getTPS)
                .average()
                .orElse(20.0);
    }

    private NodeConfig loadConfig() {
        return NodeConfigLoader.load(getPluginContainer().dataFolder());
    }

}
