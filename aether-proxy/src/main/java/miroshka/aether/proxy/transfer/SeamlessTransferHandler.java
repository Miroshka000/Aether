package miroshka.aether.proxy.transfer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.RequiredArgsConstructor;
import miroshka.aether.common.protocol.TransferRequestPacket;
import miroshka.aether.proxy.balancer.ProxyLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public final class SeamlessTransferHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeamlessTransferHandler.class);

    private final ProxyServer proxyServer;
    private final ProxyLoadBalancer loadBalancer;

    public TransferResult transfer(UUID playerUuid, String targetServerName, boolean seamless) {
        ProxiedPlayer player = proxyServer.getPlayer(playerUuid);
        if (player == null) {
            return TransferResult.failure("Player not found");
        }

        ServerInfo targetServer = proxyServer.getServerInfo(targetServerName);
        if (targetServer == null) {
            return TransferResult.failure("Target server not found: " + targetServerName);
        }

        return executeTransfer(player, targetServer, seamless);
    }

    public TransferResult transferWithLoadBalancing(UUID playerUuid, List<String> candidateServers, boolean seamless) {
        ProxiedPlayer player = proxyServer.getPlayer(playerUuid);
        if (player == null) {
            return TransferResult.failure("Player not found");
        }

        Optional<ServerInfo> selectedServer = loadBalancer.selectServer(playerUuid, candidateServers);
        if (selectedServer.isEmpty()) {
            return TransferResult.failure("No available servers");
        }

        ServerInfo targetServer = selectedServer.get();
        LOGGER.debug("Load balancer selected server {} for player {}", targetServer.getServerName(), player.getName());

        return executeTransfer(player, targetServer, seamless);
    }

    public void handleTransferRequest(TransferRequestPacket packet) {
        TransferResult result = transfer(packet.playerUuid(), packet.targetServer(), packet.seamless());
        if (!result.success()) {
            LOGGER.warn("Failed to handle transfer request for player {}: {}", packet.playerUuid(), result.message());
        }
    }

    private TransferResult executeTransfer(ProxiedPlayer player, ServerInfo targetServer, boolean seamless) {
        long startTime = System.currentTimeMillis();
        player.connect(targetServer);
        long transferTime = System.currentTimeMillis() - startTime;
        return TransferResult.success(transferTime, seamless, targetServer.getServerName());
    }

    public record TransferResult(boolean success, String message, long transferTimeMs, boolean seamless,
            String targetServer) {
        public static TransferResult success(long transferTimeMs, boolean seamless, String targetServer) {
            return new TransferResult(true, "Transfer successful", transferTimeMs, seamless, targetServer);
        }

        public static TransferResult failure(String reason) {
            return new TransferResult(false, reason, 0, false, null);
        }
    }
}
