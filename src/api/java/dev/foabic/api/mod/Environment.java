package dev.foabic.api.mod;

import java.util.Locale;

/**
 * The side on which Foabic is running, or the sides a mod declares support for.
 */
public enum Environment {
    CLIENT,
    SERVER,
    /** A mod that declares no side restriction. Never returned by the running loader. */
    UNIVERSAL;

    public static Environment parse(String value) {
        if (value == null || value.isBlank() || value.equals("*")) {
            return UNIVERSAL;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            case "universal", "both" -> UNIVERSAL;
            default -> throw new IllegalArgumentException("unknown environment: " + value);
        };
    }
}
