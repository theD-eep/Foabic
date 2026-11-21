package dev.foabic.loader.version;

import java.util.Objects;

/** A small, lenient Semantic Version value. */
public record Version(int major, int minor, int patch, String preRelease) implements Comparable<Version> {

    public Version {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("version components must be >= 0");
        }
        if (preRelease != null) {
            preRelease = preRelease.isBlank() ? null : preRelease;
            if (preRelease != null) {
                validatePrerelease(preRelease);
            }
        }
    }

    public static Version parse(String text) {
        Objects.requireNonNull(text, "text");
        String value = text.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty version");
        }
        int plus = value.indexOf('+');
        if (plus >= 0) {
            value = value.substring(0, plus);
        }
        String core = value;
        String pre = null;
        int dash = value.indexOf('-');
        if (dash >= 0) {
            core = value.substring(0, dash);
            pre = value.substring(dash + 1);
            if (pre.isEmpty()) {
                throw new IllegalArgumentException("empty prerelease in version: " + text);
            }
        }
        String[] parts = core.split("\\.", -1);
        if (parts.length < 1 || parts.length > 4) {
            throw new IllegalArgumentException("invalid version: " + text);
        }
        int[] numbers = new int[3];
        int numericParts = Math.min(parts.length, 3);
        for (int i = 0; i < numericParts; i++) {
            if (parts[i].isEmpty()) {
                throw new IllegalArgumentException("invalid version: " + text);
            }
            for (int c = 0; c < parts[i].length(); c++) {
                if (!Character.isDigit(parts[i].charAt(c))) {
                    throw new IllegalArgumentException("invalid version: " + text);
                }
            }
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("version component too large: " + text, e);
            }
        }
        return new Version(numbers[0], numbers[1], numbers[2], pre);
    }

    private static void validatePrerelease(String pre) {
        for (int i = 0; i < pre.length(); i++) {
            char c = pre.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '-';
            if (!ok) {
                throw new IllegalArgumentException("invalid prerelease character: " + c);
            }
        }
        if (pre.startsWith(".") || pre.endsWith(".") || pre.contains("..")) {
            throw new IllegalArgumentException("invalid prerelease: " + pre);
        }
    }

    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) return result;
        result = Integer.compare(minor, other.minor);
        if (result != 0) return result;
        result = Integer.compare(patch, other.patch);
        if (result != 0) return result;
        return comparePrerelease(preRelease, other.preRelease);
    }

    private static int comparePrerelease(String left, String right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        String[] a = left.split("\\.", -1);
        String[] b = right.split("\\.", -1);
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int result = compareIdentifier(a[i], b[i]);
            if (result != 0) return result;
        }
        return Integer.compare(a.length, b.length);
    }

    private static int compareIdentifier(String left, String right) {
        boolean ln = isNumeric(left);
        boolean rn = isNumeric(right);
        if (ln && rn) {
            String a = stripZeros(left);
            String b = stripZeros(right);
            int result = Integer.compare(a.length(), b.length());
            return result != 0 ? result : a.compareTo(b);
        }
        if (ln) return -1;
        if (rn) return 1;
        return left.compareTo(right);
    }

    private static boolean isNumeric(String value) {
        if (value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static String stripZeros(String value) {
        int i = 0;
        while (i < value.length() - 1 && value.charAt(i) == '0') i++;
        return value.substring(i);
    }

    @Override
    public String toString() {
        String base = major + "." + minor + "." + patch;
        return preRelease == null ? base : base + "-" + preRelease;
    }
}
