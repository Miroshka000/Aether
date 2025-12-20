package miroshka.aether.proxy.security;

import java.util.concurrent.atomic.AtomicLong;

public final class RateLimiter {

    private final int maxTokens;
    private final int refillRatePerSecond;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTimestamp;

    public RateLimiter(int maxTokens, int refillRatePerSecond) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be positive");
        }
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = new AtomicLong(maxTokens);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public boolean tryAcquire() {
        refill();
        long currentTokens = tokens.get();
        while (currentTokens > 0) {
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
            currentTokens = tokens.get();
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTimestamp.get();
        long elapsedMillis = now - lastRefill;

        if (elapsedMillis < 1000) {
            return;
        }

        if (lastRefillTimestamp.compareAndSet(lastRefill, now)) {
            long tokensToAdd = (elapsedMillis / 1000) * refillRatePerSecond;
            long currentTokens = tokens.get();
            long newTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
            tokens.set(newTokens);
        }
    }
}
