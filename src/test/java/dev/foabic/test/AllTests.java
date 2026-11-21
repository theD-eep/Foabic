package dev.foabic.test;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.FoabicSubscription;
import dev.foabic.api.network.FoabicPacket;
import dev.foabic.api.network.NetworkManager;
import dev.foabic.api.signal.PlayerBreakBlockSignal;
import dev.foabic.api.signal.PlayerInteractBlockSignal;
import dev.foabic.api.signal.ServerTickSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.loader.LoaderDefensiveTests;
import dev.foabic.loader.version.Version;
import dev.foabic.loader.version.VersionRange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        testNetworkRoundTrip();
        LoaderDefensiveTests.run();
        pass("loader defenses");
        System.out.println("ALL TESTS PASSED (" + passed + ")");
    }

    private static void testEventOrderAndClose() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.subscribe(String.class, event -> order.add("first"));
        FoabicSubscription second = bus.subscribe(String.class, event -> order.add("second"));
        bus.subscribe(String.class, event -> order.add("third"));

        bus.post("first");
        check(order.equals(List.of("first", "second", "third")), "subscription order: " + order);

        second.unsubscribe();
        second.unsubscribe();
        order.clear();
        bus.post("second");
        check(order.equals(List.of("first", "third")), "closed listener ran: " + order);
        check(bus.hasListeners(String.class), "other listeners removed");

        AtomicInteger afterClear = new AtomicInteger();
        bus.subscribe(String.class, event -> afterClear.incrementAndGet());
        bus.clear();
        bus.post("third");
        check(afterClear.get() == 0, "cleared listener ran");
        check(!bus.hasListeners(String.class), "clear left listeners");
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
        check(Version.parse("26.3.0.115").equals(Version.parse("26.3.0")),
                "four-part platform version");
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

    private static void testNetworkRoundTrip() {
        AtomicReference<byte[]> sent = new AtomicReference<>();
        NetworkManager.reset();
        NetworkManager.bind(new NetworkManager.Transport() {
            @Override
            public void sendToServer(String type, byte[] data) {
                sent.set(data);
            }

            @Override
            public void sendToClient(Object player, String type, byte[] data) {
                throw new AssertionError("not used");
            }
        });
        NetworkManager.register(TestPacket.class, TestPacket::new);
        NetworkManager.sendToServer(new TestPacket("hello"));
        check(sent.get() != null, "packet was not sent");

        NetworkManager.receive(TestPacket.class.getName(), sent.get());
        // The fake transport only records; receive decodes into the registered
        // factory instance and calls handle, which is checked by the class.
        check(TestPacket.lastHandled.equals("hello"), "packet decode/handle failed: " + TestPacket.lastHandled);
        NetworkManager.reset();
        pass("networking round trip");
    }

    public static final class TestPacket implements FoabicPacket {
        static volatile String lastHandled;
        private String value;

        public TestPacket() {
        }

        TestPacket(String value) {
            this.value = value;
        }

        @Override
        public byte[] encode() {
            return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public void decode(byte[] data) {
            value = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public void handle() {
            lastHandled = value;
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
