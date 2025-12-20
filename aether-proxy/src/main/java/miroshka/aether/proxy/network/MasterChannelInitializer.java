package miroshka.aether.proxy.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import miroshka.aether.common.codec.PacketDecoder;
import miroshka.aether.common.codec.PacketEncoder;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.config.ProxyConfig;
import miroshka.aether.proxy.event.EventRouter;
import miroshka.aether.proxy.security.SecretKeyValidator;
import miroshka.aether.proxy.transfer.SeamlessTransferHandler;

import java.util.Objects;

public final class MasterChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NodeRegistry nodeRegistry;
    private final ProxyConfig config;
    private final SecretKeyValidator secretKeyValidator;
    private final StateBroadcaster stateBroadcaster;
    private final EventRouter eventRouter;
    private final SeamlessTransferHandler transferHandler;

    public MasterChannelInitializer(
            NodeRegistry nodeRegistry,
            ProxyConfig config,
            SecretKeyValidator secretKeyValidator,
            StateBroadcaster stateBroadcaster,
            EventRouter eventRouter,
            SeamlessTransferHandler transferHandler) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.config = Objects.requireNonNull(config, "config");
        this.secretKeyValidator = Objects.requireNonNull(secretKeyValidator, "secretKeyValidator");
        this.stateBroadcaster = Objects.requireNonNull(stateBroadcaster, "stateBroadcaster");
        this.eventRouter = eventRouter;
        this.transferHandler = transferHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("decoder", new PacketDecoder());
        pipeline.addLast("encoder", new PacketEncoder());
        pipeline.addLast("handler", new MasterPacketHandler(
                nodeRegistry,
                config,
                secretKeyValidator,
                stateBroadcaster,
                eventRouter,
                transferHandler));
    }
}
