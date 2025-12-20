package miroshka.aether.common.protocol;

public final class PacketIds {

    public static final int AUTH_HANDSHAKE = 0x01;

    public static final int AUTH_RESULT = 0x02;

    public static final int HEARTBEAT = 0x03;

    public static final int HEARTBEAT_ACK = 0x04;

    public static final int NODE_SNAPSHOT = 0x10;

    public static final int NETWORK_STATE = 0x11;

    public static final int METRICS_REPORT = 0x12;

    public static final int CIRCUIT_BREAKER_TRIPPED = 0x20;

    public static final int PROTOCOL_ERROR = 0x21;

    private PacketIds() {
    }
}
