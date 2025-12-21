package miroshka.aether.proxy.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.event.EventRouter;
import miroshka.aether.proxy.security.SecretKeyValidator;
import miroshka.aether.proxy.transfer.SeamlessTransferHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Getter
public final class MasterNetworkServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterNetworkServer.class);

    private final ProxyConfig config;
    private final NodeRegistry nodeRegistry;
    private final SecretKeyValidator secretKeyValidator;
    private final StateBroadcaster stateBroadcaster;
    private final EventRouter eventRouter;
    private final SeamlessTransferHandler transferHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;

    public MasterNetworkServer(
            ProxyConfig config,
            NodeRegistry nodeRegistry,
            EventRouter eventRouter,
            SeamlessTransferHandler transferHandler) {
        this.config = Objects.requireNonNull(config, "config");
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.secretKeyValidator = new SecretKeyValidator(config.secretKeys());
        this.stateBroadcaster = new StateBroadcaster(nodeRegistry, config.broadcastIntervalMillis());
        this.eventRouter = eventRouter;
        this.transferHandler = transferHandler;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new MasterChannelInitializer(
                            nodeRegistry,
                            config,
                            secretKeyValidator,
                            stateBroadcaster,
                            eventRouter,
                            transferHandler));

            serverChannel = bootstrap.bind(config.network().port()).sync();
            stateBroadcaster.start();

            LOGGER.info("Aether Master started on port {}", config.network().port());
        } catch (Exception e) {
            LOGGER.error("Failed to start Aether Master", e);
            shutdown();
            throw e;
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down Aether Master...");

        stateBroadcaster.stop();

        if (serverChannel != null) {
            serverChannel.channel().close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        LOGGER.info("Aether Master shutdown complete");
    }
}
