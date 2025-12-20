package miroshka.aether.addons;

import java.util.List;
import java.util.Optional;

public interface AddonManager {

    void loadAddons();

    void unloadAddons();

    void enableAddon(String addonId);

    void disableAddon(String addonId);

    Optional<AetherAddon> getAddon(String addonId);

    List<AetherAddon> getLoadedAddons();

    List<AetherAddon> getEnabledAddons();

    boolean isLoaded(String addonId);

    boolean isEnabled(String addonId);
}
