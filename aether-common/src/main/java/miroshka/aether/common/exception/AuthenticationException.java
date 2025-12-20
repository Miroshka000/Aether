package miroshka.aether.common.exception;

public final class AuthenticationException extends AetherException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AuthenticationException invalidSecretKey() {
        return new AuthenticationException("Invalid secret key");
    }

    public static AuthenticationException protocolVersionMismatch(int expected, int actual) {
        return new AuthenticationException(
                "Protocol version mismatch: expected " + expected + ", got " + actual);
    }

    public static AuthenticationException clockSkewDetected(long skewMillis) {
        return new AuthenticationException(
                "Clock skew detected: " + skewMillis + "ms");
    }

    public static AuthenticationException timeout() {
        return new AuthenticationException("Authentication timeout");
    }
}
