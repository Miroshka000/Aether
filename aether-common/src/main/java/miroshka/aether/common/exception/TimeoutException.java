package miroshka.aether.common.exception;

public final class TimeoutException extends AetherException {

    private final long timeoutMillis;

    public TimeoutException(String message, long timeoutMillis) {
        super(message);
        this.timeoutMillis = timeoutMillis;
    }

    public long timeoutMillis() {
        return timeoutMillis;
    }

    public static TimeoutException connectionTimeout(long timeoutMillis) {
        return new TimeoutException("Connection timeout after " + timeoutMillis + "ms", timeoutMillis);
    }

    public static TimeoutException heartbeatTimeout(long timeoutMillis) {
        return new TimeoutException("Heartbeat timeout after " + timeoutMillis + "ms", timeoutMillis);
    }

    public static TimeoutException authenticationTimeout(long timeoutMillis) {
        return new TimeoutException("Authentication timeout after " + timeoutMillis + "ms", timeoutMillis);
    }
}
