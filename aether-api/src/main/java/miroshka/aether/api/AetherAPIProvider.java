package miroshka.aether.api;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public final class AetherAPIProvider {

    private static final AtomicReference<AetherAPI> INSTANCE = new AtomicReference<>();

    private AetherAPIProvider() {
    }

    public static Optional<AetherAPI> getInstance() {
        AetherAPI api = INSTANCE.get();
        if (api != null) {
            return Optional.of(api);
        }
        return loadFromServiceLoader();
    }

    public static void register(AetherAPI api) {
        if (!INSTANCE.compareAndSet(null, api)) {
            throw new IllegalStateException("AetherAPI already registered");
        }
    }

    public static void unregister() {
        INSTANCE.set(null);
    }

    private static Optional<AetherAPI> loadFromServiceLoader() {
        ServiceLoader<AetherAPI> loader = ServiceLoader.load(AetherAPI.class);
        return loader.findFirst().map(api -> {
            INSTANCE.compareAndSet(null, api);
            return INSTANCE.get();
        });
    }
}
