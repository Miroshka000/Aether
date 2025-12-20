package miroshka.aether.proxy;

import lombok.Getter;
import miroshka.aether.common.protocol.NodeSnapshotPacket;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public record NodeSession(
        String nodeId,
        String remoteAddress,
        io.netty.channel.Channel channel,
        long connectedAt,
        NodeState state) {

    public NodeSession {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(remoteAddress, "remoteAddress");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(state, "state");
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public static NodeSession create(String nodeId, String remoteAddress, io.netty.channel.Channel channel) {
        return new NodeSession(
                nodeId,
                remoteAddress,
                channel,
                System.currentTimeMillis(),
                new NodeState());
    }

    @Getter
    public static final class NodeState {
        private volatile int onlinePlayers;
        private volatile int maxPlayers;
        private volatile double tps;
        private volatile long lastUpdateTimestamp;
        private volatile boolean dirty;
        private final Map<String, String> extraData;

        public NodeState() {
            this.extraData = new ConcurrentHashMap<>();
            this.tps = 20.0;
            this.lastUpdateTimestamp = System.currentTimeMillis();
        }

        public void updateFromSnapshot(NodeSnapshotPacket snapshot) {
            Objects.requireNonNull(snapshot, "snapshot");
            this.onlinePlayers = snapshot.onlinePlayers();
            this.maxPlayers = snapshot.maxPlayers();
            this.tps = snapshot.tps();
            this.lastUpdateTimestamp = snapshot.captureTimestamp();
            this.extraData.clear();
            this.extraData.putAll(snapshot.extraData());
            this.dirty = true;
        }

        public void updateHeartbeat() {
            this.lastUpdateTimestamp = System.currentTimeMillis();
        }

        public void clearDirty() {
            this.dirty = false;
        }

        public Map<String, String> getAllExtraData() {
            return Map.copyOf(extraData);
        }
    }
}
