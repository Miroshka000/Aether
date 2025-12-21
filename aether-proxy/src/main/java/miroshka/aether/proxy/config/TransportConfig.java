package miroshka.aether.proxy.config;

import java.util.List;
import java.util.Objects;

public record TransportConfig(
        boolean enabled,
        Protocol protocol,
        Compression compression,
        List<String> servers) {

    public TransportConfig {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(compression, "compression");
        Objects.requireNonNull(servers, "servers");
    }

    public static TransportConfig defaults() {
        return new TransportConfig(false, Protocol.TCP, Compression.ZSTD, List.of());
    }

    public static TransportConfig disabled() {
        return new TransportConfig(false, Protocol.TCP, Compression.ZSTD, List.of());
    }

    public enum Protocol {
        TCP,
        QUIC
    }

    public enum Compression {
        ZLIB,
        SNAPPY,
        ZSTD
    }
}
