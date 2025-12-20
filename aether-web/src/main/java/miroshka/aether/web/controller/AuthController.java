package miroshka.aether.web.controller;

import io.javalin.http.Context;
import miroshka.aether.web.security.JwtService;

import java.util.Map;
import java.util.Objects;

public final class AuthController {

    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_ADMIN_PASS = "aether123";

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = Objects.requireNonNull(jwtService, "jwtService");
    }

    public void login(Context ctx) {
        LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

        if (!validateCredentials(request.username(), request.password())) {
            ctx.status(401).json(Map.of("error", "Invalid credentials"));
            return;
        }

        String role = getRole(request.username());
        String accessToken = jwtService.generateAccessToken(request.username(), role);
        String refreshToken = jwtService.generateRefreshToken(request.username());

        ctx.json(new LoginResponse(accessToken, refreshToken, request.username(), role));
    }

    public void refresh(Context ctx) {
        RefreshRequest request = ctx.bodyAsClass(RefreshRequest.class);

        if (!jwtService.isRefreshToken(request.refreshToken())) {
            ctx.status(401).json(Map.of("error", "Invalid refresh token"));
            return;
        }

        var username = jwtService.getUsername(request.refreshToken());
        if (username.isEmpty()) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return;
        }

        String role = getRole(username.get());
        String accessToken = jwtService.generateAccessToken(username.get(), role);
        String newRefreshToken = jwtService.generateRefreshToken(username.get());

        ctx.json(new LoginResponse(accessToken, newRefreshToken, username.get(), role));
    }

    public void me(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }

        String jwt = token.substring(7);
        var username = jwtService.getUsername(jwt);
        var role = jwtService.getRole(jwt);

        if (username.isEmpty() || role.isEmpty()) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return;
        }

        ctx.json(new UserInfo(username.get(), role.get()));
    }

    private boolean validateCredentials(String username, String password) {
        return DEFAULT_ADMIN_USER.equals(username) && DEFAULT_ADMIN_PASS.equals(password);
    }

    private String getRole(String username) {
        return "admin".equals(username) ? "ADMIN" : "USER";
    }

    public record LoginRequest(String username, String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record LoginResponse(String accessToken, String refreshToken, String username, String role) {
    }

    public record UserInfo(String username, String role) {
    }
}
