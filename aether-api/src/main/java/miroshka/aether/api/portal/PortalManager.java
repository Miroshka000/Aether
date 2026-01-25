package miroshka.aether.api.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PortalManager {

    void registerPortal(Portal portal);

    void unregisterPortal(String portalId);

    Optional<Portal> getPortal(String portalId);

    List<Portal> getPortals();

    List<Portal> getPortalsByTarget(String targetServer);

    CompletableFuture<TransferResult> transferPlayer(UUID playerUuid, String targetServer, boolean seamless);

    CompletableFuture<TransferResult> transferPlayer(UUID playerUuid, Portal portal);

    boolean isInPortalZone(String world, double x, double y, double z);

    Optional<Portal> findPortalAtLocation(String world, double x, double y, double z);

    record Portal(
            String id,
            String sourceServer,
            String targetServer,
            String world,
            PortalType type,
            BoundaryConfig boundary,
            RegionConfig region,
            boolean seamless,
            double targetX,
            double targetY,
            double targetZ) {

        public static Portal boundary(String id, String sourceServer, String targetServer, String world,
                int minX, int maxX, int minZ, int maxZ, boolean seamless) {
            return new Portal(id, sourceServer, targetServer, world, PortalType.BOUNDARY,
                    new BoundaryConfig(minX, maxX, minZ, maxZ), null, seamless, 0, 0, 0);
        }

        public static Portal region(String id, String sourceServer, String targetServer,
                String world, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ,
                double targetX, double targetY, double targetZ,
                boolean seamless) {
            return new Portal(id, sourceServer, targetServer, world, PortalType.REGION,
                    null, new RegionConfig(world, minX, minY, minZ, maxX, maxY, maxZ),
                    seamless, targetX, targetY, targetZ);
        }
    }

    enum PortalType {
        BOUNDARY,
        REGION
    }

    record BoundaryConfig(int minX, int maxX, int minZ, int maxZ) {
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    record RegionConfig(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(String worldName, int x, int y, int z) {
            return world.equals(worldName)
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }

    record TransferResult(boolean success, String message, long transferTime) {
        public static TransferResult success(long transferTime) {
            return new TransferResult(true, "Transfer successful", transferTime);
        }

        public static TransferResult failure(String reason) {
            return new TransferResult(false, reason, 0);
        }
    }
}
