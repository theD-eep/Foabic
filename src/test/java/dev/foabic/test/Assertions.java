package dev.foabic.test;

import java.util.Objects;

final class Assertions {

    private Assertions() {
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError(message + " (unexpectedly equal to " + unexpected + ")");
        }
    }

    static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return;
            }
            throw new AssertionError(message + " (threw " + failure.getClass().getName() + ")", failure);
        }
        throw new AssertionError(message + " (nothing was thrown)");
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
