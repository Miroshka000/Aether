package miroshka.aether.web.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public final class JwtService {

    private static final String ISSUER = "aether-web";
    private static final long ACCESS_TOKEN_VALIDITY_HOURS = 24;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final Algorithm algorithm;

    public JwtService(String secret) {
        this.algorithm = Algorithm.HMAC256(Objects.requireNonNull(secret, "secret"));
    }

    public String generateAccessToken(String username, String role) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim("role", role)
                .withClaim("type", "access")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(ACCESS_TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS)))
                .sign(algorithm);
    }

    public String generateRefreshToken(String username) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim("type", "refresh")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS)))
                .sign(algorithm);
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public Optional<DecodedJWT> decodeToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
            return Optional.of(decoded);
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getUsername(String token) {
        return decodeToken(token).map(DecodedJWT::getSubject);
    }

    public Optional<String> getRole(String token) {
        return decodeToken(token).map(jwt -> jwt.getClaim("role").asString());
    }

    public boolean isRefreshToken(String token) {
        return decodeToken(token)
                .map(jwt -> "refresh".equals(jwt.getClaim("type").asString()))
                .orElse(false);
    }
}
