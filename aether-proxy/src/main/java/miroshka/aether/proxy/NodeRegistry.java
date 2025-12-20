package miroshka.aether.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NodeRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRegistry.class);

    private final Map<String, NodeSession> sessionsByNodeId;

    public NodeRegistry() {
        this.sessionsByNodeId = new ConcurrentHashMap<>();
    }

    public void register(NodeSession session) {
        Objects.requireNonNull(session, "session");
        sessionsByNodeId.put(session.nodeId(), session);
        LOGGER.info("Node registered: {} from {}", session.nodeId(), session.remoteAddress());
    }

    public void unregister(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        NodeSession session = sessionsByNodeId.remove(nodeId);
        if (session != null) {
            LOGGER.info("Node unregistered: {}", nodeId);
        }
    }

    public Optional<NodeSession> getByNodeId(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        return Optional.ofNullable(sessionsByNodeId.get(nodeId));
    }

    public Collection<NodeSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessionsByNodeId.values());
    }

    public int getNodeCount() {
        return sessionsByNodeId.size();
    }

    public int getTotalOnlinePlayers() {
        return sessionsByNodeId.values().stream()
                .mapToInt(session -> session.state().getOnlinePlayers())
                .sum();
    }

    public boolean isRegistered(String nodeId) {
        return sessionsByNodeId.containsKey(nodeId);
    }

    public boolean hasAnyDirtyState() {
        return sessionsByNodeId.values().stream()
                .anyMatch(session -> session.state().isDirty());
    }

    public void clearAllDirtyFlags() {
        sessionsByNodeId.values().forEach(session -> session.state().clearDirty());
    }
}
