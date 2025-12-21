package miroshka.aether.proxy.transport;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.protocol.ProtocolCodecs;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import lombok.Getter;
import miroshka.aether.common.event.AetherEventBus;
import miroshka.aether.common.event.AuthenticationCompletedEvent;
import miroshka.aether.common.event.ConnectionLostEvent;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;
import miroshka.aether.proxy.config.TransportConfig;
import org.nethergames.proxytransport.integration.QuicTransportServerInfo;
import org.nethergames.proxytransport.integration.TcpTransportServerInfo;
import org.nethergames.proxytransport.utils.CodecUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class TransportManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportManager.class);
    private static final int DEFAULT_BEDROCK_PORT = 19132;

    private final ProxyServer proxyServer;
    private final TransportConfig config;
    private final Set<String> transportServers = new HashSet<>();

    @Getter
    private boolean initialized = false;

    private NodeRegistry nodeRegistry;
    private Consumer<AuthenticationCompletedEvent> authHandler;
    private Consumer<ConnectionLostEvent> disconnectHandler;

    public TransportManager(ProxyServer proxyServer, TransportConfig config) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void initialize() {
        if (!config.enabled()) {
            LOGGER.info("ProxyTransport is disabled");
            return;
        }

        ProtocolCodecs.addUpdater(new CodecUpdater());

        LOGGER.info("ProxyTransport initialized with protocol={}, compression={}",
                config.protocol(), config.compression());
        LOGGER.info("Registered transport types: {}, {}",
                TcpTransportServerInfo.TYPE.getIdentifier(),
                QuicTransportServerInfo.TYPE.getIdentifier());

        initialized = true;
    }

    public void startListening(NodeRegistry nodeRegistry) {
        if (!config.enabled() || !initialized) {
            return;
        }

        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");

        authHandler = this::onNodeAuthenticated;
        disconnectHandler = this::onNodeDisconnected;

        AetherEventBus.instance().subscribe(AuthenticationCompletedEvent.class, authHandler);
        AetherEventBus.instance().subscribe(ConnectionLostEvent.class, disconnectHandler);

        LOGGER.info("TransportManager listening for node connections");
    }

    public void stopListening() {
        if (authHandler != null) {
            AetherEventBus.instance().unsubscribe(AuthenticationCompletedEvent.class, authHandler);
        }
        if (disconnectHandler != null) {
            AetherEventBus.instance().unsubscribe(ConnectionLostEvent.class, disconnectHandler);
        }
    }

    private void onNodeAuthenticated(AuthenticationCompletedEvent event) {
        if (!event.success()) {
            return;
        }

        String nodeId = event.nodeId();
        if (!shouldUseTransport(nodeId)) {
            LOGGER.debug("Node {} not in transport servers list, skipping", nodeId);
            return;
        }

        nodeRegistry.getByNodeId(nodeId).ifPresent(session -> {
            String host = extractHost(session.remoteAddress());
            int port = extractPort(session);

            registerServer(nodeId, host, port);
        });
    }

    private void onNodeDisconnected(ConnectionLostEvent event) {
        unregisterServer(event.nodeId());
    }

    public void registerServer(String serverName, String host, int port) {
        if (!config.enabled() || !initialized) {
            return;
        }

        if (transportServers.contains(serverName)) {
            LOGGER.debug("Server {} already registered with transport", serverName);
            return;
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        ServerInfo serverInfo = createServerInfo(serverName, address);

        proxyServer.registerServerInfo(serverInfo);
        transportServers.add(serverName);

        LOGGER.info("Registered server {} with {} transport at {}:{}",
                serverName, config.protocol(), host, port);
    }

    public void unregisterServer(String serverName) {
        if (transportServers.remove(serverName)) {
            proxyServer.removeServerInfo(serverName);
            LOGGER.info("Unregistered transport server: {}", serverName);
        }
    }

    public boolean isTransportServer(String serverName) {
        return transportServers.contains(serverName);
    }

    private boolean shouldUseTransport(String serverName) {
        if (config.servers().isEmpty()) {
            return true;
        }
        return config.servers().contains(serverName);
    }

    private ServerInfo createServerInfo(String serverName, InetSocketAddress address) {
        return switch (config.protocol()) {
            case TCP -> new TcpTransportServerInfo(serverName, address, address);
            case QUIC -> new QuicTransportServerInfo(serverName, address, address);
        };
    }

    private String extractHost(String remoteAddress) {
        if (remoteAddress.contains(":")) {
            return remoteAddress.substring(0, remoteAddress.lastIndexOf(':'));
        }
        return remoteAddress;
    }

    private int extractPort(NodeSession session) {
        String portStr = session.state().getAllExtraData().get("bedrock-port");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_BEDROCK_PORT;
    }
}
