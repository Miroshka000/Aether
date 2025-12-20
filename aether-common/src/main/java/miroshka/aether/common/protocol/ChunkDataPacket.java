package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.UUID;

public record ChunkDataPacket(
        UUID requestId,
        String sourceServer,
        String targetServer,
        ChunkAction action,
        int chunkX,
        int chunkZ,
        byte[] chunkData,
        long timestamp) implements Packet {

    public ChunkDataPacket {
        Objects.requireNonNull(sourceServer, "sourceServer");
        Objects.requireNonNull(targetServer, "targetServer");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(chunkData, "chunkData");
    }

    public ChunkDataPacket(UUID requestId, String sourceServer, String targetServer,
            ChunkAction action, int chunkX, int chunkZ, byte[] chunkData) {
        this(requestId, sourceServer, targetServer, action, chunkX, chunkZ, chunkData, System.currentTimeMillis());
    }

    @Override
    public int packetId() {
        return PacketIds.CHUNK_DATA;
    }

    @Override
    public void encode(ByteBuf buffer) {
        if (requestId != null) {
            PacketHelper.writeBoolean(buffer, true);
            PacketHelper.writeUUID(buffer, requestId);
        } else {
            PacketHelper.writeBoolean(buffer, false);
        }
        PacketHelper.writeString(buffer, sourceServer);
        PacketHelper.writeString(buffer, targetServer);
        PacketHelper.writeVarInt(buffer, action.ordinal());
        PacketHelper.writeInt(buffer, chunkX);
        PacketHelper.writeInt(buffer, chunkZ);
        PacketHelper.writeLong(buffer, timestamp);
        PacketHelper.writeInt(buffer, chunkData.length);
        buffer.writeBytes(chunkData);
    }

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    public static ChunkDataPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        UUID requestId = null;
        if (PacketHelper.readBoolean(buffer)) {
            requestId = PacketHelper.readUUID(buffer);
        }
        String sourceServer = PacketHelper.readString(buffer);
        String targetServer = PacketHelper.readString(buffer);
        ChunkAction action = ChunkAction.values()[PacketHelper.readVarInt(buffer)];
        int chunkX = PacketHelper.readInt(buffer);
        int chunkZ = PacketHelper.readInt(buffer);
        long timestamp = PacketHelper.readLong(buffer);
        int length = PacketHelper.readInt(buffer);
        byte[] chunkData = new byte[length];
        buffer.readBytes(chunkData);
        return new ChunkDataPacket(requestId, sourceServer, targetServer, action, chunkX, chunkZ, chunkData, timestamp);
    }

    public long chunkKey() {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public enum ChunkAction {
        REQUEST,
        RESPONSE,
        PUSH
    }
}
