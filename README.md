<div align="center">

# ⚡ Aether Network Core

**High-Performance Network Bridge for WaterdogPE & AllayMC**

[![License](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0-green.svg)](https://github.com/Miroshka000/Aether/releases)
[![Java](https://img.shields.io/badge/Java-21+-purple.svg)](https://adoptium.net/)
[![Netty](https://img.shields.io/badge/Netty-4.2.x-orange.svg)](https://netty.io/)
[![Build](https://github.com/Miroshka000/Aether/actions/workflows/build.yml/badge.svg)](https://github.com/Miroshka000/Aether/actions)

<br>

[![Русский](https://img.shields.io/badge/Language-Русский-red?style=for-the-badge&logo=google-translate&logoColor=white)](README_RU.md)

</div>

---

**Aether** is a blazing-fast, low-latency TCP bridge connecting **WaterdogPE Proxy** (Master) with multiple **AllayMC Servers** (Nodes). Built with Netty for maximum performance and designed for real-time cluster state synchronization.

## ✨ Features

### Core
- **Ultra-Low Latency**: < 2ms network latency with TCP_NODELAY and Netty optimizations
- **Binary Protocol**: Custom framing with VarInt encoding and Snappy compression
- **Star Topology**: Proxy acts as Master, orchestrating network state across all Nodes
- **Real-time Sync**: Automatic state broadcasting with configurable intervals
- **Circuit Breaker**: Smart backpressure handling to prevent cascade failures
- **Heartbeat System**: Connection liveness detection with RTT metrics
- **Secure Auth**: SHA-256 key hashing with constant-time comparison

### Advanced Modules (v1.0.1+)
- **🌀 Seamless World Streaming**: Cross-server portals with pre-loaded chunks
- **📡 Event Broadcasting**: Cross-server events with LuckPerms group filtering
- **💾 Distributed PDC**: PersistentDataContainer sync across network
- **⚖️ Smart Load Balancer**: Multiple strategies (Round Robin, Least Connections, etc.)
- **🔧 Packet Rewrite Pipeline**: Entity filtering, resource pack overrides
- **🌐 Web Admin Panel**: REST API + WebSocket for real-time monitoring
- **🔌 Addons System**: Extensible addon architecture (coming soon)

## 🏗️ Architecture

```
┌─────────────────┐         TCP/Binary         ┌─────────────────┐
│   WaterdogPE    │◄─────────────────────────►│    AllayMC      │
│    (MASTER)     │                            │    (NODE 1)     │
│                 │         TCP/Binary         ├─────────────────┤
│  aether-proxy   │◄─────────────────────────►│    AllayMC      │
│                 │                            │    (NODE 2)     │
│  Port: 3000     │         TCP/Binary         ├─────────────────┤
│                 │◄─────────────────────────►│    AllayMC      │
│  Web: 8080      │                            │    (NODE N)     │
└─────────────────┘                            └─────────────────┘
```

## 📡 Protocol

| Packet | ID | Direction | Description |
|--------|-------|-----------|-------------|
| `AuthHandshake` | 0x01 | Node→Master | Initial authentication |
| `AuthResult` | 0x02 | Master→Node | Auth response |
| `Heartbeat` | 0x10 | Bidirectional | Connection liveness |
| `HeartbeatAck` | 0x11 | Bidirectional | RTT measurement |
| `NodeSnapshot` | 0x20 | Node→Master | Server state report |
| `NetworkState` | 0x21 | Master→Nodes | Cluster state broadcast |
| `TransferRequest` | 0x30 | Bidirectional | Player transfer |
| `PortalSync` | 0x31 | Node↔Master | Portal configuration |
| `EventBroadcast` | 0x32 | Bidirectional | Cross-server events |
| `PDCSync` | 0x33 | Bidirectional | PDC synchronization |
| `ChunkData` | 0x34 | Node↔Node | Chunk streaming |

## 🔌 Modules

| Module | Description |
|--------|-------------|
| `aether-common` | Shared protocol, codecs, events, exceptions |
| `aether-api` | Public API for third-party plugins |
| `aether-proxy` | WaterdogPE plugin (Master role) |
| `aether-server` | AllayMC plugin (Node role) |
| `aether-web` | Web Admin Panel (Javalin + React) |
| `aether-addons` | Addon system foundation |

## 📦 Installation

### Maven/Gradle (API Only)

```xml
<!-- Maven -->
<dependency>
    <groupId>io.github.miroshka000</groupId>
    <artifactId>aether-api</artifactId>
    <version>1.0.1</version>
    <scope>provided</scope>
</dependency>
```

```kotlin
// Gradle
compileOnly("io.github.miroshka000:aether-api:1.0.1")
```

### Download

Get the latest release from [GitHub Releases](https://github.com/Miroshka000/Aether/releases).

### WaterdogPE Proxy
1. Download `aether-proxy-*.jar`
2. Place in `plugins/` folder
3. Restart the proxy
4. Configure `plugins/aether-proxy/config.yml`

### AllayMC Server
1. Download `aether-server-*.jar`
2. Place in `plugins/` folder
3. Restart the server
4. Configure `plugins/aether-server/config.yml`

## 🎮 API Usage

### Basic API

```java
import miroshka.aether.api.AetherAPI;

AetherAPI.getInstance().ifPresent(api -> {
    int globalOnline = api.getGlobalOnline();
    int serverCount = api.getServerCount();
    
    // Per-server data
    int lobbyOnline = api.getServerOnline("lobby");
    double lobbyTps = api.getServerTps("lobby");
});
```

### Event Broadcasting

```java
api.getEventBridge().ifPresent(bridge -> {
    bridge.publish("PlayerAchievement", playerUuid, playerName, 
        List.of("vip", "survivor"), 
        Map.of("achievement", "First Kill"));
    
    bridge.subscribe("CustomEvent", event -> {
        System.out.println("Received: " + event.eventType());
    });
});
```

### Portal Management

```java
api.getPortalManager().ifPresent(portals -> {
    portals.transferPlayer(playerUuid, "survival", true); // seamless
});
```

### Load Balancing

```java
api.getLoadBalancer().ifPresent(balancer -> {
    balancer.selectServer(playerUuid, List.of("lobby-1", "lobby-2"))
        .ifPresent(server -> player.connect(server));
});
```

## ⚙️ Configuration Files

See detailed configuration in:
- `portals.yml` - Portal definitions
- `events.yml` - Event subscriptions
- `load-balancer.yml` - Balancing strategies
- `packet-rewrite.yml` - Packet transformers

## 🔧 Build from Source

```bash
git clone https://github.com/Miroshka000/Aether.git
cd Aether
./gradlew build

# Run tests
./gradlew test
```

## 🔗 Requirements

- **Java 21+**
- **WaterdogPE 2.0+** (for proxy plugin)
- **AllayMC 0.18+** (for server plugin)
- **LuckPerms** (optional, for permission-based filtering)

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
    <br>
    <p>Created by <b>Miroshka</b> for <b>WaterdogPE + AllayMC</b> clusters with ⚡</p>
</div>
