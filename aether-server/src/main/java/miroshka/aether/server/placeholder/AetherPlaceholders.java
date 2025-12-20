package miroshka.aether.server.placeholder;

import miroshka.aether.api.AetherAPI;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;
import org.allaymc.papi.PlaceholderAPI;

public final class AetherPlaceholders {

    private final Plugin plugin;
    private final AetherAPI api;
    private boolean registered = false;

    public AetherPlaceholders(Plugin plugin, AetherAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void register() {
        if (!isPlaceholderAPIAvailable()) {
            plugin.getPluginLogger().info("PlaceholderAPI not found, skipping placeholder registration");
            return;
        }

        try {
            registerPlaceholders();
            registered = true;
            plugin.getPluginLogger().info("Aether placeholders registered successfully");
        } catch (Exception e) {
            plugin.getPluginLogger().warn("Failed to register Aether placeholders", e);
        }
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        registered = false;
    }

    private boolean isPlaceholderAPIAvailable() {
        return Server.getInstance().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private void registerPlaceholders() {
        var papi = PlaceholderAPI.getAPI();

        papi.registerPlaceholder(plugin, "aether_global_online",
                (player, params) -> String.valueOf(api.getGlobalOnline()));

        papi.registerPlaceholder(plugin, "aether_global_max",
                (player, params) -> String.valueOf(api.getGlobalMaxPlayers()));

        papi.registerPlaceholder(plugin, "aether_server_count",
                (player, params) -> String.valueOf(api.getServerCount()));

        papi.registerPlaceholder(plugin, "aether_servers", (player, params) -> String.join(", ", api.getServerNames()));

        papi.registerPlaceholder(plugin, "aether_connection_status",
                (player, params) -> api.getConnectionStatus().state().name().toLowerCase());

        papi.registerPlaceholder(plugin, "aether_connected",
                (player, params) -> api.getConnectionStatus().connected() ? "true" : "false");

        papi.registerPlaceholder(plugin, "aether_latency", (player, params) -> String.valueOf(api.getLatencyMillis()));

        papi.registerPlaceholder(plugin, "aether_latency_formatted", (player, params) -> api.getLatencyMillis() + "ms");

        papi.registerPlaceholder(plugin, "aether_state_version",
                (player, params) -> String.valueOf(api.getLastStateVersion()));

        papi.registerPlaceholder(plugin, "aether_state_stale",
                (player, params) -> api.isStateStale() ? "true" : "false");

        papi.registerPlaceholder(plugin, "aether_server_online", (player, params) -> {
            if (params == null || params.isEmpty())
                return "0";
            return String.valueOf(api.getServerOnline(params));
        });

        papi.registerPlaceholder(plugin, "aether_server_max", (player, params) -> {
            if (params == null || params.isEmpty())
                return "0";
            return String.valueOf(api.getServerMaxPlayers(params));
        });

        papi.registerPlaceholder(plugin, "aether_server_tps", (player, params) -> {
            if (params == null || params.isEmpty())
                return "0.0";
            return String.format("%.1f", api.getServerTps(params));
        });

        papi.registerPlaceholder(plugin, "aether_server_status", (player, params) -> {
            if (params == null || params.isEmpty())
                return "unknown";
            return api.isServerOnline(params) ? "online" : "offline";
        });

        papi.registerPlaceholder(plugin, "aether_server_load", (player, params) -> {
            if (params == null || params.isEmpty())
                return "0%";
            int online = api.getServerOnline(params);
            int max = api.getServerMaxPlayers(params);
            if (max == 0)
                return "0%";
            return String.format("%.0f%%", (double) online / max * 100);
        });

        for (String serverName : api.getServerNames()) {
            String prefix = "aether_" + serverName.toLowerCase().replace("-", "_");

            papi.registerPlaceholder(plugin, prefix + "_online",
                    (player, p) -> String.valueOf(api.getServerOnline(serverName)));

            papi.registerPlaceholder(plugin, prefix + "_max",
                    (player, p) -> String.valueOf(api.getServerMaxPlayers(serverName)));

            papi.registerPlaceholder(plugin, prefix + "_tps",
                    (player, p) -> String.format("%.1f", api.getServerTps(serverName)));

            papi.registerPlaceholder(plugin, prefix + "_status",
                    (player, p) -> api.isServerOnline(serverName) ? "online" : "offline");
        }
    }
}
