package miroshka.aether.common.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AetherEventBus {

    private static final AetherEventBus INSTANCE = new AetherEventBus();

    private final Map<Class<? extends AetherEvent>, List<EventSubscription<?>>> subscriptions;
    private final ExecutorService asyncExecutor;

    private AetherEventBus() {
        this.subscriptions = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static AetherEventBus instance() {
        return INSTANCE;
    }

    public <E extends AetherEvent> void subscribe(Class<E> eventType, Consumer<E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        subscribe(eventType, handler, false, 0);
    }

    public <E extends AetherEvent> void subscribe(Class<E> eventType, Consumer<E> handler, boolean async) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        subscribe(eventType, handler, async, 0);
    }

    public <E extends AetherEvent> void subscribe(Class<E> eventType, Consumer<E> handler, boolean async,
            int priority) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        EventSubscription<E> subscription = new EventSubscription<>(handler, async, priority);
        subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(subscription);
        sortSubscriptions(eventType);
    }

    public <E extends AetherEvent> void unsubscribe(Class<E> eventType, Consumer<E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        List<EventSubscription<?>> subs = subscriptions.get(eventType);
        if (subs != null) {
            subs.removeIf(sub -> sub.handler().equals(handler));
        }
    }

    @SuppressWarnings("unchecked")
    public <E extends AetherEvent> void publish(E event) {
        Objects.requireNonNull(event, "event");
        List<EventSubscription<?>> subs = subscriptions.get(event.getClass());
        if (subs == null || subs.isEmpty()) {
            return;
        }
        for (EventSubscription<?> subscription : subs) {
            EventSubscription<E> typed = (EventSubscription<E>) subscription;
            if (typed.async()) {
                asyncExecutor.submit(() -> typed.handler().accept(event));
            } else {
                typed.handler().accept(event);
            }
        }
    }

    private void sortSubscriptions(Class<? extends AetherEvent> eventType) {
        List<EventSubscription<?>> subs = subscriptions.get(eventType);
        if (subs != null && subs.size() > 1) {
            ((CopyOnWriteArrayList<EventSubscription<?>>) subs)
                    .sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        }
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }

    private record EventSubscription<E extends AetherEvent>(
            Consumer<E> handler,
            boolean async,
            int priority) {
        EventSubscription {
            Objects.requireNonNull(handler, "handler");
        }
    }
}
