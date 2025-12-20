package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NetworkStatePacket(
        int globalOnline,
        int serverCount,
        long stateVersion,
        int ttlSeconds,
        Map<String, String> globalProperties,
        Map<String, String> routingHints,
        List<ServerInfo> servers) implements Packet {

    public NetworkStatePacket {
        Objects.requireNonNull(globalProperties, "globalProperties");
        Objects.requireNonNull(routingHints, "routingHints");
        Objects.requireNonNull(servers, "servers");
    }

    public NetworkStatePacket(
            int globalOnline,
            int serverCount,
            long stateVersion,
            int ttlSeconds,
            Map<String, String> globalProperties,
            Map<String, String> routingHints) {
        this(globalOnline, serverCount, stateVersion, ttlSeconds, globalProperties, routingHints, List.of());
    }

    @Override
    public int packetId() {
        return PacketIds.NETWORK_STATE;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeInt(buffer, globalOnline);
        PacketHelper.writeInt(buffer, serverCount);
        PacketHelper.writeLong(buffer, stateVersion);
        PacketHelper.writeInt(buffer, ttlSeconds);
        PacketHelper.writePropertyMap(buffer, globalProperties);
        PacketHelper.writePropertyMap(buffer, routingHints);

        PacketHelper.writeInt(buffer, servers.size());
        for (ServerInfo server : servers) {
            PacketHelper.writeString(buffer, server.name());
            PacketHelper.writeInt(buffer, server.onlinePlayers());
            PacketHelper.writeInt(buffer, server.maxPlayers());
            PacketHelper.writeDouble(buffer, server.tps());
            PacketHelper.writeLong(buffer, server.lastUpdateTimestamp());
            PacketHelper.writePropertyMap(buffer, server.extraData());
        }
    }

    @Override
    public Priority priority() {
        return Priority.NORMAL;
    }

    public static NetworkStatePacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int globalOnline = PacketHelper.readInt(buffer);
        int serverCount = PacketHelper.readInt(buffer);
        long stateVersion = PacketHelper.readLong(buffer);
        int ttlSeconds = PacketHelper.readInt(buffer);
        Map<String, String> globalProperties = PacketHelper.readPropertyMap(buffer);
        Map<String, String> routingHints = PacketHelper.readPropertyMap(buffer);

        List<ServerInfo> servers = new ArrayList<>();
        if (buffer.isReadable()) {
            int serversCount = PacketHelper.readInt(buffer);
            for (int i = 0; i < serversCount; i++) {
                String name = PacketHelper.readString(buffer);
                int onlinePlayers = PacketHelper.readInt(buffer);
                int maxPlayers = PacketHelper.readInt(buffer);
                double tps = PacketHelper.readDouble(buffer);
                long lastUpdateTimestamp = PacketHelper.readLong(buffer);
                Map<String, String> extraData = PacketHelper.readPropertyMap(buffer);
                servers.add(new ServerInfo(name, onlinePlayers, maxPlayers, tps, lastUpdateTimestamp, extraData));
            }
        }

        return new NetworkStatePacket(globalOnline, serverCount, stateVersion, ttlSeconds,
                globalProperties, routingHints, servers);
    }
}
