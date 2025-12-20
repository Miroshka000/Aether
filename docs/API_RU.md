# Документация Aether API

Полная документация API для Aether Network Core.

## Содержание
- [Начало работы](#начало-работы)
- [Базовый API](#базовый-api)
- [Event Bridge](#event-bridge)
- [Portal Manager](#portal-manager)
- [Load Balancer](#load-balancer)
- [Distributed PDC](#distributed-pdc)
- [LuckPerms интеграция](#luckperms-интеграция)
- [PlaceholderAPI](#placeholderapi)
- [Примеры](#примеры)

---

## Начало работы

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

## Базовый API

### Получение инстанса

```java
import miroshka.aether.api.AetherAPI;

AetherAPI.getInstance().ifPresent(api -> {
    int globalOnline = api.getGlobalOnline();
    int serverCount = api.getServerCount();
    
    int lobbyOnline = api.getServerOnline("lobby");
    double lobbyTps = api.getServerTps("lobby");
});
```

### Методы AetherAPI

| Метод | Возвращает | Описание |
|-------|------------|----------|
| `getGlobalOnline()` | `int` | Всего игроков в сети |
| `getServerCount()` | `int` | Количество серверов |
| `getServerNames()` | `List<String>` | Список имён серверов |
| `getServerOnline(name)` | `int` | Игроки на сервере |
| `getServerMaxPlayers(name)` | `int` | Макс. игроков |
| `getServerTps(name)` | `double` | TPS сервера |
| `isServerOnline(name)` | `boolean` | Онлайн ли сервер |
| `getLatencyMillis()` | `int` | Пинг до Master |

---

## Event Bridge

Кросс-серверная рассылка событий с поддержкой LuckPerms групп.

### Публикация событий

```java
api.getEventBridge().ifPresent(bridge -> {
    bridge.publish("PlayerAchievement", Map.of(
        "achievement", "Первое убийство",
        "player", player.getName()
    ));
    
    bridge.publish("VIPReward", 
        player.getUniqueId(), 
        player.getName(),
        List.of("vip", "supporter"),
        Map.of("reward", "Алмазный меч")
    );
});
```

### Подписка на события

```java
api.getEventBridge().ifPresent(bridge -> {
    bridge.subscribe("PlayerAchievement", event -> {
        broadcastMessage(event.playerName() + " получил " + 
            event.eventData().get("achievement"));
    });
    
    bridge.subscribe("StaffAlert", 
        EventFilter.requireGroups("admin", "moderator"), 
        event -> { /* только для стаффа */ }
    );
});
```

---

## Portal Manager

Бесшовные переносы между серверами.

```java
api.getPortalManager().ifPresent(portals -> {
    // Бесшовный перенос (предзагрузка чанков)
    portals.transferPlayer(player.getUniqueId(), "survival", true);
    
    // Перенос на координаты
    portals.transferPlayer(player.getUniqueId(), "survival", 100.5, 64.0, -200.5);
});
```

---

## Load Balancer

Умная балансировка нагрузки с приоритетом VIP.

```java
api.getLoadBalancer().ifPresent(balancer -> {
    balancer.selectServer(player.getUniqueId(), 
            List.of("lobby-1", "lobby-2", "lobby-3"))
        .ifPresent(server -> player.connect(server));
    
    // С конкретной стратегией
    balancer.selectServer(player.getUniqueId(), 
            List.of("survival-1", "survival-2"),
            BalancingStrategy.LEAST_TPS_LOAD);
});
```

### Стратегии

| Стратегия | Описание |
|-----------|----------|
| `ROUND_ROBIN` | Последовательный перебор |
| `LEAST_CONNECTIONS` | Меньше всего игроков |
| `LEAST_TPS_LOAD` | Лучший TPS |
| `RANDOM` | Случайный выбор |
| `PRIORITY_QUEUE` | VIP первые (LuckPerms) |

---

## Distributed PDC

Синхронизация PersistentDataContainer по сети.

```java
api.getDistributedPDC().ifPresent(pdc -> {
    UUID playerId = player.getUniqueId();
    
    pdc.set(playerId, "coins", 1500);
    int coins = pdc.get(playerId, "coins", Integer.class).orElse(0);
    pdc.remove(playerId, "temp_data");
});
```

---

## LuckPerms интеграция

```java
api.getLuckPerms().ifPresent(lp -> {
    lp.getPlayerGroups(player.getUniqueId())
        .thenAccept(groups -> logger.info("Группы: " + groups));
    
    lp.isInGroup(player.getUniqueId(), "vip")
        .thenAccept(isVip -> { /* ... */ });
    
    lp.hasPermission(player.getUniqueId(), "aether.admin")
        .thenAccept(hasPerm -> { /* ... */ });
});
```

---

## PlaceholderAPI

### Глобальные

| Плейсхолдер | Описание |
|-------------|----------|
| `%aether_global_online%` | Всего игроков |
| `%aether_server_count%` | Кол-во серверов |
| `%aether_latency%` | Пинг до Master |

### По серверам

| Плейсхолдер | Описание |
|-------------|----------|
| `%aether_<server>_online%` | Игроки на сервере |
| `%aether_<server>_tps%` | TPS сервера |
| `%aether_<server>_status%` | online/offline |

---

## Примеры

### Авто-подключение к лучшему серверу

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

### Кросс-серверный чат

```java
// Отправитель
bridge.publish("GlobalChat", Map.of(
    "player", player.getName(),
    "message", message,
    "server", serverName
));

// Получатель
bridge.subscribe("GlobalChat", event -> {
    String msg = String.format("[%s] %s: %s",
        event.eventData().get("server"),
        event.eventData().get("player"),
        event.eventData().get("message"));
    broadcastMessage(msg);
});
```
