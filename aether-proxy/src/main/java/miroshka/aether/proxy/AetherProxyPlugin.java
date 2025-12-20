package miroshka.aether.proxy;

import dev.waterdog.waterdogpe.plugin.Plugin;
import miroshka.aether.proxy.balancer.ProxyLoadBalancer;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.config.ProxyConfigLoader;
import miroshka.aether.proxy.event.EventRouter;
import miroshka.aether.proxy.network.MasterNetworkServer;
import miroshka.aether.proxy.rewrite.PacketRewriteService;
import miroshka.aether.proxy.transfer.SeamlessTransferHandler;
import miroshka.aether.web.WebServer;

public final class AetherProxyPlugin extends Plugin {

    private static final String LOG_PREFIX = "[Aether] ";

    private MasterNetworkServer networkServer;
    private WebServer webServer;
    private EventRouter eventRouter;
    private SeamlessTransferHandler transferHandler;
    private ProxyLoadBalancer loadBalancer;
    private PacketRewriteService packetRewriteService;
    private NodeRegistry nodeRegistry;
    private ProxyConfig proxyConfig;

    @Override
    public void onEnable() {
        logInfo("Aether Proxy starting...");

        proxyConfig = loadAetherConfig();
        nodeRegistry = new NodeRegistry();

        initializeServices();
        startNetworkServer();
        startWebPanel();

        logInfo("Aether Proxy enabled successfully");
    }

    private void initializeServices() {
        eventRouter = new EventRouter(getProxy(), nodeRegistry);
        transferHandler = new SeamlessTransferHandler(getProxy());
        loadBalancer = new ProxyLoadBalancer(getProxy(), nodeRegistry);
        packetRewriteService = new PacketRewriteService();

        logInfo("Services initialized: EventRouter, TransferHandler, LoadBalancer, PacketRewrite");
    }

    private void startNetworkServer() {
        networkServer = new MasterNetworkServer(proxyConfig, nodeRegistry, eventRouter, transferHandler);

        try {
            networkServer.start();
            logInfo("Network server started on port " + proxyConfig.network().port());
        } catch (InterruptedException e) {
            getLogger().error(LOG_PREFIX + "Failed to start network server", e);
            Thread.currentThread().interrupt();
        }
    }

    private void startWebPanel() {
        int webPort = proxyConfig.network().webPort();
        if (webPort <= 0) {
            logInfo("Web Panel disabled (web-port = 0)");
            return;
        }

        try {
            webServer = new WebServer(webPort, proxyConfig.webJwtSecret(), nodeRegistry);
            webServer.start();
            logInfo("Web Panel started on http://localhost:" + webPort);
        } catch (Exception e) {
            getLogger().error(LOG_PREFIX + "Failed to start Web Panel", e);
        }
    }

    @Override
    public void onDisable() {
        logInfo("Aether Proxy shutting down...");

        if (webServer != null) {
            webServer.stop();
        }

        if (networkServer != null) {
            networkServer.shutdown();
        }

        logInfo("Aether Proxy disabled");
    }

    private void logInfo(String message) {
        getLogger().info(LOG_PREFIX + message);
    }

    private ProxyConfig loadAetherConfig() {
        return ProxyConfigLoader.load(getDataFolder().toPath());
    }

    public MasterNetworkServer getNetworkServer() {
        return networkServer;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public EventRouter getEventRouter() {
        return eventRouter;
    }

    public SeamlessTransferHandler getTransferHandler() {
        return transferHandler;
    }

    public ProxyLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public PacketRewriteService getPacketRewriteService() {
        return packetRewriteService;
    }
}
