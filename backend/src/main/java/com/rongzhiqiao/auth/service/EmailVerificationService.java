package com.rongzhiqiao.auth.service;

import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.config.AuthVerificationProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REGISTER_SCENE = "register";
    private static final String PASSWORD_RESET_SCENE = "password-reset";

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final AuthVerificationProperties verificationProperties;
    private final EmailVerificationCodeStore verificationCodeStore;
    private final SysUserMapper sysUserMapper;

    public void sendRegisterCode(String rawEmail) {
        ensureMailConfigured();

        String email = normalizeEmail(rawEmail);
        if (sysUserMapper.selectByEmail(email) != null) {
            throw new BusinessException(4001, "该邮箱已被注册");
        }

        sendSceneCode(REGISTER_SCENE, email);
    }

    public void verifyRegisterCode(String rawEmail, String rawCode) {
        verifySceneCode(REGISTER_SCENE, rawEmail, rawCode);
    }

    public void sendPasswordResetCode(String rawEmail) {
        ensureMailConfigured();

        String email = normalizeEmail(rawEmail);
        if (sysUserMapper.selectByEmail(email) == null) {
            throw new BusinessException(4004, "未找到绑定该邮箱的账号");
        }

        sendSceneCode(PASSWORD_RESET_SCENE, email);
    }

    public void verifyPasswordResetCode(String rawEmail, String rawCode) {
        verifySceneCode(PASSWORD_RESET_SCENE, rawEmail, rawCode);
    }

    private void sendSceneCode(String scene, String email) {
        String storeKey = buildStoreKey(scene, email);
        long cooldownRemaining = verificationCodeStore.getCooldownRemainingSeconds(storeKey);
        if (cooldownRemaining > 0) {
            throw new BusinessException(4001, "发送过于频繁，请稍后再试");
        }

        String code = generateCode();
        sendEmail(email, code);
        verificationCodeStore.save(
                storeKey,
                code,
                Duration.ofMinutes(verificationProperties.getExpireMinutes()),
                Duration.ofSeconds(verificationProperties.getCooldownSeconds())
        );
    }

    private void verifySceneCode(String scene, String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = rawCode == null ? "" : rawCode.trim();
        EmailVerificationCodeStore.VerificationResult result = verificationCodeStore.verify(
                buildStoreKey(scene, email),
                code,
                verificationProperties.getMaxAttempts()
        );

        switch (result) {
            case SUCCESS -> {
                return;
            }
            case NOT_FOUND, EXPIRED -> throw new BusinessException(4001, "验证码已失效，请重新发送");
            case INVALID -> throw new BusinessException(4001, "验证码错误");
            case TOO_MANY_ATTEMPTS -> throw new BusinessException(4001, "验证码错误次数过多，请重新发送");
            default -> throw new BusinessException(5000, "验证码校验失败");
        }
    }

    private void sendEmail(String email, String code) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject(verificationProperties.getSubject());
            helper.setText(buildMailBody(code), false);
            helper.setFrom(new InternetAddress(resolveFromAddress(), verificationProperties.getFromName(), StandardCharsets.UTF_8.name()).toString());
            javaMailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.error("Failed to send verification email to {}", email, exception);
            throw new BusinessException(5000, "验证码邮件发送失败，请检查 SMTP 配置");
        }
    }

    private String buildMailBody(String code) {
        return """
                Your BridgeAbility verification code is: %s

                This code expires in %d minutes.
                If you did not request it, you can ignore this email.
                """.formatted(code, verificationProperties.getExpireMinutes());
    }

    private String resolveFromAddress() {
        String fromAddress = verificationProperties.getFromAddress();
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = mailProperties.getUsername();
        }
        return fromAddress;
    }

    private void ensureMailConfigured() {
        if (isBlank(mailProperties.getHost()) || isBlank(mailProperties.getUsername()) || isBlank(mailProperties.getPassword())) {
            throw new BusinessException(5000, "邮箱服务未配置，请先设置 SMTP 参数");
        }
    }

    private String buildStoreKey(String scene, String email) {
        return scene + ":" + email;
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(verificationProperties.getCodeLength());
        for (int index = 0; index < verificationProperties.getCodeLength(); index++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
