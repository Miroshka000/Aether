package miroshka.aether.common.exception;

public sealed class AetherException extends RuntimeException
        permits AuthenticationException,
        ConnectionException,
        ProtocolException,
        TimeoutException {

    public AetherException(String message) {
        super(message);
    }

    public AetherException(String message, Throwable cause) {
        super(message, cause);
    }
}
