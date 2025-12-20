package miroshka.aether.proxy.event;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import miroshka.aether.common.protocol.EventBroadcastPacket;
import miroshka.aether.proxy.NodeSession;
import miroshka.aether.proxy.NodeRegistry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EventRouter {

    private final ProxyServer proxyServer;
    private final NodeRegistry nodeRegistry;
    private final Map<String, Set<String>> subscriptions;

    public EventRouter(ProxyServer proxyServer, NodeRegistry nodeRegistry) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.subscriptions = new ConcurrentHashMap<>();
    }

    public void registerSubscription(String eventType, String serverName) {
        subscriptions.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet())
                .add(serverName);
    }

    public void unregisterSubscription(String eventType, String serverName) {
        Set<String> servers = subscriptions.get(eventType);
        if (servers != null) {
            servers.remove(serverName);
        }
    }

    public void routeEvent(EventBroadcastPacket packet) {
        String eventType = packet.eventType();
        Set<String> targetServers = subscriptions.get(eventType);

        if (targetServers == null || targetServers.isEmpty()) {
            broadcastToAll(packet);
            return;
        }

        for (String serverName : targetServers) {
            if (!serverName.equals(packet.sourceServer())) {
                sendToServer(serverName, packet);
            }
        }
    }

    public void broadcastToAll(EventBroadcastPacket packet) {
        for (NodeSession session : nodeRegistry.getAllSessions()) {
            if (!session.nodeId().equals(packet.sourceServer()) && session.isActive()) {
                session.channel().writeAndFlush(packet);
            }
        }
    }

    public void broadcastToPlayers(EventBroadcastPacket packet, List<String> requiredGroups) {
        for (ProxiedPlayer player : proxyServer.getPlayers().values()) {
            if (shouldReceiveEvent(packet, requiredGroups)) {
                sendEventToPlayer(player, packet);
            }
        }
    }

    private boolean shouldReceiveEvent(EventBroadcastPacket packet, List<String> requiredGroups) {
        if (requiredGroups == null || requiredGroups.isEmpty()) {
            return true;
        }
        return packet.hasAnyGroup(requiredGroups);
    }

    private void sendEventToPlayer(ProxiedPlayer player, EventBroadcastPacket packet) {
        String message = formatEventMessage(packet);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    private void sendToServer(String serverName, EventBroadcastPacket packet) {
        nodeRegistry.getByNodeId(serverName).ifPresent(session -> {
            if (session.isActive()) {
                session.channel().writeAndFlush(packet);
            }
        });
    }

    private String formatEventMessage(EventBroadcastPacket packet) {
        String format = packet.eventData().get("format");
        if (format == null) {
            return null;
        }

        String message = format
                .replace("{player}", packet.playerName() != null ? packet.playerName() : "Unknown")
                .replace("{server}", packet.sourceServer())
                .replace("{event}", packet.eventType());

        for (Map.Entry<String, String> entry : packet.eventData().entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }
}
