package miroshka.aether.web.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.javalin.http.Context;
import miroshka.aether.web.security.JwtService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class AuthController {

    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_ADMIN_PASS = "admin";
    private static final Path CREDENTIALS_FILE = Path.of("plugins/Aether/web-credentials.dat");

    private final JwtService jwtService;
    private String currentPasswordHash;
    private boolean usingDefaultPassword = true;

    public AuthController(JwtService jwtService) {
        this.jwtService = Objects.requireNonNull(jwtService, "jwtService");
        loadCredentials();
    }

    private void loadCredentials() {
        try {
            if (Files.exists(CREDENTIALS_FILE)) {
                currentPasswordHash = Files.readString(CREDENTIALS_FILE).trim();
                usingDefaultPassword = false;
            } else {
                currentPasswordHash = BCrypt.withDefaults().hashToString(12, DEFAULT_ADMIN_PASS.toCharArray());
                usingDefaultPassword = true;
            }
        } catch (IOException e) {
            currentPasswordHash = BCrypt.withDefaults().hashToString(12, DEFAULT_ADMIN_PASS.toCharArray());
            usingDefaultPassword = true;
        }
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

        ctx.json(new LoginResponse(accessToken, refreshToken, request.username(), role, usingDefaultPassword));
    }

    public void changePassword(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }

        String jwt = token.substring(7);
        var username = jwtService.getUsername(jwt);
        if (username.isEmpty()) {
            ctx.status(401).json(Map.of("error", "Invalid token"));
            return;
        }

        ChangePasswordRequest request = ctx.bodyAsClass(ChangePasswordRequest.class);
        if (request.newPassword() == null || request.newPassword().length() < 8) {
            ctx.status(400).json(Map.of("error", "Password must be at least 8 characters"));
            return;
        }

        currentPasswordHash = BCrypt.withDefaults().hashToString(12, request.newPassword().toCharArray());
        usingDefaultPassword = false;

        try {
            Files.createDirectories(CREDENTIALS_FILE.getParent());
            Files.writeString(CREDENTIALS_FILE, currentPasswordHash);
            ctx.json(Map.of("success", true, "message", "Password changed successfully"));
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", "Failed to save password: " + e.getMessage()));
        }
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

        ctx.json(new LoginResponse(accessToken, newRefreshToken, username.get(), role, usingDefaultPassword));
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

        ctx.json(new UserInfo(username.get(), role.get(), usingDefaultPassword));
    }

    private boolean validateCredentials(String username, String password) {
        if (!DEFAULT_ADMIN_USER.equals(username)) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), currentPasswordHash);
        return result.verified;
    }

    private String getRole(String username) {
        return "admin".equals(username) ? "ADMIN" : "USER";
    }

    public record LoginRequest(String username, String password) {
    }

    public record ChangePasswordRequest(String newPassword) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record LoginResponse(String accessToken, String refreshToken, String username, String role,
            boolean isDefaultPassword) {
    }

    public record UserInfo(String username, String role, boolean isDefaultPassword) {
    }
}
