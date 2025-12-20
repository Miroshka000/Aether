package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PDCSyncPacket(
        UUID playerUuid,
        String playerName,
        SyncOperation operation,
        Map<String, byte[]> data,
        long version) implements Packet {

    public PDCSyncPacket {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(data, "data");
    }

    public static PDCSyncPacket fullSync(UUID playerUuid, String playerName, Map<String, byte[]> data) {
        return new PDCSyncPacket(playerUuid, playerName, SyncOperation.FULL_SYNC, data,
                System.currentTimeMillis());
    }

    public static PDCSyncPacket partialUpdate(UUID playerUuid, String playerName,
            Map<String, byte[]> data, long version) {
        return new PDCSyncPacket(playerUuid, playerName, SyncOperation.PARTIAL_UPDATE, data, version);
    }

    public static PDCSyncPacket delete(UUID playerUuid, String playerName, String key) {
        Map<String, byte[]> deleteData = new HashMap<>();
        deleteData.put(key, new byte[0]);
        return new PDCSyncPacket(playerUuid, playerName, SyncOperation.DELETE, deleteData,
                System.currentTimeMillis());
    }

    @Override
    public int packetId() {
        return PacketIds.PDC_SYNC;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeUUID(buffer, playerUuid);
        PacketHelper.writeString(buffer, playerName);
        PacketHelper.writeInt(buffer, operation.ordinal());
        PacketHelper.writeLong(buffer, version);

        PacketHelper.writeInt(buffer, data.size());
        for (Map.Entry<String, byte[]> entry : data.entrySet()) {
            PacketHelper.writeString(buffer, entry.getKey());
            PacketHelper.writeInt(buffer, entry.getValue().length);
            buffer.writeBytes(entry.getValue());
        }
    }

    @Override
    public Priority priority() {
        return Priority.NORMAL;
    }

    public static PDCSyncPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        UUID playerUuid = PacketHelper.readUUID(buffer);
        String playerName = PacketHelper.readString(buffer);
        SyncOperation operation = SyncOperation.values()[PacketHelper.readInt(buffer)];
        long version = PacketHelper.readLong(buffer);

        int dataCount = PacketHelper.readInt(buffer);
        Map<String, byte[]> data = new HashMap<>(dataCount);
        for (int i = 0; i < dataCount; i++) {
            String key = PacketHelper.readString(buffer);
            int length = PacketHelper.readInt(buffer);
            byte[] value = new byte[length];
            buffer.readBytes(value);
            data.put(key, value);
        }

        return new PDCSyncPacket(playerUuid, playerName, operation, data, version);
    }

    public enum SyncOperation {
        FULL_SYNC,
        PARTIAL_UPDATE,
        DELETE,
        REQUEST
    }
}
