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

- **Ultra-Low Latency**: < 2ms network latency with TCP_NODELAY and Netty optimizations
- **Binary Protocol**: Custom framing with VarInt encoding and Snappy compression
- **Star Topology**: Proxy acts as Master, orchestrating network state across all Nodes
- **Real-time Sync**: Automatic state broadcasting with configurable intervals
- **Circuit Breaker**: Smart backpressure handling to prevent cascade failures
- **Heartbeat System**: Connection liveness detection with RTT metrics
- **Secure Auth**: SHA-256 key hashing with constant-time comparison
- **Rate Limiting**: Token bucket algorithm to prevent packet flooding
- **PlaceholderAPI**: Full integration with AllayMC PlaceholderAPI for easy data display
- **Public API**: Clean API for third-party plugin integration

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
│                 │                            │    (NODE N)     │
└─────────────────┘                            └─────────────────┘
```

## 📡 Protocol

| Packet | ID | Direction | Priority | Description |
|--------|-------|-----------|----------|-------------|
| `AuthHandshake` | 0x01 | Node→Master | CRITICAL | Initial authentication |
| `AuthResult` | 0x02 | Master→Node | CRITICAL | Auth response |
| `Heartbeat` | 0x10 | Bidirectional | NORMAL | Connection liveness |
| `HeartbeatAck` | 0x11 | Bidirectional | NORMAL | RTT measurement |
| `NodeSnapshot` | 0x20 | Node→Master | NORMAL | Server state report |
| `NetworkState` | 0x21 | Master→Nodes | NORMAL | Cluster state broadcast |
| `MetricsReport` | 0x30 | Node→Master | LOW | Historical metrics |
| `CircuitBreaker` | 0x40 | Master→Nodes | CRITICAL | Backpressure signal |
| `ProtocolError` | 0xFF | Bidirectional | CRITICAL | Error notification |

**Frame Format:**
```
[4 bytes: Length] [1 byte: Flags] [4 bytes: PacketID] [N bytes: Payload]
```

## 🔌 Modules

| Module | Description |
|--------|-------------|
| `aether-common` | Shared protocol, codecs, events, exceptions |
| `aether-api` | Public API for third-party plugins |
| `aether-proxy` | WaterdogPE plugin (Master role) |
| `aether-server` | AllayMC plugin (Node role) |

## 📦 Installation

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

### Docker Setup

For Docker environments, use service names instead of `localhost`:

```yaml
# Server config (aether-server)
master:
  host: "proxy"  # Docker service name
  port: 3000
```

## 🎮 API & PlaceholderAPI

📖 **Full API documentation**: [docs/API.md](docs/API.md)

### Quick Start

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

### PlaceholderAPI Placeholders

| Placeholder | Description |
|------------|-------------|
| `%aether_global_online%` | Total players on network |
| `%aether_server_count%` | Connected servers count |
| `%aether_<server>_online%` | Players on specific server |
| `%aether_<server>_tps%` | TPS of specific server |
| `%aether_latency%` | Ping to Master |

[View all placeholders →](docs/API.md#placeholderapi-integration)

## ⚙️ Configuration

### Proxy (Master)

```yaml
network:
  port: 3000
  metrics-port: 9090
  broadcast-interval-ms: 500
  heartbeat-timeout-ms: 15000
  compression-enabled: true

security:
  secret-keys:
    - "your-secret-key-here"
  allowed-ips: []

limits:
  max-nodes: 100
```

### Server (Node)

```yaml
master:
  host: "localhost"
  port: 3000

server:
  name: "lobby"
  secret-key: "your-secret-key-here"

network:
  heartbeat-interval-ms: 5000
  snapshot-interval-ms: 200
```

## 🔧 Build from Source

```bash
git clone https://github.com/Miroshka000/Aether.git
cd Aether
./gradlew build

# Artifacts:
# - aether-proxy/build/libs/aether-proxy-*.jar
# - aether-server/build/libs/aether-server-*.jar
```

## 🔗 Requirements

- **Java 21+**
- **WaterdogPE 2.0+** (for proxy plugin)
- **AllayMC 0.18+** (for server plugin)
- **PlaceholderAPI** (optional, for placeholder support)

## 📊 Metrics

Prometheus metrics exposed on configured port (default: 9090):

- `aether_nodes_connected` — Connected nodes count
- `aether_global_players` — Total online players
- `aether_packet_latency` — Packet round-trip time
- `aether_packets_sent_total` — Total packets sent
- `aether_packets_received_total` — Total packets received

## 🛡️ Security

- SHA-256 key hashing
- Constant-time key comparison
- IP allowlist support
- Rate limiting per connection
- Clock skew detection

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
    <br>
    <p>Created by <b>Miroshka</b> for <b>WaterdogPE + AllayMC</b> clusters with ⚡</p>
</div>
