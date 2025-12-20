package miroshka.aether.api.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface AetherEventBridge {

    void publish(String eventType, UUID playerUuid, String playerName,
            List<String> playerGroups, Map<String, String> eventData);

    void publish(String eventType, Map<String, String> eventData);

    Subscription subscribe(String eventType, Consumer<NetworkEvent> handler);

    Subscription subscribe(String eventType, EventFilter filter, Consumer<NetworkEvent> handler);

    Subscription subscribeToServer(String serverName, String eventType, Consumer<NetworkEvent> handler);

    void unsubscribe(Subscription subscription);

    record NetworkEvent(
            String eventType,
            String sourceServer,
            UUID playerUuid,
            String playerName,
            List<String> playerGroups,
            Map<String, String> eventData,
            long timestamp) {

        public boolean hasGroup(String group) {
            return playerGroups != null && playerGroups.contains(group);
        }

        public boolean hasAnyGroup(List<String> groups) {
            if (playerGroups == null || groups == null) {
                return false;
            }
            for (String group : groups) {
                if (playerGroups.contains(group)) {
                    return true;
                }
            }
            return false;
        }

        public String getData(String key) {
            return eventData != null ? eventData.get(key) : null;
        }

        public String getData(String key, String defaultValue) {
            if (eventData == null) {
                return defaultValue;
            }
            return eventData.getOrDefault(key, defaultValue);
        }
    }

    interface Subscription {
        String id();

        String eventType();

        void cancel();

        boolean isActive();
    }

    interface EventFilter extends Predicate<NetworkEvent> {

        static EventFilter byGroups(List<String> groups) {
            return event -> event.hasAnyGroup(groups);
        }

        static EventFilter bySource(String serverName) {
            return event -> serverName.equals(event.sourceServer());
        }

        static EventFilter hasPlayer() {
            return event -> event.playerUuid() != null;
        }

        static EventFilter all() {
            return event -> true;
        }

        default EventFilter and(EventFilter other) {
            return event -> this.test(event) && other.test(event);
        }

        default EventFilter or(EventFilter other) {
            return event -> this.test(event) || other.test(event);
        }
    }
}
