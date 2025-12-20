package miroshka.aether.common.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PortalSyncPacket(
        String serverName,
        List<PortalData> portals) implements Packet {

    public PortalSyncPacket {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(portals, "portals");
    }

    @Override
    public int packetId() {
        return PacketIds.PORTAL_SYNC;
    }

    @Override
    public void encode(ByteBuf buffer) {
        PacketHelper.writeString(buffer, serverName);
        PacketHelper.writeInt(buffer, portals.size());
        for (PortalData portal : portals) {
            portal.encode(buffer);
        }
    }

    @Override
    public Priority priority() {
        return Priority.NORMAL;
    }

    public static PortalSyncPacket decode(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        String serverName = PacketHelper.readString(buffer);
        int count = PacketHelper.readInt(buffer);
        List<PortalData> portals = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            portals.add(PortalData.decode(buffer));
        }
        return new PortalSyncPacket(serverName, portals);
    }

    public record PortalData(
            String id,
            String targetServer,
            PortalType type,
            BoundaryRegion boundary,
            BoxRegion region,
            boolean seamless) {

        public PortalData {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetServer, "targetServer");
            Objects.requireNonNull(type, "type");
        }

        public void encode(ByteBuf buffer) {
            PacketHelper.writeString(buffer, id);
            PacketHelper.writeString(buffer, targetServer);
            PacketHelper.writeInt(buffer, type.ordinal());
            buffer.writeBoolean(seamless);

            boolean hasBoundary = boundary != null;
            buffer.writeBoolean(hasBoundary);
            if (hasBoundary) {
                boundary.encode(buffer);
            }

            boolean hasRegion = region != null;
            buffer.writeBoolean(hasRegion);
            if (hasRegion) {
                region.encode(buffer);
            }
        }

        public static PortalData decode(ByteBuf buffer) {
            String id = PacketHelper.readString(buffer);
            String targetServer = PacketHelper.readString(buffer);
            PortalType type = PortalType.values()[PacketHelper.readInt(buffer)];
            boolean seamless = buffer.readBoolean();

            BoundaryRegion boundary = null;
            if (buffer.readBoolean()) {
                boundary = BoundaryRegion.decode(buffer);
            }

            BoxRegion region = null;
            if (buffer.readBoolean()) {
                region = BoxRegion.decode(buffer);
            }

            return new PortalData(id, targetServer, type, boundary, region, seamless);
        }
    }

    public enum PortalType {
        BOUNDARY,
        REGION
    }

    public record BoundaryRegion(
            int minX,
            int maxX,
            int minZ,
            int maxZ) {

        public void encode(ByteBuf buffer) {
            PacketHelper.writeInt(buffer, minX);
            PacketHelper.writeInt(buffer, maxX);
            PacketHelper.writeInt(buffer, minZ);
            PacketHelper.writeInt(buffer, maxZ);
        }

        public static BoundaryRegion decode(ByteBuf buffer) {
            return new BoundaryRegion(
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer));
        }

        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    public record BoxRegion(
            String world,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {

        public BoxRegion {
            Objects.requireNonNull(world, "world");
        }

        public void encode(ByteBuf buffer) {
            PacketHelper.writeString(buffer, world);
            PacketHelper.writeInt(buffer, minX);
            PacketHelper.writeInt(buffer, minY);
            PacketHelper.writeInt(buffer, minZ);
            PacketHelper.writeInt(buffer, maxX);
            PacketHelper.writeInt(buffer, maxY);
            PacketHelper.writeInt(buffer, maxZ);
        }

        public static BoxRegion decode(ByteBuf buffer) {
            return new BoxRegion(
                    PacketHelper.readString(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer),
                    PacketHelper.readInt(buffer));
        }

        public boolean contains(String worldName, int x, int y, int z) {
            return world.equals(worldName)
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
