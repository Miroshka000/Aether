# Aether API Documentation

This document describes how to use Aether API in your AllayMC plugins to access cross-server data and state.

## Table of Contents
- [Getting Started](#getting-started)
- [Basic Usage](#basic-usage)
- [API Methods](#api-methods)
- [Events](#events)
- [PlaceholderAPI Integration](#placeholderapi-integration)
- [Examples](#examples)

---

## Getting Started

### Add Dependency

Add Aether API to your plugin's `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Miroshka000:Aether:aether-api:VERSION")
}
```

### Specify Dependency in Plugin

Add `aether-server` as a dependency in your plugin descriptor:

```kotlin
allay {
    plugin {
        depend = listOf("aether-server")
    }
}
```

---

## Basic Usage

### Getting the API Instance

```java
import miroshka.aether.api.AetherAPI;

public class MyPlugin extends Plugin {
    
    @Override
    public void onEnable() {
        AetherAPI.getInstance().ifPresent(api -> {
            int globalOnline = api.getGlobalOnline();
            getPluginLogger().info("Global online: " + globalOnline);
        });
    }
}
```

### Checking Connection Status

```java
AetherAPI.getInstance().ifPresent(api -> {
    ConnectionStatus status = api.getConnectionStatus();
    
    if (status.isConnected()) {
        getPluginLogger().info("Connected to Aether network!");
    } else {
        getPluginLogger().warn("Not connected. State: " + status.state());
    }
});
```

---

## API Methods

### Global Network Data

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getGlobalOnline()` | `int` | Total players across all servers |
| `getServerCount()` | `int` | Number of connected servers |
| `getServerNames()` | `List<String>` | List of all server names |
| `getConnectionStatus()` | `ConnectionStatus` | Current connection state |
| `getLatencyMillis()` | `int` | Ping to Master in milliseconds |
| `isStateStale()` | `boolean` | Whether cached state is outdated |
| `getLastStateVersion()` | `long` | Current state version number |

### Per-Server Data

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `getServerOnline(name)` | `String` | `int` | Online players on specific server |
| `getServerMaxPlayers(name)` | `String` | `int` | Max players on specific server |
| `getServerTps(name)` | `String` | `double` | TPS of specific server |
| `isServerOnline(name)` | `String` | `boolean` | Whether server is online |

### Properties and Routing

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `getGlobalProperty(key)` | `String` | `Optional<String>` | Get global property by key |
| `getRoutingHint(gameType)` | `String` | `Optional<String>` | Get least loaded server for game type |
| `sendCustomProperty(key, value)` | `String, String` | `void` | Send custom property to network |

---

## Events

Subscribe to network events to react to state changes:

```java
AetherAPI.getInstance().ifPresent(api -> {
    // Network state updates
    api.subscribe(NetworkStateChangedEvent.class, event -> {
        getPluginLogger().info("Network updated! Global online: " + event.globalOnline());
    });
    
    // Connection state changes
    api.subscribe(ConnectionStateChangedEvent.class, event -> {
        getPluginLogger().info("Connection: " + event.previousState() + " -> " + event.newState());
    });
    
    // Circuit breaker events (connection issues)
    api.subscribe(CircuitBreakerEvent.class, event -> {
        if (event.isOpen()) {
            getPluginLogger().warn("Connection unstable, circuit breaker opened");
        }
    });
});
```

### Available Events

| Event | Description |
|-------|-------------|
| `NetworkStateChangedEvent` | Fired when network state is updated from Master |
| `ConnectionStateChangedEvent` | Fired when connection state changes (connecting, connected, disconnected) |
| `CircuitBreakerEvent` | Fired when circuit breaker opens/closes due to connection issues |

---

## PlaceholderAPI Integration

If PlaceholderAPI is installed, Aether automatically registers placeholders.

### Global Placeholders

| Placeholder | Description |
|------------|-------------|
| `%aether_global_online%` | Total players on all servers |
| `%aether_server_count%` | Number of connected servers |
| `%aether_servers%` | Comma-separated list of servers |
| `%aether_connection_status%` | Connection status (connected/disconnected/connecting) |
| `%aether_connected%` | true/false - whether connected to Master |
| `%aether_latency%` | Ping to Master (number only) |
| `%aether_latency_formatted%` | Ping with "ms" suffix |
| `%aether_state_version%` | Current state version |
| `%aether_state_stale%` | true/false - whether state is outdated |

### Server-Specific Placeholders (with parameter)

| Placeholder | Description |
|------------|-------------|
| `%aether_server_online_<server>%` | Online players on `<server>` |
| `%aether_server_max_<server>%` | Max players on `<server>` |
| `%aether_server_tps_<server>%` | TPS of `<server>` |
| `%aether_server_status_<server>%` | online/offline status |
| `%aether_server_load_<server>%` | Load percentage (online/max %) |

### Auto-Generated Placeholders

For each connected server, placeholders are automatically created:

- `%aether_lobby_online%`, `%aether_lobby_max%`, `%aether_lobby_tps%`, `%aether_lobby_status%`
- `%aether_rpg_online%`, `%aether_rpg_max%`, `%aether_rpg_tps%`, `%aether_rpg_status%`
- etc.

---

## Examples

### Display Server Selector

```java
public void showServerSelector(Player player) {
    AetherAPI.getInstance().ifPresent(api -> {
        StringBuilder message = new StringBuilder("§6Available Servers:\n");
        
        for (String serverName : api.getServerNames()) {
            int online = api.getServerOnline(serverName);
            int max = api.getServerMaxPlayers(serverName);
            String status = api.isServerOnline(serverName) ? "§a●" : "§c●";
            
            message.append(String.format("%s §f%s §7(%d/%d)\n", 
                status, serverName, online, max));
        }
        
        player.sendMessage(message.toString());
    });
}
```

### Smart Server Routing

```java
public String findBestServer(String gameType) {
    return AetherAPI.getInstance()
        .flatMap(api -> api.getRoutingHint(gameType))
        .orElse("lobby");
}
```

### Connection Status Display

```java
public String getNetworkStatus() {
    return AetherAPI.getInstance().map(api -> {
        ConnectionStatus status = api.getConnectionStatus();
        return String.format("Status: %s | Servers: %d | Players: %d | Ping: %dms",
            status.state().name(),
            api.getServerCount(),
            api.getGlobalOnline(),
            api.getLatencyMillis());
    }).orElse("Aether not available");
}
```

### React to Player Changes

```java
AetherAPI.getInstance().ifPresent(api -> {
    api.subscribe(NetworkStateChangedEvent.class, event -> {
        // Update scoreboard, tab list, or other UI elements
        updateGlobalPlayerCount(event.globalOnline());
        
        // Check for specific server changes
        for (String server : api.getServerNames()) {
            updateServerStatus(server, api.isServerOnline(server));
        }
    });
});
```

---

## Error Handling

Always check if the API is available and handle connection issues:

```java
public int getGlobalOnlineSafe() {
    return AetherAPI.getInstance()
        .filter(api -> !api.isStateStale())
        .map(AetherAPI::getGlobalOnline)
        .orElse(0);
}
```

---

## Best Practices

1. **Cache the API instance** if calling frequently
2. **Check `isStateStale()`** before displaying critical data
3. **Subscribe to events** instead of polling for updates
4. **Handle disconnection** gracefully in your UI
5. **Use PlaceholderAPI** for simple text replacements
