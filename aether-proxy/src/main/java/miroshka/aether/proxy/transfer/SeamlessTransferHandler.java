package miroshka.aether.proxy.transfer;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import miroshka.aether.common.protocol.TransferRequestPacket;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SeamlessTransferHandler {

    private final ProxyServer proxyServer;

    public SeamlessTransferHandler(ProxyServer proxyServer) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
    }

    public CompletableFuture<TransferResult> transfer(UUID playerUuid, String targetServerName, boolean seamless) {
        ProxiedPlayer player = proxyServer.getPlayer(playerUuid);
        if (player == null) {
            return CompletableFuture.completedFuture(TransferResult.failure("Player not found"));
        }

        ServerInfo targetServer = proxyServer.getServerInfo(targetServerName);
        if (targetServer == null) {
            return CompletableFuture
                    .completedFuture(TransferResult.failure("Target server not found: " + targetServerName));
        }

        long startTime = System.currentTimeMillis();

        CompletableFuture<TransferResult> future = new CompletableFuture<>();

        if (seamless) {
            executeSeamlessTransfer(player, targetServer, startTime, future);
        } else {
            executeStandardTransfer(player, targetServer, startTime, future);
        }

        return future;
    }

    public void handleTransferRequest(TransferRequestPacket packet) {
        transfer(packet.playerUuid(), packet.targetServer(), packet.seamless());
    }

    private void executeSeamlessTransfer(ProxiedPlayer player, ServerInfo targetServer,
            long startTime, CompletableFuture<TransferResult> future) {
        player.connect(targetServer);

        long transferTime = System.currentTimeMillis() - startTime;
        future.complete(TransferResult.success(transferTime, true));
    }

    private void executeStandardTransfer(ProxiedPlayer player, ServerInfo targetServer,
            long startTime, CompletableFuture<TransferResult> future) {
        player.connect(targetServer);

        long transferTime = System.currentTimeMillis() - startTime;
        future.complete(TransferResult.success(transferTime, false));
    }

    public record TransferResult(boolean success, String message, long transferTimeMs, boolean seamless) {
        public static TransferResult success(long transferTimeMs, boolean seamless) {
            return new TransferResult(true, "Transfer successful", transferTimeMs, seamless);
        }

        public static TransferResult failure(String reason) {
            return new TransferResult(false, reason, 0, false);
        }
    }
}
