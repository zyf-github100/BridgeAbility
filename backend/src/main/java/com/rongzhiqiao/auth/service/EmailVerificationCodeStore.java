package com.rongzhiqiao.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationCodeStore {

    private final Map<String, CodeEntry> codeEntries = new ConcurrentHashMap<>();
    private final Map<String, Instant> cooldownEntries = new ConcurrentHashMap<>();

    public synchronized long getCooldownRemainingSeconds(String key) {
        Instant now = Instant.now();
        Instant cooldownUntil = cooldownEntries.get(key);
        if (cooldownUntil == null) {
            return 0;
        }
        if (!cooldownUntil.isAfter(now)) {
            cooldownEntries.remove(key);
            return 0;
        }
        return Math.max(1, Duration.between(now, cooldownUntil).getSeconds());
    }

    public synchronized void save(String key, String code, Duration codeTtl, Duration cooldownTtl) {
        Instant now = Instant.now();
        codeEntries.put(key, new CodeEntry(code, now.plus(codeTtl), 0));
        cooldownEntries.put(key, now.plus(cooldownTtl));
    }

    public synchronized VerificationResult verify(String key, String submittedCode, int maxAttempts) {
        CodeEntry entry = codeEntries.get(key);
        if (entry == null) {
            return VerificationResult.NOT_FOUND;
        }

        Instant now = Instant.now();
        if (!entry.expiresAt().isAfter(now)) {
            codeEntries.remove(key);
            return VerificationResult.EXPIRED;
        }

        if (!entry.code().equals(submittedCode)) {
            int nextFailures = entry.failedAttempts() + 1;
            if (nextFailures >= maxAttempts) {
                codeEntries.remove(key);
                return VerificationResult.TOO_MANY_ATTEMPTS;
            }
            codeEntries.put(key, new CodeEntry(entry.code(), entry.expiresAt(), nextFailures));
            return VerificationResult.INVALID;
        }

        codeEntries.remove(key);
        return VerificationResult.SUCCESS;
    }

    private record CodeEntry(String code, Instant expiresAt, int failedAttempts) {
    }

    public enum VerificationResult {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        INVALID,
        TOO_MANY_ATTEMPTS
    }
}
