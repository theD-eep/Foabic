package dev.foabic.loader.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** AND-only version ranges: *, exact, >=, >, <=, <, ^ and ~. */
public final class VersionRange implements Predicate<Version> {

    public static final VersionRange ANY = new VersionRange("*", List.of());

    private final String expression;
    private final List<Predicate<Version>> constraints;

    private VersionRange(String expression, List<Predicate<Version>> constraints) {
        this.expression = expression;
        this.constraints = constraints;
    }

    public static VersionRange parse(String text) {
        Objects.requireNonNull(text, "text");
        String value = text.trim();
        if (value.isEmpty() || value.equals("*")) return ANY;
        List<Predicate<Version>> constraints = new ArrayList<>();
        for (String token : value.split("\\s+")) {
            if (!token.isEmpty()) addConstraint(token, constraints);
        }
        return constraints.isEmpty() ? ANY : new VersionRange(value, List.copyOf(constraints));
    }

    private static void addConstraint(String token, List<Predicate<Version>> out) {
        if (token.startsWith("^")) {
            Version lower = Version.parse(token.substring(1));
            Version upper = lower.major() > 0
                    ? new Version(lower.major() + 1, 0, 0, null)
                    : lower.minor() > 0
                    ? new Version(0, lower.minor() + 1, 0, null)
                    : new Version(0, 0, lower.patch() + 1, null);
            out.add(v -> v.compareTo(lower) >= 0 && v.compareTo(upper) < 0);
            return;
        }
        if (token.startsWith("~")) {
            Version lower = Version.parse(token.substring(1));
            int parts = countCoreParts(token.substring(1));
            Version upper = parts >= 2
                    ? new Version(lower.major(), lower.minor() + 1, 0, null)
                    : new Version(lower.major() + 1, 0, 0, null);
            out.add(v -> v.compareTo(lower) >= 0 && v.compareTo(upper) < 0);
            return;
        }
        if (token.startsWith(">=")) {
            Version bound = Version.parse(token.substring(2));
            out.add(v -> v.compareTo(bound) >= 0);
            return;
        }
        if (token.startsWith("<=")) {
            Version bound = Version.parse(token.substring(2));
            out.add(v -> v.compareTo(bound) <= 0);
            return;
        }
        if (token.startsWith(">")) {
            Version bound = Version.parse(token.substring(1));
            out.add(v -> v.compareTo(bound) > 0);
            return;
        }
        if (token.startsWith("<")) {
            Version bound = Version.parse(token.substring(1));
            out.add(v -> v.compareTo(bound) < 0);
            return;
        }
        if (token.startsWith("=")) {
            Version exact = Version.parse(token.substring(1));
            out.add(v -> v.compareTo(exact) == 0);
            return;
        }
        Version exact = Version.parse(token);
        out.add(v -> v.compareTo(exact) == 0);
    }

    private static int countCoreParts(String text) {
        int end = text.length();
        int dash = text.indexOf('-');
        if (dash >= 0) end = dash;
        int plus = text.indexOf('+');
        if (plus >= 0 && plus < end) end = plus;
        String core = text.substring(0, end);
        if (core.isEmpty()) return 0;
        int parts = 1;
        for (int i = 0; i < core.length(); i++) {
            if (core.charAt(i) == '.') parts++;
        }
        return parts;
    }

    @Override
    public boolean test(Version version) {
        Objects.requireNonNull(version, "version");
        for (Predicate<Version> constraint : constraints) {
            if (!constraint.test(version)) return false;
        }
        return true;
    }

    public String expression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }
}
