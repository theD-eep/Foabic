package dev.foabic.test.fixture;

import java.util.concurrent.atomic.AtomicInteger;

public final class Hook {

    public static final AtomicInteger ENTRIES = new AtomicInteger();
    public static final AtomicInteger EXITS = new AtomicInteger();

    private Hook() {
    }

    public static void enter(Object receiver) {
        ENTRIES.incrementAndGet();
    }

    public static void exit(Object receiver) {
        EXITS.incrementAndGet();
    }

    public static void reset() {
        ENTRIES.set(0);
        EXITS.set(0);
    }
}
