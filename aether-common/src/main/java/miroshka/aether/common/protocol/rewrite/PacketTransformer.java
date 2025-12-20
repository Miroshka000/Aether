package miroshka.aether.common.protocol.rewrite;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public interface PacketTransformer {

    String id();

    boolean shouldTransform(TransformContext context);

    ByteBuf transform(ByteBuf packet, TransformContext context);

    default int priority() {
        return 0;
    }

    record TransformContext(
            UUID playerUuid,
            String playerName,
            String sourceServer,
            String targetServer,
            String direction,
            int packetId) {

        public static final String DIRECTION_UPSTREAM = "upstream";
        public static final String DIRECTION_DOWNSTREAM = "downstream";

        public boolean isUpstream() {
            return DIRECTION_UPSTREAM.equals(direction);
        }

        public boolean isDownstream() {
            return DIRECTION_DOWNSTREAM.equals(direction);
        }
    }
}
