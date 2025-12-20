package miroshka.aether.proxy.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import miroshka.aether.common.event.AetherEventBus;
import miroshka.aether.common.event.AuthenticationCompletedEvent;
import miroshka.aether.common.event.ConnectionLostEvent;
import miroshka.aether.common.protocol.*;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.security.RateLimiter;
import miroshka.aether.proxy.security.SecretKeyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class MasterPacketHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterPacketHandler.class);

    private final NodeRegistry nodeRegistry;
    private final SecretKeyValidator secretKeyValidator;
    private final StateBroadcaster stateBroadcaster;
    private final RateLimiter rateLimiter;

    private volatile boolean authenticated;
    private volatile String nodeId;

    public MasterPacketHandler(
            NodeRegistry nodeRegistry,
            ProxyConfig config,
            SecretKeyValidator secretKeyValidator,
            StateBroadcaster stateBroadcaster) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.secretKeyValidator = Objects.requireNonNull(secretKeyValidator, "secretKeyValidator");
        this.stateBroadcaster = Objects.requireNonNull(stateBroadcaster, "stateBroadcaster");
        this.rateLimiter = new RateLimiter(config.rateLimitBurstSize(), config.rateLimitPacketsPerSecond());
        this.authenticated = false;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Packet packet)) {
            return;
        }

        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded for {}", ctx.channel().remoteAddress());
            return;
        }

        switch (packet) {
            case AuthHandshakePacket handshake -> handleAuthHandshake(ctx, handshake);
            case HeartbeatPacket heartbeat -> handleHeartbeat(ctx, heartbeat);
            case NodeSnapshotPacket snapshot -> handleNodeSnapshot(snapshot);
            case MetricsReportPacket metrics -> handleMetricsReport(metrics);
            case ProtocolErrorPacket error -> handleProtocolError(ctx, error);
            default -> LOGGER.warn("Unexpected packet type: {}", packet.getClass().getSimpleName());
        }
    }

    private void handleAuthHandshake(ChannelHandlerContext ctx, AuthHandshakePacket handshake) {
        if (authenticated) {
            ctx.writeAndFlush(AuthResultPacket.failure("Already authenticated"));
            return;
        }

        if (handshake.protocolVersion() != ProtocolConstants.PROTOCOL_VERSION) {
            ctx.writeAndFlush(AuthResultPacket.failure("Protocol version mismatch"));
            ctx.close();
            AetherEventBus.instance()
                    .publish(AuthenticationCompletedEvent.failure(handshake.serverName(), "Protocol mismatch"));
            return;
        }

        if (!secretKeyValidator.validate(handshake.secretKey())) {
            ctx.writeAndFlush(AuthResultPacket.failure("Invalid secret key"));
            ctx.close();
            AetherEventBus.instance()
                    .publish(AuthenticationCompletedEvent.failure(handshake.serverName(), "Invalid key"));
            return;
        }

        long clockSkew = Math.abs(System.currentTimeMillis() - handshake.timestamp());
        if (clockSkew > ProtocolConstants.CLOCK_SKEW_THRESHOLD_MILLIS) {
            LOGGER.warn("Clock skew detected for {}: {}ms", handshake.serverName(), clockSkew);
        }

        if (nodeRegistry.isRegistered(handshake.serverName())) {
            ctx.writeAndFlush(AuthResultPacket.failure("Node already connected: " + handshake.serverName()));
            ctx.close();
            return;
        }

        this.authenticated = true;
        this.nodeId = handshake.serverName();

        String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        NodeSession session = NodeSession.create(nodeId, remoteAddress, ctx.channel());
        nodeRegistry.register(session);

        ctx.writeAndFlush(AuthResultPacket.success(ProtocolConstants.PROTOCOL_VERSION, java.util.Map.of()));
        AetherEventBus.instance().publish(AuthenticationCompletedEvent.success(nodeId));

        LOGGER.info("Node authenticated: {} from {}", nodeId, remoteAddress);
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, HeartbeatPacket heartbeat) {
        if (!authenticated) {
            return;
        }

        long startTime = System.nanoTime();
        nodeRegistry.getByNodeId(nodeId).ifPresent(session -> session.state().updateHeartbeat());

        long processingDelay = (System.nanoTime() - startTime) / 1000;
        ctx.writeAndFlush(HeartbeatAckPacket.fromHeartbeat(heartbeat, processingDelay));
    }

    private void handleNodeSnapshot(NodeSnapshotPacket snapshot) {
        if (!authenticated) {
            return;
        }

        nodeRegistry.getByNodeId(nodeId).ifPresent(session -> {
            session.state().updateFromSnapshot(snapshot);
            LOGGER.debug("Snapshot received from {}: {} players, TPS {}",
                    nodeId, snapshot.onlinePlayers(), snapshot.tps());
        });
    }

    private void handleMetricsReport(MetricsReportPacket metrics) {
        if (!authenticated) {
            return;
        }
        LOGGER.debug("Metrics received from {}: {} TPS points, {} player points",
                nodeId, metrics.tpsHistory().size(), metrics.playerHistory().size());
    }

    private void handleProtocolError(ChannelHandlerContext ctx, ProtocolErrorPacket error) {
        LOGGER.error("Protocol error from {}: {} - {} (packet 0x{})",
                nodeId != null ? nodeId : ctx.channel().remoteAddress(),
                error.errorCode(), error.details(), Integer.toHexString(error.failedPacketId()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (authenticated && nodeId != null) {
            nodeRegistry.unregister(nodeId);
            AetherEventBus.instance().publish(ConnectionLostEvent.create(nodeId, "Channel closed"));
            stateBroadcaster.triggerEmergencyBroadcast();
            LOGGER.info("Node disconnected: {}", nodeId);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception in channel {}: {}",
                nodeId != null ? nodeId : ctx.channel().remoteAddress(),
                cause.getMessage());
        ctx.close();
    }
}
