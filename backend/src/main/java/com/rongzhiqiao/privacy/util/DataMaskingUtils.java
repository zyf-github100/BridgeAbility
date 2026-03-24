package com.rongzhiqiao.privacy.util;

public final class DataMaskingUtils {

    private DataMaskingUtils() {
    }

    public static String maskName(String rawValue, String defaultValue) {
        String normalized = normalize(rawValue);
        if (normalized == null) {
            return defaultValue;
        }
        if (normalized.length() <= 1) {
            return normalized + "*";
        }
        return normalized.substring(0, 1) + "*".repeat(Math.min(3, normalized.length() - 1));
    }

    public static String maskPhone(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 7) {
            return normalized.substring(0, Math.min(2, normalized.length())) + "***";
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    public static String maskGovernmentId(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized.substring(0, Math.min(2, normalized.length())) + "****";
        }
        return normalized.substring(0, 4) + "****" + normalized.substring(normalized.length() - 4);
    }

    public static String maskAddress(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 6) {
            return normalized.substring(0, Math.min(3, normalized.length())) + "****";
        }
        return normalized.substring(0, 6) + "****";
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
