package miroshka.aether.common.exception;

public final class ProtocolException extends AetherException {

    private final int packetId;

    public ProtocolException(String message, int packetId) {
        super(message);
        this.packetId = packetId;
    }

    public ProtocolException(String message, int packetId, Throwable cause) {
        super(message, cause);
        this.packetId = packetId;
    }

    public int packetId() {
        return packetId;
    }

    public static ProtocolException malformedPacket(int packetId, String details) {
        return new ProtocolException("Malformed packet: " + details, packetId);
    }

    public static ProtocolException unknownPacket(int packetId) {
        return new ProtocolException("Unknown packet ID: 0x" + Integer.toHexString(packetId), packetId);
    }

    public static ProtocolException encodingFailed(int packetId, Throwable cause) {
        return new ProtocolException("Failed to encode packet", packetId, cause);
    }

    public static ProtocolException decodingFailed(int packetId, Throwable cause) {
        return new ProtocolException("Failed to decode packet", packetId, cause);
    }
}
