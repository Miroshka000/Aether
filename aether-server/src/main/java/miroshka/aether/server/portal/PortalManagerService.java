package miroshka.aether.server.portal;

import miroshka.aether.api.portal.PortalManager;
import miroshka.aether.common.protocol.TransferRequestPacket;
import miroshka.aether.server.network.NodeNetworkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalManagerService implements PortalManager {

    private final NodeNetworkClient networkClient;
    private final String serverName;
    private final Map<String, Portal> portals;

    public PortalManagerService(NodeNetworkClient networkClient, String serverName) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.portals = new ConcurrentHashMap<>();
    }

    @Override
    public void registerPortal(Portal portal) {
        Objects.requireNonNull(portal, "portal");
        portals.put(portal.id(), portal);
        syncPortals();
    }

    @Override
    public void unregisterPortal(String portalId) {
        if (portals.remove(portalId) != null) {
            syncPortals();
        }
    }

    @Override
    public Optional<Portal> getPortal(String portalId) {
        return Optional.ofNullable(portals.get(portalId));
    }

    @Override
    public List<Portal> getPortals() {
        return new ArrayList<>(portals.values());
    }

    @Override
    public List<Portal> getPortalsByTarget(String targetServer) {
        Objects.requireNonNull(targetServer, "targetServer");
        return portals.values().stream()
                .filter(p -> targetServer.equals(p.targetServer()))
                .toList();
    }

    @Override
    public CompletableFuture<TransferResult> transferPlayer(UUID playerUuid, String targetServer, boolean seamless) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(targetServer, "targetServer");

        long startTime = System.currentTimeMillis();

        TransferRequestPacket packet = new TransferRequestPacket(
                playerUuid, "", serverName, targetServer, "direct", 0, 64, 0, seamless);
        networkClient.sendPacket(packet);

        long transferTime = System.currentTimeMillis() - startTime;
        return CompletableFuture.completedFuture(TransferResult.success(transferTime));
    }

    @Override
    public CompletableFuture<TransferResult> transferPlayer(UUID playerUuid, Portal portal) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(portal, "portal");

        long startTime = System.currentTimeMillis();

        TransferRequestPacket packet = new TransferRequestPacket(
                playerUuid, "", serverName, portal.targetServer(), portal.id(),
                portal.targetX(), portal.targetY(), portal.targetZ(), portal.seamless());
        networkClient.sendPacket(packet);

        long transferTime = System.currentTimeMillis() - startTime;
        return CompletableFuture.completedFuture(TransferResult.success(transferTime));
    }

    @Override
    public boolean isInPortalZone(String world, double x, double y, double z) {
        return findPortalAtLocation(world, x, y, z).isPresent();
    }

    @Override
    public Optional<Portal> findPortalAtLocation(String world, double x, double y, double z) {
        int ix = (int) x;
        int iy = (int) y;
        int iz = (int) z;

        for (Portal portal : portals.values()) {
            if (!serverName.equals(portal.sourceServer())) {
                continue;
            }

            if (portal.type() == PortalType.BOUNDARY && portal.boundary() != null) {
                if (portal.boundary().contains(ix, iz)) {
                    return Optional.of(portal);
                }
            } else if (portal.type() == PortalType.REGION && portal.region() != null) {
                if (portal.region().contains(world, ix, iy, iz)) {
                    return Optional.of(portal);
                }
            }
        }

        return Optional.empty();
    }

    public void syncPortals() {
        List<miroshka.aether.common.protocol.PortalSyncPacket.PortalData> portalData = portals.values().stream()
                .map(this::toPacketData)
                .toList();

        miroshka.aether.common.protocol.PortalSyncPacket packet = new miroshka.aether.common.protocol.PortalSyncPacket(
                serverName, portalData);
        networkClient.sendPacket(packet);
    }

    private miroshka.aether.common.protocol.PortalSyncPacket.PortalData toPacketData(Portal portal) {
        miroshka.aether.common.protocol.PortalSyncPacket.BoundaryRegion boundary = null;
        miroshka.aether.common.protocol.PortalSyncPacket.BoxRegion region = null;

        if (portal.boundary() != null) {
            boundary = new miroshka.aether.common.protocol.PortalSyncPacket.BoundaryRegion(
                    portal.boundary().minX(), portal.boundary().maxX(),
                    portal.boundary().minZ(), portal.boundary().maxZ());
        }

        if (portal.region() != null) {
            region = new miroshka.aether.common.protocol.PortalSyncPacket.BoxRegion(
                    portal.region().world(),
                    portal.region().minX(), portal.region().minY(), portal.region().minZ(),
                    portal.region().maxX(), portal.region().maxY(), portal.region().maxZ());
        }

        miroshka.aether.common.protocol.PortalSyncPacket.PortalType packetType = portal.type() == PortalType.BOUNDARY
                ? miroshka.aether.common.protocol.PortalSyncPacket.PortalType.BOUNDARY
                : miroshka.aether.common.protocol.PortalSyncPacket.PortalType.REGION;

        return new miroshka.aether.common.protocol.PortalSyncPacket.PortalData(
                portal.id(), portal.targetServer(), packetType, boundary, region, portal.seamless());
    }
}
