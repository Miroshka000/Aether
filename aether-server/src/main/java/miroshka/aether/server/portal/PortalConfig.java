package miroshka.aether.server.portal;

import java.util.List;
import java.util.Objects;

public record PortalConfig(
        boolean enabled,
        List<PortalEntry> portals) {

    public PortalConfig {
        Objects.requireNonNull(portals, "portals");
    }

    public static PortalConfig defaults() {
        return new PortalConfig(true, List.of());
    }

    public record PortalEntry(
            String id,
            String targetServer,
            String world,
            Position pos1,
            Position pos2,
            Position spawnPos,
            boolean seamless) {

        public PortalEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetServer, "targetServer");
            Objects.requireNonNull(world, "world");
            Objects.requireNonNull(pos1, "pos1");
            Objects.requireNonNull(pos2, "pos2");
        }

        public int minX() {
            return Math.min(pos1.x(), pos2.x());
        }

        public int maxX() {
            return Math.max(pos1.x(), pos2.x());
        }

        public int minY() {
            return Math.min(pos1.y(), pos2.y());
        }

        public int maxY() {
            return Math.max(pos1.y(), pos2.y());
        }

        public int minZ() {
            return Math.min(pos1.z(), pos2.z());
        }

        public int maxZ() {
            return Math.max(pos1.z(), pos2.z());
        }

        public int targetX() {
            return spawnPos != null ? spawnPos.x() : 0;
        }

        public int targetY() {
            return spawnPos != null ? spawnPos.y() : 64;
        }

        public int targetZ() {
            return spawnPos != null ? spawnPos.z() : 0;
        }
    }

    public record Position(int x, int y, int z) {
    }
}
