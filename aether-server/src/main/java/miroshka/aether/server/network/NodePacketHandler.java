package miroshka.aether.server.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import miroshka.aether.api.AetherAPIProvider;
import miroshka.aether.api.CircuitBreakerEvent;
import miroshka.aether.api.ConnectionStatus;
import miroshka.aether.common.event.AetherEventBus;
import miroshka.aether.common.event.NetworkStateUpdatedEvent;
import miroshka.aether.common.protocol.*;
import miroshka.aether.server.AetherServerAPI;
import miroshka.aether.server.config.NodeConfig;
import miroshka.aether.server.state.NetworkStateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class NodePacketHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodePacketHandler.class);

    private final NodeConfig config;
    private final NetworkStateCache stateCache;
    @Getter
    private final CompletableFuture<Boolean> authFuture;
    private final AtomicReference<ConnectionStatus.ConnectionState> connectionState;
    private final AtomicInteger latencyMillis;
    private final AtomicLong circuitBreakerEndTime;

    private volatile ChannelHandlerContext ctx;

    public NodePacketHandler(NodeConfig config, NetworkStateCache stateCache) {
        this.config = Objects.requireNonNull(config, "config");
        this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
        this.authFuture = new CompletableFuture<>();
        this.connectionState = new AtomicReference<>(ConnectionStatus.ConnectionState.CONNECTING);
        this.latencyMillis = new AtomicInteger(0);
        this.circuitBreakerEndTime = new AtomicLong(0);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        connectionState.set(ConnectionStatus.ConnectionState.AUTHENTICATING);

        AuthHandshakePacket handshake = new AuthHandshakePacket(
                ProtocolConstants.PROTOCOL_VERSION,
                config.serverName(),
                config.secretKey(),
                System.currentTimeMillis());
        ctx.writeAndFlush(handshake);
        LOGGER.info("Sent authentication handshake to Master");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Packet packet)) {
            return;
        }

        switch (packet) {
            case AuthResultPacket result -> handleAuthResult(result);
            case HeartbeatAckPacket ack -> handleHeartbeatAck(ack);
            case NetworkStatePacket state -> handleNetworkState(state);
            case CircuitBreakerTrippedPacket cb -> handleCircuitBreaker(cb);
            case ProtocolErrorPacket error -> handleProtocolError(error);
            default -> LOGGER.warn("Unexpected packet from Master: {}", packet.getClass().getSimpleName());
        }
    }

    private void handleAuthResult(AuthResultPacket result) {
        if (result.success()) {
            connectionState.set(ConnectionStatus.ConnectionState.CONNECTED);
            authFuture.complete(true);
            LOGGER.info("Authentication successful, protocol version: {}", result.negotiatedVersion());
        } else {
            connectionState.set(ConnectionStatus.ConnectionState.DISCONNECTED);
            authFuture.complete(false);
            LOGGER.error("Authentication failed: {}", result.reason());
            ctx.close();
        }
    }

    private void handleHeartbeatAck(HeartbeatAckPacket ack) {
        int rtt = (int) (System.currentTimeMillis() - ack.originalTimestamp());
        latencyMillis.set(rtt);
        LOGGER.debug("Heartbeat ACK: RTT={}ms, processing={}µs", rtt, ack.processingDelayMicros());
    }

    private void handleNetworkState(NetworkStatePacket state) {
        stateCache.update(state);
        AetherEventBus.instance().publish(NetworkStateUpdatedEvent.create(state));

        AetherAPIProvider.getInstance().ifPresent(api -> {
            if (api instanceof AetherServerAPI serverApi) {
                serverApi.notifyStateChanged(state);
            }
        });

        LOGGER.debug("Network state updated: v{}, {} global players, {} servers",
                state.stateVersion(), state.globalOnline(), state.serverCount());
    }

    private void handleCircuitBreaker(CircuitBreakerTrippedPacket cb) {
        circuitBreakerEndTime.set(System.currentTimeMillis() + cb.durationMillis());

        AetherAPIProvider.getInstance().ifPresent(api -> {
            if (api instanceof AetherServerAPI serverApi) {
                serverApi.notifyCircuitBreaker(CircuitBreakerEvent.tripped(cb.reason(), cb.durationMillis()));
            }
        });

        LOGGER.warn("Circuit breaker tripped: {} ({}ms)", cb.reason(), cb.durationMillis());
    }

    private void handleProtocolError(ProtocolErrorPacket error) {
        LOGGER.error("Protocol error from Master: {} - {} (packet 0x{})",
                error.errorCode(), error.details(), Integer.toHexString(error.failedPacketId()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        connectionState.set(ConnectionStatus.ConnectionState.DISCONNECTED);
        authFuture.completeExceptionally(new RuntimeException("Channel closed"));
        LOGGER.warn("Connection to Master lost");
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception in connection to Master: {}", cause.getMessage());
        ctx.close();
    }

    public ConnectionStatus.ConnectionState getConnectionState() {
        return connectionState.get();
    }

    public int getLatencyMillis() {
        return latencyMillis.get();
    }

    public boolean isCircuitBreakerActive() {
        return System.currentTimeMillis() < circuitBreakerEndTime.get();
    }

    public boolean isAuthenticated() {
        return connectionState.get() == ConnectionStatus.ConnectionState.CONNECTED;
    }
}
