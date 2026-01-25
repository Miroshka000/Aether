package miroshka.aether.server.portal;

import miroshka.aether.api.portal.PortalManager;
import miroshka.aether.common.protocol.TransferRequestPacket;
import miroshka.aether.server.network.NodeNetworkClient;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.player.PlayerMoveEvent;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalManagerService implements PortalManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalManagerService.class);

    private final NodeNetworkClient networkClient;
    private final String serverName;
    private final Map<String, Portal> portals;
    private final Map<UUID, Long> lastTransferAttempt;
    private PortalConfig portalConfig;

    public PortalManagerService(Plugin plugin, NodeNetworkClient networkClient, String serverName) {
        Objects.requireNonNull(plugin, "plugin"); 
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.portals = new ConcurrentHashMap<>();
        this.lastTransferAttempt = new ConcurrentHashMap<>();
    }

    public void loadFromConfig(Path dataFolder) {
        portalConfig = PortalConfigLoader.load(dataFolder);

        if (!portalConfig.enabled()) {
            LOGGER.info("Portal system is disabled in config");
            return;
        }

        portals.clear();
        for (PortalConfig.PortalDefinition def : portalConfig.portals()) {
            if (!def.enabled()) {
                continue;
            }
            try {
                Portal portal = createPortalFromConfig(def);
                portals.put(portal.id(), portal);
                LOGGER.info("Loaded portal '{}' -> {} (Type: {})",
                        def.id(), def.targetServer(),
                        portal.type());
            } catch (Exception e) {
                LOGGER.error("Failed to load portal {}", def.id(), e);
            }
        }

        syncPortals();
        LOGGER.info("Loaded {} portals from config", portals.size());
    }

    private Portal createPortalFromConfig(PortalConfig.PortalDefinition def) {
        if (def instanceof PortalConfig.RegionPortalDefinition regionDef) {
            int x1 = regionDef.min().x();
            int x2 = regionDef.max().x();
            int y1 = regionDef.min().y();
            int y2 = regionDef.max().y();
            int z1 = regionDef.min().z();
            int z2 = regionDef.max().z();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            return Portal.region(
                    regionDef.id(),
                    serverName,
                    regionDef.targetServer(),
                    regionDef.world(),
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    0, 64, 0, // Default target spawn
                    regionDef.seamless());
        } else if (def instanceof PortalConfig.BoundaryPortalDefinition boundaryDef) {
            int minX = Integer.MIN_VALUE, maxX = Integer.MAX_VALUE;
            int minZ = Integer.MIN_VALUE, maxZ = Integer.MAX_VALUE;
            int t = boundaryDef.threshold();

            switch (boundaryDef.direction()) {
                case EAST -> minX = t;          // X > t
                case WEST -> maxX = t;          // X < t
                case SOUTH -> minZ = t;         // Z > t
                case NORTH -> maxZ = t;         // Z < t
                default -> { /* Ignore UP/DOWN for 2D boundary */ }
            }

            return Portal.boundary(
                    boundaryDef.id(),
                    serverName,
                    boundaryDef.targetServer(),
                    boundaryDef.world(),
                    minX, maxX, minZ, maxZ,
                    boundaryDef.seamless());
        }
        throw new IllegalArgumentException("Unknown portal definition type: " + def.getClass().getName());
    }

    public void registerEvents() {
        Server.getInstance().getEventBus().registerListener(this);
    }

    public void unregisterEvents() {
        Server.getInstance().getEventBus().unregisterListener(this);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (portalConfig == null || !portalConfig.enabled()) {
            return;
        }

        EntityPlayer player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        var to = event.getTo();
        String worldName = event.getFrom().dimension().getWorld().getWorldData().getDisplayName();

        findPortalAtLocation(worldName, to.x(), to.y(), to.z()).ifPresent(portal -> {
            long now = System.currentTimeMillis();
            long lastAttempt = lastTransferAttempt.getOrDefault(playerUuid, 0L);
            if (now - lastAttempt < 5000) {
                return;
            }
            lastTransferAttempt.put(playerUuid, now);

            LOGGER.info("Player {} entered portal '{}', transferring to {}",
                    player.getDisplayName(), portal.id(), portal.targetServer());

            transferPlayer(playerUuid, portal);
        });
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
                (int) portal.targetX(), (int) portal.targetY(), (int) portal.targetZ(), portal.seamless());
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
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);

        for (Portal portal : portals.values()) {
            if (!serverName.equals(portal.sourceServer())) {
                continue;
            }
            
            if (!portal.world().equals(world)) {
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
