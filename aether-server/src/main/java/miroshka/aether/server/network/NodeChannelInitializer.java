package miroshka.aether.server.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import miroshka.aether.common.codec.PacketDecoder;
import miroshka.aether.common.codec.PacketEncoder;
import miroshka.aether.server.config.NodeConfig;
import miroshka.aether.server.state.NetworkStateCache;

import java.util.Objects;
import java.util.function.Consumer;

public final class NodeChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NodeConfig config;
    private final NetworkStateCache stateCache;
    private final Consumer<NodePacketHandler> handlerConsumer;

    public NodeChannelInitializer(
            NodeConfig config,
            NetworkStateCache stateCache,
            Consumer<NodePacketHandler> handlerConsumer) {
        this.config = Objects.requireNonNull(config, "config");
        this.stateCache = Objects.requireNonNull(stateCache, "stateCache");
        this.handlerConsumer = Objects.requireNonNull(handlerConsumer, "handlerConsumer");
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        NodePacketHandler handler = new NodePacketHandler(config, stateCache);

        pipeline.addLast("decoder", new PacketDecoder());
        pipeline.addLast("encoder", new PacketEncoder());
        pipeline.addLast("handler", handler);

        handlerConsumer.accept(handler);
    }
}
