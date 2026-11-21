package dev.foabic.api.mod;

import java.util.Locale;

public enum Environment {
    CLIENT,
    SERVER,
    UNIVERSAL;

    public static Environment parse(String value) {
        if (value == null || value.isBlank() || value.equals("*")) return UNIVERSAL;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            case "universal", "both" -> UNIVERSAL;
            default -> throw new IllegalArgumentException("unknown environment: " + value);
        };
    }
}
