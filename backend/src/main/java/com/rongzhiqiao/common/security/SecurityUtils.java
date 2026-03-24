package com.rongzhiqiao.common.security;

import com.rongzhiqiao.common.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(4003, "未登录");
        }
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException exception) {
            throw new BusinessException(4003, "登录态无效");
        }
    }
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    public static void requireAuthority(String authority) {
        if (!hasAuthority(authority)) {
            throw new BusinessException(4003, "forbidden");
        }
    }

    public static Set<String> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (grantedAuthority != null && grantedAuthority.getAuthority() != null) {
                authorities.add(grantedAuthority.getAuthority());
            }
        }
        return Set.copyOf(authorities);
    }
}
