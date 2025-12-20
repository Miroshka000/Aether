package miroshka.aether.addons;

import miroshka.aether.api.AetherAPI;

public interface AetherAddon {

    String getId();

    String getName();

    String getVersion();

    String getAuthor();

    String getDescription();

    void onLoad(AetherAPI api);

    void onEnable();

    void onDisable();

    default void onReload() {
        onDisable();
        onEnable();
    }

    AddonState getState();

    enum AddonState {
        UNLOADED,
        LOADED,
        ENABLED,
        DISABLED,
        ERROR
    }
}
