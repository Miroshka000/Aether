package miroshka.aether.proxy;

import dev.waterdog.waterdogpe.plugin.Plugin;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.config.ProxyConfigLoader;
import miroshka.aether.proxy.network.MasterNetworkServer;

public final class AetherProxyPlugin extends Plugin {

    private MasterNetworkServer networkServer;

    @Override
    public void onEnable() {
        getLogger().info("Aether Proxy starting...");

        ProxyConfig config = loadAetherConfig();
        NodeRegistry nodeRegistry = new NodeRegistry();
        networkServer = new MasterNetworkServer(config, nodeRegistry);

        try {
            networkServer.start();
            getLogger().info("Aether Proxy enabled successfully");
        } catch (InterruptedException e) {
            getLogger().error("Failed to start Aether network server", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Aether Proxy shutting down...");

        if (networkServer != null) {
            networkServer.shutdown();
        }

        getLogger().info("Aether Proxy disabled");
    }

    private ProxyConfig loadAetherConfig() {
        return ProxyConfigLoader.load(getDataFolder().toPath());
    }
}
