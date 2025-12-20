package miroshka.aether.server.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import miroshka.aether.api.ConnectionStatus;
import miroshka.aether.common.protocol.HeartbeatPacket;
import miroshka.aether.common.protocol.NodeSnapshotPacket;
import miroshka.aether.common.protocol.ProtocolConstants;
import miroshka.aether.server.config.NodeConfig;
import miroshka.aether.server.state.NetworkStateCache;
import miroshka.aether.server.state.SnapshotCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class NodeNetworkClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeNetworkClient.class);

    private final NodeConfig config;
    private final NetworkStateCache stateCache;
    private final SnapshotCollector snapshotCollector;
    private final ScheduledExecutorService scheduler;

    private final AtomicReference<Channel> channel;
    private final AtomicReference<NodePacketHandler> handler;
    private final AtomicInteger heartbeatSequence;
    private final AtomicInteger reconnectDelay;

    private EventLoopGroup workerGroup;
    private volatile boolean running;

    public NodeNetworkClient(NodeConfig config, NetworkStateCache stateCache, SnapshotCollector snapshotCollector) {
        this.config = Objects.requireNonNull(config, "config");
        this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
        this.snapshotCollector = Objects.requireNonNull(snapshotCollector, "snapshotCollector");
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Aether-NodeScheduler");
            t.setDaemon(true);
            return t;
        });
        this.channel = new AtomicReference<>();
        this.handler = new AtomicReference<>();
        this.heartbeatSequence = new AtomicInteger(0);
        this.reconnectDelay = new AtomicInteger(config.reconnectionInitialDelayMillis());
        this.running = false;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        @SuppressWarnings("deprecation")
        EventLoopGroup group = new NioEventLoopGroup();
        workerGroup = group;

        connect();

        scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                config.heartbeatIntervalMillis(),
                config.heartbeatIntervalMillis(),
                TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(
                this::sendSnapshotIfNeeded,
                config.snapshotIntervalMillis(),
                config.snapshotIntervalMillis(),
                TimeUnit.MILLISECONDS);

        LOGGER.info("NodeNetworkClient started");
    }

    public void stop() {
        running = false;
        scheduler.shutdown();

        Channel ch = channel.get();
        if (ch != null) {
            ch.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("NodeNetworkClient stopped");
    }

    private void connect() {
        if (!running) {
            return;
        }

        LOGGER.info("Connecting to Master at {}:{}", config.masterHost(), config.masterPort());

        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ProtocolConstants.CONNECTION_TIMEOUT_MILLIS)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new NodeChannelInitializer(config, stateCache, handler::set));

        ChannelFuture future = bootstrap.connect(config.masterHost(), config.masterPort());
        future.addListener(f -> {
            if (f.isSuccess()) {
                channel.set(future.channel());
                reconnectDelay.set(config.reconnectionInitialDelayMillis());
                LOGGER.info("Connected to Master");

                future.channel().closeFuture().addListener(cf -> {
                    if (running) {
                        scheduleReconnect();
                    }
                });
            } else {
                LOGGER.error("Failed to connect to Master: {}", f.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        if (!running) {
            return;
        }

        int delay = reconnectDelay.get();
        LOGGER.info("Reconnecting in {}ms", delay);

        scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);

        int newDelay = Math.min(delay * ProtocolConstants.RECONNECTION_MULTIPLIER, config.reconnectionMaxDelayMillis());
        reconnectDelay.set(newDelay);
    }

    private void sendHeartbeat() {
        NodePacketHandler h = handler.get();
        if (h == null || !h.isAuthenticated()) {
            return;
        }

        Channel ch = channel.get();
        if (ch == null || !ch.isActive()) {
            return;
        }

        HeartbeatPacket heartbeat = HeartbeatPacket.create(heartbeatSequence.incrementAndGet());
        ch.writeAndFlush(heartbeat);
    }

    private void sendSnapshotIfNeeded() {
        NodePacketHandler h = handler.get();
        if (h == null || !h.isAuthenticated()) {
            return;
        }

        if (h.isCircuitBreakerActive()) {
            return;
        }

        Channel ch = channel.get();
        if (ch == null || !ch.isActive()) {
            return;
        }

        NodeSnapshotPacket snapshot = snapshotCollector.captureSnapshot();
        ch.writeAndFlush(snapshot);
    }

    public boolean isConnected() {
        Channel ch = channel.get();
        NodePacketHandler h = handler.get();
        return ch != null && ch.isActive() && h != null && h.isAuthenticated();
    }

    public ConnectionStatus getConnectionStatus() {
        NodePacketHandler h = handler.get();
        if (h == null) {
            return ConnectionStatus.disconnected();
        }

        return new ConnectionStatus(
                isConnected(),
                h.getLatencyMillis(),
                stateCache.isStale(),
                config.masterAddress(),
                h.getConnectionState());
    }

    public int getLatencyMillis() {
        NodePacketHandler h = handler.get();
        return h != null ? h.getLatencyMillis() : 0;
    }
}
