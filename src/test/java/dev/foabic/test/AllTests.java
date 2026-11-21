package dev.foabic.test;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventPriority;
import dev.foabic.api.event.EventSubscription;
import dev.foabic.api.signal.PlayerBreakBlockSignal;
import dev.foabic.api.signal.PlayerInteractBlockSignal;
import dev.foabic.api.signal.ServerTickSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;
import dev.foabic.loader.LoaderDefensiveTests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Small platform-neutral tests, run by the Gradle {@code :coreTest} task. */
public final class AllTests {

    private static int passed;

    private AllTests() {
    }

    public static void main(String[] args) {
        testEventOrderAndClose();
        testListenerFailureIsolation();
        testVersionAndRange();
        testUseBlockSignal();
        testPlayerInteractBlockSignal();
        testPlayerBreakBlockSignal();
        testServerTickSignal();
        LoaderDefensiveTests.run();
        pass("loader defenses");
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
        Version version = Version.parse("1.1.0");
        check(version.equals(Version.parse("1.1.0")), "version equality");
        check(Version.parse("1.1.1").compareTo(version) > 0, "version ordering");
        check(VersionRange.parse("^1.0.0").test(version), "^1.0.0 should match 1.1.0");
        check(!VersionRange.parse("^2.0.0").test(version), "^2.0.0 should not match 1.1.0");
        check(VersionRange.parse(">=1.0.0 <2.0.0").test(Version.parse("1.9.9")),
                "range endpoint");
        check(!VersionRange.parse(">=1.0.0 <2.0.0").test(Version.parse("2.0.0")),
                "exclusive upper endpoint");
        pass("versions");
    }

    @SuppressWarnings("deprecation")
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
        pass("legacy use-block signal");
    }

    private static void testPlayerInteractBlockSignal() {
        PlayerInteractBlockSignal signal = new PlayerInteractBlockSignal(
                "Tester",
                "minecraft:overworld",
                ResourceLocation.of("foabic_test", "test_block"),
                new BlockPos(4, 5, 6),
                PlayerInteractBlockSignal.Hand.OFF_HAND);
        check(signal.result() == PlayerInteractBlockSignal.Result.PASS, "default interact result");
        signal.setResult(PlayerInteractBlockSignal.Result.CONSUME);
        check(signal.result() == PlayerInteractBlockSignal.Result.CONSUME, "mutable interact result");
        check(signal.hand() == PlayerInteractBlockSignal.Hand.OFF_HAND, "hand");
        check(signal.pos().equals(new BlockPos(4, 5, 6)), "interact position");
        pass("player-interact-block signal");
    }

    private static void testPlayerBreakBlockSignal() {
        PlayerBreakBlockSignal signal = new PlayerBreakBlockSignal(
                "Tester",
                "minecraft:overworld",
                ResourceLocation.of("foabic_test", "test_block"),
                new BlockPos(7, 8, 9));
        check(!signal.isCancelled(), "break starts uncancelled");
        signal.cancel();
        check(signal.isCancelled(), "break cancellation");
        pass("player-break-block signal");
    }

    private static void testServerTickSignal() {
        ServerTickSignal signal = new ServerTickSignal("fabric", 20L, 2);
        check(signal.tick() == 20L, "tick");
        check(signal.playerCount() == 2, "player count");
        check(signal.platform().equals("fabric"), "platform");
        pass("server-tick signal");
    }

    private static void pass(String name) {
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
