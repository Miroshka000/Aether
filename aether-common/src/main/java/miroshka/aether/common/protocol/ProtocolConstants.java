package miroshka.aether.common.protocol;

public final class ProtocolConstants {

    public static final int PROTOCOL_VERSION = 1;

    public static final int DEFAULT_PORT = 3000;

    public static final int METRICS_PORT = 9090;

    public static final int FRAME_LENGTH_FIELD_SIZE = 4;

    public static final int FRAME_FLAGS_SIZE = 1;

    public static final int FRAME_PACKET_ID_SIZE = 4;

    public static final int FRAME_HEADER_SIZE = FRAME_LENGTH_FIELD_SIZE + FRAME_FLAGS_SIZE + FRAME_PACKET_ID_SIZE;

    public static final int MAX_FRAME_SIZE = 1024 * 1024;

    public static final int COMPRESSION_THRESHOLD = 1024;

    public static final int MAX_PROPERTY_MAP_ENTRIES = 1000;

    public static final int MAX_PROPERTY_VALUE_SIZE = 1024;

    public static final int VARINT_MAX_BYTES = 5;

    public static final long CLOCK_SKEW_THRESHOLD_MILLIS = 5000;

    public static final int HEARTBEAT_INTERVAL_MILLIS = 5000;

    public static final int HEARTBEAT_TIMEOUT_MILLIS = 15000;

    public static final int MISSED_HEARTBEATS_THRESHOLD = 3;

    public static final int BROADCAST_INTERVAL_MILLIS = 500;

    public static final int SNAPSHOT_INTERVAL_MILLIS = 200;

    public static final int RECONNECTION_INITIAL_DELAY_MILLIS = 1000;

    public static final int RECONNECTION_MAX_DELAY_MILLIS = 30000;

    public static final int RECONNECTION_MULTIPLIER = 2;

    public static final int CONNECTION_TIMEOUT_MILLIS = 10000;

    public static final int AUTH_TIMEOUT_MILLIS = 10000;

    public static final int STATE_STALE_THRESHOLD_MILLIS = 15000;

    public static final int CIRCUIT_BREAKER_DURATION_MILLIS = 30000;

    public static final int CIRCUIT_BREAKER_QUEUE_THRESHOLD = 10000;

    public static final double CIRCUIT_BREAKER_CPU_THRESHOLD = 0.90;

    public static final double CIRCUIT_BREAKER_MEMORY_THRESHOLD = 0.85;

    public static final int RATE_LIMIT_PACKETS_PER_SECOND = 100;

    public static final int RATE_LIMIT_BURST_SIZE = 200;

    public static final int METRICS_HISTORY_CAPACITY = 100;

    public static final byte FLAG_COMPRESSION_ENABLED = 0x01;

    public static final byte FLAG_PRIORITY_CRITICAL = 0x02;

    private ProtocolConstants() {
    }
}
