# Aether API Documentation

Full API documentation for Aether Network Core.

## Table of Contents
- [Getting Started](#getting-started)
- [Basic API](#basic-api)
- [Event Bridge](#event-bridge)
- [Portal Manager](#portal-manager)
- [Load Balancer](#load-balancer)
- [Distributed PDC](#distributed-pdc)
- [LuckPerms Integration](#luckperms-integration)
- [PlaceholderAPI](#placeholderapi)
- [Examples](#examples)

---

## Getting Started

### Gradle (Kotlin DSL)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.Miroshka000.Aether:aether-api:v1.0.0")
}
```

### Gradle (Groovy)

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.Miroshka000.Aether:aether-api:v1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Miroshka000.Aether</groupId>
    <artifactId>aether-api</artifactId>
    <version>v1.0.0</version>
</dependency>
```

---

## Basic API

### Getting Instance

```java
import miroshka.aether.api.AetherAPI;

AetherAPI.getInstance().ifPresent(api -> {
    int globalOnline = api.getGlobalOnline();
    int serverCount = api.getServerCount();
    
    int lobbyOnline = api.getServerOnline("lobby");
    double lobbyTps = api.getServerTps("lobby");
});
```

### AetherAPI Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getGlobalOnline()` | `int` | Total players on network |
| `getServerCount()` | `int` | Number of servers |
| `getServerNames()` | `List<String>` | Server names list |
| `getServerOnline(name)` | `int` | Players on server |
| `getServerMaxPlayers(name)` | `int` | Max players |
| `getServerTps(name)` | `double` | Server TPS |
| `isServerOnline(name)` | `boolean` | Is server online |
| `getLatencyMillis()` | `int` | Ping to Master |

---

## Event Bridge

Cross-server event broadcasting with LuckPerms group support.

### Publishing Events

```java
api.getEventBridge().ifPresent(bridge -> {
    bridge.publish("PlayerAchievement", Map.of(
        "achievement", "First Kill",
        "player", player.getName()
    ));
    
    bridge.publish("VIPReward", 
        player.getUniqueId(), 
        player.getName(),
        List.of("vip", "supporter"),
        Map.of("reward", "Diamond Sword")
    );
});
```

### Subscribing to Events

```java
api.getEventBridge().ifPresent(bridge -> {
    bridge.subscribe("PlayerAchievement", event -> {
        broadcastMessage(event.playerName() + " achieved " + 
            event.eventData().get("achievement"));
    });
    
    bridge.subscribe("StaffAlert", 
        EventFilter.requireGroups("admin", "moderator"), 
        event -> { /* staff only */ }
    );
});
```

---

## Portal Manager

Seamless cross-server transfers.

```java
api.getPortalManager().ifPresent(portals -> {
    // Seamless transfer (pre-loads chunks)
    portals.transferPlayer(player.getUniqueId(), "survival", true);
    
    // Transfer to coordinates
    portals.transferPlayer(player.getUniqueId(), "survival", 100.5, 64.0, -200.5);
});
```

---

## Load Balancer

Smart load balancing with VIP priority.

```java
api.getLoadBalancer().ifPresent(balancer -> {
    balancer.selectServer(player.getUniqueId(), 
            List.of("lobby-1", "lobby-2", "lobby-3"))
        .ifPresent(server -> player.connect(server));
    
    // With specific strategy
    balancer.selectServer(player.getUniqueId(), 
            List.of("survival-1", "survival-2"),
            BalancingStrategy.LEAST_TPS_LOAD);
});
```

### Strategies

| Strategy | Description |
|----------|-------------|
| `ROUND_ROBIN` | Sequential rotation |
| `LEAST_CONNECTIONS` | Fewest players |
| `LEAST_TPS_LOAD` | Best TPS |
| `RANDOM` | Random selection |
| `PRIORITY_QUEUE` | VIP first (LuckPerms) |

---

## Distributed PDC

PersistentDataContainer sync across network.

```java
api.getDistributedPDC().ifPresent(pdc -> {
    UUID playerId = player.getUniqueId();
    
    pdc.set(playerId, "coins", 1500);
    int coins = pdc.get(playerId, "coins", Integer.class).orElse(0);
    pdc.remove(playerId, "temp_data");
});
```

---

## LuckPerms Integration

```java
api.getLuckPerms().ifPresent(lp -> {
    lp.getPlayerGroups(player.getUniqueId())
        .thenAccept(groups -> logger.info("Groups: " + groups));
    
    lp.isInGroup(player.getUniqueId(), "vip")
        .thenAccept(isVip -> { /* ... */ });
    
    lp.hasPermission(player.getUniqueId(), "aether.admin")
        .thenAccept(hasPerm -> { /* ... */ });
});
```

---

## PlaceholderAPI

### Global

| Placeholder | Description |
|-------------|-------------|
| `%aether_global_online%` | Total players |
| `%aether_server_count%` | Server count |
| `%aether_latency%` | Ping to Master |

### Per Server

| Placeholder | Description |
|-------------|-------------|
| `%aether_<server>_online%` | Players on server |
| `%aether_<server>_tps%` | Server TPS |
| `%aether_<server>_status%` | online/offline |

---

## Examples

### Auto-Join Best Server

```java
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    AetherAPI.getInstance().flatMap(AetherAPI::getLoadBalancer)
        .ifPresent(balancer -> {
            balancer.selectServer(event.getPlayer().getUniqueId(), 
                    List.of("lobby-1", "lobby-2"))
                .ifPresent(server -> event.getPlayer().connect(server));
        });
}
```

### Cross-Server Chat

```java
// Sender
bridge.publish("GlobalChat", Map.of(
    "player", player.getName(),
    "message", message,
    "server", serverName
));

// Receiver
bridge.subscribe("GlobalChat", event -> {
    String msg = String.format("[%s] %s: %s",
        event.eventData().get("server"),
        event.eventData().get("player"),
        event.eventData().get("message"));
    broadcastMessage(msg);
});
```
