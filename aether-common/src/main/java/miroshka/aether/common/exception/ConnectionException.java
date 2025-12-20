package miroshka.aether.common.exception;

public final class ConnectionException extends AetherException {

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConnectionException connectionFailed(String host, int port, Throwable cause) {
        return new ConnectionException(
                "Failed to connect to " + host + ":" + port,
                cause);
    }

    public static ConnectionException connectionLost(String reason) {
        return new ConnectionException("Connection lost: " + reason);
    }

    public static ConnectionException channelClosed() {
        return new ConnectionException("Channel is closed");
    }
}
