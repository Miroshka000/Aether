package miroshka.aether.proxy.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

public final class SecretKeyValidator {

    private final List<String> validKeyHashes;

    public SecretKeyValidator(List<String> secretKeys) {
        Objects.requireNonNull(secretKeys, "secretKeys");
        this.validKeyHashes = secretKeys.stream()
                .map(SecretKeyValidator::hashKey)
                .toList();
    }

    public boolean validate(String providedKey) {
        Objects.requireNonNull(providedKey, "providedKey");
        String providedHash = hashKey(providedKey);
        return validKeyHashes.stream()
                .anyMatch(hash -> constantTimeEquals(hash, providedHash));
    }

    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
