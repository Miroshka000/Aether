package miroshka.aether.server.event;

import miroshka.aether.api.event.AetherEventBridge;
import miroshka.aether.common.protocol.EventBroadcastPacket;
import miroshka.aether.server.network.NodeNetworkClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class EventBridgeService implements AetherEventBridge {

    private final NodeNetworkClient networkClient;
    private final String serverName;
    private final Map<String, List<SubscriptionImpl>> subscriptions;
    private final AtomicLong subscriptionIdCounter;

    public EventBridgeService(NodeNetworkClient networkClient, String serverName) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.subscriptions = new ConcurrentHashMap<>();
        this.subscriptionIdCounter = new AtomicLong(0);
    }

    @Override
    public void publish(String eventType, UUID playerUuid, String playerName,
            List<String> playerGroups, Map<String, String> eventData) {
        Objects.requireNonNull(eventType, "eventType");
        List<String> groups = playerGroups != null ? playerGroups : List.of();
        Map<String, String> data = eventData != null ? eventData : Map.of();

        EventBroadcastPacket packet = new EventBroadcastPacket(
                eventType, serverName, playerUuid, playerName, groups, data);
        networkClient.sendPacket(packet);
    }

    @Override
    public void publish(String eventType, Map<String, String> eventData) {
        publish(eventType, null, null, List.of(), eventData);
    }

    @Override
    public Subscription subscribe(String eventType, Consumer<NetworkEvent> handler) {
        return subscribe(eventType, EventFilter.all(), handler);
    }

    @Override
    public Subscription subscribe(String eventType, EventFilter filter, Consumer<NetworkEvent> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(handler, "handler");

        String id = String.valueOf(subscriptionIdCounter.incrementAndGet());
        SubscriptionImpl subscription = new SubscriptionImpl(id, eventType, filter, handler);

        subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(subscription);

        return subscription;
    }

    @Override
    public Subscription subscribeToServer(String serverName, String eventType, Consumer<NetworkEvent> handler) {
        Objects.requireNonNull(serverName, "serverName");
        return subscribe(eventType, EventFilter.bySource(serverName), handler);
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        if (subscription instanceof SubscriptionImpl impl) {
            List<SubscriptionImpl> subs = subscriptions.get(impl.eventType());
            if (subs != null) {
                subs.remove(impl);
            }
            impl.cancel();
        }
    }

    public void handleIncomingEvent(EventBroadcastPacket packet) {
        NetworkEvent event = new NetworkEvent(
                packet.eventType(),
                packet.sourceServer(),
                packet.playerUuid(),
                packet.playerName(),
                packet.playerGroups(),
                packet.eventData(),
                packet.timestamp());

        List<SubscriptionImpl> subs = subscriptions.get(packet.eventType());
        if (subs != null) {
            for (SubscriptionImpl sub : subs) {
                if (sub.isActive() && sub.filter.test(event)) {
                    try {
                        sub.handler.accept(event);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        List<SubscriptionImpl> wildcardSubs = subscriptions.get("*");
        if (wildcardSubs != null) {
            for (SubscriptionImpl sub : wildcardSubs) {
                if (sub.isActive() && sub.filter.test(event)) {
                    try {
                        sub.handler.accept(event);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static final class SubscriptionImpl implements Subscription {
        private final String id;
        private final String eventType;
        private final EventFilter filter;
        private final Consumer<NetworkEvent> handler;
        private volatile boolean active = true;

        SubscriptionImpl(String id, String eventType, EventFilter filter, Consumer<NetworkEvent> handler) {
            this.id = id;
            this.eventType = eventType;
            this.filter = filter;
            this.handler = handler;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String eventType() {
            return eventType;
        }

        @Override
        public void cancel() {
            this.active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
