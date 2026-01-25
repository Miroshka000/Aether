package miroshka.aether.server;

import lombok.Getter;
import miroshka.aether.api.AetherAPIProvider;
import miroshka.aether.common.event.AetherEventBus;
import miroshka.aether.common.event.EventBroadcastReceivedEvent;
import miroshka.aether.common.event.PDCSyncReceivedEvent;
import miroshka.aether.common.protocol.ProtocolConstants;
import miroshka.aether.server.balancer.LoadBalancerService;
import miroshka.aether.server.config.NodeConfig;
import miroshka.aether.server.config.NodeConfigLoader;
import miroshka.aether.server.event.EventBridgeService;
import miroshka.aether.server.network.NodeNetworkClient;
import miroshka.aether.server.pdc.DistributedPDCService;
import miroshka.aether.server.placeholder.AetherPlaceholders;
import miroshka.aether.server.portal.PortalManagerService;
import miroshka.aether.server.state.NetworkStateCache;
import miroshka.aether.server.state.SnapshotCollector;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;

import java.util.function.Consumer;

public final class AetherServerPlugin extends Plugin {

    @Getter
    private NodeNetworkClient networkClient;
    @Getter
    private NetworkStateCache stateCache;
    @Getter
    private AetherServerAPI api;

    private PortalManagerService portalManager;
    private EventBridgeService eventBridge;
    private DistributedPDCService distributedPDC;
    private LoadBalancerService loadBalancer;
    private AetherPlaceholders placeholders;

    private Consumer<PDCSyncReceivedEvent> pdcEventHandler;
    private Consumer<EventBroadcastReceivedEvent> eventBroadcastHandler;

    @Override
    public void onEnable() {
        getPluginLogger().info("Aether Server starting...");

        NodeConfig config = loadConfig();

        stateCache = new NetworkStateCache(ProtocolConstants.STATE_STALE_THRESHOLD_MILLIS);

        SnapshotCollector snapshotCollector = new SnapshotCollector(
                () -> Server.getInstance().getPlayerManager().getPlayerCount(),
                () -> Server.getInstance().getPlayerManager().getMaxPlayerCount(),
                this::getAverageTps);
        snapshotCollector.setExtraData("bedrock-port", String.valueOf(config.bedrockPort()));

        networkClient = new NodeNetworkClient(config, stateCache, snapshotCollector);

        initializeServices(config);
        subscribeToEvents();

        api = new AetherServerAPI(networkClient, stateCache, snapshotCollector);
        registerServicesInAPI();
        AetherAPIProvider.register(api);

        networkClient.start();

        Server.getInstance().getScheduler().scheduleDelayed(this, () -> {
            placeholders = new AetherPlaceholders(this, api);
            placeholders.register();
            return false;
        }, 100);

        logStartupInfo();
    }

    private void initializeServices(NodeConfig config) {
        portalManager = new PortalManagerService(this, networkClient, config.serverName());
        portalManager.loadFromConfig(getPluginContainer().dataFolder());
        portalManager.registerEvents();

        eventBridge = new EventBridgeService(networkClient, config.serverName());

        distributedPDC = new DistributedPDCService(networkClient, config.serverName());

        loadBalancer = new LoadBalancerService(stateCache);
    }

    private void subscribeToEvents() {
        pdcEventHandler = event -> distributedPDC.handleIncomingSync(event.packet());
        AetherEventBus.instance().subscribe(PDCSyncReceivedEvent.class, pdcEventHandler);

        eventBroadcastHandler = event -> eventBridge.handleIncomingEvent(event.packet());
        AetherEventBus.instance().subscribe(EventBroadcastReceivedEvent.class, eventBroadcastHandler);
    }

    private void unsubscribeFromEvents() {
        if (pdcEventHandler != null) {
            AetherEventBus.instance().unsubscribe(PDCSyncReceivedEvent.class, pdcEventHandler);
        }
        if (eventBroadcastHandler != null) {
            AetherEventBus.instance().unsubscribe(EventBroadcastReceivedEvent.class, eventBroadcastHandler);
        }
    }

    private void registerServicesInAPI() {
        api.setPortalManager(portalManager);
        api.setEventBridge(eventBridge);
        api.setDistributedPDC(distributedPDC);
        api.setLoadBalancer(loadBalancer);
    }

    private void logStartupInfo() {
        getPluginLogger().info("Aether Server enabled successfully");
    }

    @Override
    public void onDisable() {
        getPluginLogger().info("Aether Server shutting down...");

        if (placeholders != null) {
            placeholders.unregister();
        }

        unsubscribeFromEvents();

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
