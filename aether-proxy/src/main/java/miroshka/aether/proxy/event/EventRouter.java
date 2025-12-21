package miroshka.aether.proxy.event;

import lombok.RequiredArgsConstructor;
import miroshka.aether.common.protocol.EventBroadcastPacket;
import miroshka.aether.proxy.NodeRegistry;
import miroshka.aether.proxy.NodeSession;

@RequiredArgsConstructor
public final class EventRouter {

    private final NodeRegistry nodeRegistry;

    public void routeEvent(EventBroadcastPacket packet) {
        broadcastToAll(packet);
    }

    private void broadcastToAll(EventBroadcastPacket packet) {
        for (NodeSession session : nodeRegistry.getAllSessions()) {
            if (!session.nodeId().equals(packet.sourceServer()) && session.isActive()) {
                session.channel().writeAndFlush(packet);
            }
        }
    }
}
