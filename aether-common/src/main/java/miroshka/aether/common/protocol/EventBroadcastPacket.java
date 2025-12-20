package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record EventBroadcastPacket(
        String eventType,
        String sourceServer,
        UUID playerUuid,
        String playerName,
        List<String> playerGroups,
        Map<String, String> eventData,
        long timestamp) implements Packet {

    public EventBroadcastPacket {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(sourceServer, "sourceServer");
        Objects.requireNonNull(playerGroups, "playerGroups");
        Objects.requireNonNull(eventData, "eventData");
    }

    public EventBroadcastPacket(String eventType, String sourceServer, UUID playerUuid,
            String playerName, List<String> playerGroups,
            Map<String, String> eventData) {
        this(eventType, sourceServer, playerUuid, playerName, playerGroups, eventData,
                System.currentTimeMillis());
    }

    @Override
    public int packetId() {
        return PacketIds.EVENT_BROADCAST;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeString(buffer, eventType);
        PacketHelper.writeString(buffer, sourceServer);

        boolean hasPlayer = playerUuid != null;
        buffer.writeBoolean(hasPlayer);
        if (hasPlayer) {
            PacketHelper.writeUUID(buffer, playerUuid);
            PacketHelper.writeString(buffer, playerName != null ? playerName : "");
        }

        PacketHelper.writeInt(buffer, playerGroups.size());
        for (String group : playerGroups) {
            PacketHelper.writeString(buffer, group);
        }

        PacketHelper.writePropertyMap(buffer, eventData);
        PacketHelper.writeLong(buffer, timestamp);
    }

    @Override
    public Priority priority() {
        return Priority.NORMAL;
    }

    public static EventBroadcastPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        String eventType = PacketHelper.readString(buffer);
        String sourceServer = PacketHelper.readString(buffer);

        UUID playerUuid = null;
        String playerName = null;
        if (buffer.readBoolean()) {
            playerUuid = PacketHelper.readUUID(buffer);
            playerName = PacketHelper.readString(buffer);
        }

        int groupCount = PacketHelper.readInt(buffer);
        List<String> playerGroups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            playerGroups.add(PacketHelper.readString(buffer));
        }

        Map<String, String> eventData = PacketHelper.readPropertyMap(buffer);
        long timestamp = PacketHelper.readLong(buffer);

        return new EventBroadcastPacket(eventType, sourceServer, playerUuid,
                playerName, playerGroups, eventData, timestamp);
    }

    public boolean hasGroup(String group) {
        return playerGroups.contains(group);
    }

    public boolean hasAnyGroup(List<String> groups) {
        for (String group : groups) {
            if (playerGroups.contains(group)) {
                return true;
            }
        }
        return false;
    }
}
