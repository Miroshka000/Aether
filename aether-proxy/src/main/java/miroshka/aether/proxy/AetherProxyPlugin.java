package miroshka.aether.proxy;

import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.plugin.Plugin;
import lombok.Getter;
import miroshka.aether.proxy.balancer.ProxyLoadBalancer;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.config.ProxyConfigLoader;
import miroshka.aether.proxy.event.EventRouter;
import miroshka.aether.proxy.network.MasterNetworkServer;
import miroshka.aether.proxy.transfer.SeamlessTransferHandler;
import miroshka.aether.proxy.transport.TransportManager;
import miroshka.aether.proxy.web.ProxyWebContext;
import miroshka.aether.web.WebServer;

@Getter
public final class AetherProxyPlugin extends Plugin {

    private static final String LOG_PREFIX = "[Aether] ";

    private MasterNetworkServer networkServer;
    private WebServer webServer;
    private EventRouter eventRouter;
    private SeamlessTransferHandler transferHandler;
    private ProxyLoadBalancer loadBalancer;
    private TransportManager transportManager;
    private NodeRegistry nodeRegistry;
    private ProxyConfig proxyConfig;
    private ProxyWebContext webContext;

    @Override
    public void onStartup() {
        proxyConfig = loadAetherConfig();
        initializeTransport();
    }

    @Override
    public void onEnable() {
        logInfo("Aether Proxy starting...");

        nodeRegistry = new NodeRegistry();

        initializeServices();
        startTransportListening();
        startNetworkServer();
        startWebPanel();
        registerEventTracking();

        logInfo("Aether Proxy enabled successfully");
    }

    private void initializeTransport() {
        transportManager = new TransportManager(getProxy(), proxyConfig.transport());
        transportManager.initialize();

        if (proxyConfig.transport().enabled()) {
            logInfo("ProxyTransport enabled: " + proxyConfig.transport().protocol() +
                    " with " + proxyConfig.transport().compression() + " compression");
        }
    }

    private void initializeServices() {
        loadBalancer = new ProxyLoadBalancer(getProxy(), nodeRegistry);
        eventRouter = new EventRouter(nodeRegistry);
        transferHandler = new SeamlessTransferHandler(getProxy(), loadBalancer);

        logInfo("Services initialized: EventRouter, TransferHandler, LoadBalancer");
    }

    private void startTransportListening() {
        if (proxyConfig.transport().enabled()) {
            transportManager.startListening(nodeRegistry);
            logInfo("TransportManager listening for node connections");
        }
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
            webContext = new ProxyWebContext(nodeRegistry, getProxy());
            webServer = new WebServer(webPort, proxyConfig.webJwtSecret(), webContext);
            webServer.start();
            logInfo("Web Panel started on http://localhost:" + webPort);
        } catch (Exception e) {
            getLogger().error(LOG_PREFIX + "Failed to start Web Panel", e);
        }
    }

    private void registerEventTracking() {
        if (webContext == null)
            return;

        getProxy().getEventManager().subscribe(PlayerLoginEvent.class, event -> {
            webContext.trackEvent("PlayerJoin");
        });

        getProxy().getEventManager().subscribe(PlayerDisconnectedEvent.class, event -> {
            webContext.trackEvent("PlayerQuit");
        });

        getProxy().getEventManager().subscribe(TransferCompleteEvent.class, event -> {
            webContext.trackEvent("PlayerTransfer");
        });

        logInfo("Event tracking registered");
    }

    @Override
    public void onDisable() {
        logInfo("Aether Proxy shutting down...");

        if (transportManager != null) {
            transportManager.stopListening();
        }

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
}
