package com.rongzhiqiao.auth.service;

import com.rongzhiqiao.auth.dto.LoginRequest;
import com.rongzhiqiao.auth.dto.ResetPasswordRequest;
import com.rongzhiqiao.auth.dto.RegisterRequest;
import com.rongzhiqiao.auth.dto.SendPasswordResetCodeRequest;
import com.rongzhiqiao.auth.entity.SysRole;
import com.rongzhiqiao.auth.entity.SysUser;
import com.rongzhiqiao.auth.entity.SysUserRole;
import com.rongzhiqiao.auth.mapper.SysRoleMapper;
import com.rongzhiqiao.auth.mapper.SysUserMapper;
import com.rongzhiqiao.auth.mapper.SysUserRoleMapper;
import com.rongzhiqiao.auth.vo.AuthTokenResponse;
import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.config.AuthVerificationProperties;
import com.rongzhiqiao.config.JwtProperties;
import com.rongzhiqiao.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthVerificationProperties verificationProperties;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        if (sysUserMapper.selectByAccount(request.getAccount()) != null) {
            throw new BusinessException(4001, "账号已存在");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (normalizedEmail != null && sysUserMapper.selectByEmail(normalizedEmail) != null) {
            throw new BusinessException(4001, "该邮箱已被注册");
        }
        if (verificationProperties.isRequireEmailCode()) {
            if (normalizedEmail == null) {
                throw new BusinessException(4001, "请填写邮箱地址");
            }
            if (request.getEmailVerificationCode() == null || request.getEmailVerificationCode().isBlank()) {
                throw new BusinessException(4001, "请输入邮箱验证码");
            }
            emailVerificationService.verifyRegisterCode(normalizedEmail, request.getEmailVerificationCode());
        } else if (normalizedEmail != null
                && request.getEmailVerificationCode() != null
                && !request.getEmailVerificationCode().isBlank()) {
            emailVerificationService.verifyRegisterCode(normalizedEmail, request.getEmailVerificationCode());
        }

        List<String> roleCodes = normalizeRoleCodes(request.getRoles());
        List<SysRole> roles = sysRoleMapper.selectByRoleCodes(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BusinessException(4001, "存在未配置的角色");
        }

        SysUser user = new SysUser();
        user.setAccount(request.getAccount());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(normalizedEmail);
        user.setStatus(1);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);

        List<SysUserRole> userRoles = roles.stream().map(role -> {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            return userRole;
        }).toList();
        sysUserRoleMapper.batchInsert(userRoles);

        return buildAuthTokenResponse(user, roles.stream().map(SysRole::getRoleCode).toList());
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectByAccount(request.getAccount());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(4001, "账号或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(4001, "账号或密码错误");
        }

        List<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes.isEmpty()) {
            throw new BusinessException(4003, "账号未分配角色");
        }

        sysUserMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        return buildAuthTokenResponse(user, roleCodes);
    }

    public void sendRegisterEmailCode(String email) {
        emailVerificationService.sendRegisterCode(email);
    }

    public void sendPasswordResetEmailCode(SendPasswordResetCodeRequest request) {
        SysUser user = requireUserForPasswordReset(request.getAccount(), request.getEmail());
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException(4001, "该账号未绑定邮箱，暂不支持在线找回");
        }
        emailVerificationService.sendPasswordResetCode(user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        SysUser user = requireUserForPasswordReset(request.getAccount(), request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (password.length() < 8) {
            throw new BusinessException(4001, "新密码至少需要 8 个字符");
        }
        emailVerificationService.verifyPasswordResetCode(user.getEmail(), request.getEmailVerificationCode());
        sysUserMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(password));
    }

    public void logout() {
        // Stateless JWT logout placeholder.
    }

    private AuthTokenResponse buildAuthTokenResponse(SysUser user, List<String> roleCodes) {
        List<String> normalizedRoleCodes = roleCodes.stream()
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .toList();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getAccount(), normalizedRoleCodes);
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtProperties.getAccessTokenExpireMinutes() * 60)
                .userId(user.getId())
                .account(user.getAccount())
                .nickname(user.getNickname())
                .roles(normalizedRoleCodes)
                .build();
    }

    private List<String> normalizeRoleCodes(List<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            return List.of("ROLE_JOBSEEKER");
        }
        List<String> roleCodes = requestedRoles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
        return roleCodes.isEmpty() ? List.of("ROLE_JOBSEEKER") : roleCodes;
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return null;
        }
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException(4001, "邮箱格式不正确");
        }
        return normalizedEmail;
    }

    private SysUser requireUserForPasswordReset(String rawAccount, String rawEmail) {
        String account = rawAccount == null ? "" : rawAccount.trim();
        if (account.isEmpty()) {
            throw new BusinessException(4001, "请输入账号");
        }

        SysUser user = sysUserMapper.selectByAccount(account);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(4004, "未找到可找回的账号");
        }

        String normalizedEmail = normalizeEmail(rawEmail);
        if (normalizedEmail == null) {
            throw new BusinessException(4001, "请填写邮箱地址");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException(4001, "该账号未绑定邮箱，暂不支持在线找回");
        }
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(4001, "账号与邮箱不匹配");
        }
        return user;
    }
}
