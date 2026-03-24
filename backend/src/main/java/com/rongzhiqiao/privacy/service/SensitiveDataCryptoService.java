package com.rongzhiqiao.privacy.service;

import com.rongzhiqiao.config.PrivacyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensitiveDataCryptoService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final PrivacyProperties privacyProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plaintext.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt sensitive data", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            String[] parts = ciphertext.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Sensitive data payload is invalid");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt sensitive data", exception);
        }
    }

    private SecretKeySpec buildSecretKey() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(privacyProperties.getEncryptionSecret().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to initialize sensitive data key", exception);
        }
    }
}
