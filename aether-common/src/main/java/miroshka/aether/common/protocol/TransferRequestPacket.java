package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.UUID;

public record TransferRequestPacket(
        UUID playerUuid,
        String playerName,
        String sourceServer,
        String targetServer,
        String portalId,
        double targetX,
        double targetY,
        double targetZ,
        boolean seamless) implements Packet {

    public TransferRequestPacket {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(sourceServer, "sourceServer");
        Objects.requireNonNull(targetServer, "targetServer");
        Objects.requireNonNull(portalId, "portalId");
    }

    @Override
    public int packetId() {
        return PacketIds.TRANSFER_REQUEST;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeUUID(buffer, playerUuid);
        PacketHelper.writeString(buffer, playerName);
        PacketHelper.writeString(buffer, sourceServer);
        PacketHelper.writeString(buffer, targetServer);
        PacketHelper.writeString(buffer, portalId);
        PacketHelper.writeDouble(buffer, targetX);
        PacketHelper.writeDouble(buffer, targetY);
        PacketHelper.writeDouble(buffer, targetZ);
        buffer.writeBoolean(seamless);
    }

    @Override
    public Priority priority() {
        return Priority.CRITICAL;
    }

    public static TransferRequestPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        UUID playerUuid = PacketHelper.readUUID(buffer);
        String playerName = PacketHelper.readString(buffer);
        String sourceServer = PacketHelper.readString(buffer);
        String targetServer = PacketHelper.readString(buffer);
        String portalId = PacketHelper.readString(buffer);
        double targetX = PacketHelper.readDouble(buffer);
        double targetY = PacketHelper.readDouble(buffer);
        double targetZ = PacketHelper.readDouble(buffer);
        boolean seamless = buffer.readBoolean();
        return new TransferRequestPacket(playerUuid, playerName, sourceServer, targetServer,
                portalId, targetX, targetY, targetZ, seamless);
    }
}
