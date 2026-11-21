package dev.foabic.test;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventPriority;
import dev.foabic.api.event.EventSubscription;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Small platform-neutral tests, run by build.ps1. */
public final class AllTests {

    private static int passed;

    private AllTests() {
    }

    public static void main(String[] args) {
        testEventOrderAndClose();
        testListenerFailureIsolation();
        testVersionAndRange();
        testUseBlockSignal();
        System.out.println("ALL TESTS PASSED (" + passed + ")");
    }

    private static void testEventOrderAndClose() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.subscribe(String.class, EventPriority.LOW, event -> order.add("low"));
        EventSubscription normal = bus.subscribe(String.class, event -> order.add("normal"));
        bus.subscribe(String.class, EventPriority.HIGH, event -> order.add("high"));

        bus.post("first");
        check(order.equals(List.of("high", "normal", "low")), "priority order: " + order);

        normal.close();
        order.clear();
        bus.post("second");
        check(order.equals(List.of("high", "low")), "closed listener ran: " + order);
        pass("events");
    }

    private static void testListenerFailureIsolation() {
        AtomicInteger errors = new AtomicInteger();
        EventBus bus = new EventBus(error -> errors.incrementAndGet());
        List<String> seen = new ArrayList<>();
        bus.subscribe(String.class, event -> {
            throw new IllegalStateException("expected test failure");
        });
        bus.subscribe(String.class, event -> seen.add(event));

        bus.post("ok");
        check(errors.get() == 1, "error handler count " + errors.get());
        check(seen.equals(List.of("ok")), "later listener did not run: " + seen);
        pass("failure isolation");
    }

    private static void testVersionAndRange() {
        Version version = Version.parse("1.0.0");
        check(version.equals(Version.parse("1.0.0")), "version equality");
        check(Version.parse("1.0.1").compareTo(version) > 0, "version ordering");
        check(VersionRange.parse("^1.0.0").test(version), "^1.0.0 should match 1.0.0");
        check(!VersionRange.parse("^2.0.0").test(version), "^2.0.0 should not match 1.0.0");
        check(VersionRange.parse(">=1.0.0 <2.0.0").test(Version.parse("1.9.9")),
                "range endpoint");
        check(!VersionRange.parse(">=1.0.0 <2.0.0").test(Version.parse("2.0.0")),
                "exclusive upper endpoint");
        pass("versions");
    }

    private static void testUseBlockSignal() {
        UseBlockSignal signal = new UseBlockSignal(
                "Tester",
                "minecraft:overworld",
                ResourceLocation.of("foabic_test", "test_block"),
                new BlockPos(1, 2, 3),
                UseBlockSignal.Hand.MAIN_HAND);
        check(signal.result() == UseBlockSignal.Result.PASS, "default result");
        signal.setResult(UseBlockSignal.Result.SUCCESS);
        check(signal.result() == UseBlockSignal.Result.SUCCESS, "mutable result");
        check(signal.pos().equals(new BlockPos(1, 2, 3)), "position");
        pass("use-block signal");
    }

    private static void pass(String name) {
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
