package dev.foabic.test;

import dev.foabic.api.command.ArgumentType;
import dev.foabic.api.command.CommandBuilder;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventPriority;
import dev.foabic.api.event.EventSubscription;
import dev.foabic.api.scheduler.Scheduler;
import dev.foabic.api.scheduler.Task;
import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;
import dev.foabic.loader.command.CommandRegistryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class AllTests {

    public static void main(String[] args) {
        java.util.logging.Logger.getLogger("Foabic").setLevel(java.util.logging.Level.OFF);
        run("versions", AllTests::versions);
        run("events", AllTests::events);
        run("scheduler", AllTests::scheduler);
        run("commands", AllTests::commands);
        System.out.println("ALL TESTS PASSED");
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            System.out.println("FAIL " + name);
            failure.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void versions() {
        check(Version.parse("1.2").toString().equals("1.2.0"), "short version");
        check(Version.parse("1.0.0").compareTo(Version.parse("1.0.0-rc.1")) > 0, "release ordering");
        check(VersionRange.parse("^1.2.3").test(Version.parse("1.9.9")), "caret inclusive");
        check(!VersionRange.parse("^1.2.3").test(Version.parse("2.0.0")), "caret upper bound");
        check(VersionRange.parse(">=1.0 <2.0").test(Version.parse("1.5.0")), "compound range");
    }

    private static void events() {
        EventBus bus = new EventBus(error -> {
        });
        List<String> order = new ArrayList<>();
        bus.subscribe(TestEvent.class, EventPriority.LOW, event -> order.add("low"));
        bus.subscribe(TestEvent.class, EventPriority.HIGHEST, event -> order.add("high"));
        bus.post(new TestEvent("x"));
        check(order.equals(List.of("high", "low")), "priority order");

        AtomicInteger calls = new AtomicInteger();
        EventSubscription subscription = bus.subscribe(TestEvent.class, event -> calls.incrementAndGet());
        bus.post(new TestEvent("a"));
        subscription.close();
        bus.post(new TestEvent("b"));
        check(calls.get() == 1, "unsubscribe");
    }

    private static void scheduler() {
        Scheduler scheduler = new Scheduler();
        List<String> ran = new ArrayList<>();
        scheduler.schedule(2, () -> ran.add("later"));
        scheduler.schedule(0, () -> ran.add("next"));
        scheduler.tick(1);
        check(ran.equals(List.of("next")), "next tick");
        scheduler.tick(2);
        check(ran.equals(List.of("next", "later")), "delay");
        List<Integer> repeating = new ArrayList<>();
        Task task = scheduler.scheduleRepeating(1, 2, self -> repeating.add((int) scheduler.tick()));
        scheduler.tick(3);
        scheduler.tick(4);
        scheduler.tick(5);
        task.cancel();
        scheduler.tick(6);
        check(repeating.equals(List.of(3, 5)), "repeating task");
    }

    private static void commands() {
        CommandRegistryImpl registry = new CommandRegistryImpl();
        CommandBuilder root = registry.literal("hello");
        CommandBuilder arg = root.argument("name", ArgumentType.STRING);
        arg.executes(context -> 1);
        check(registry.roots().size() == 1, "root");
        check(arg.argumentType() == ArgumentType.STRING, "argument");
        check(root.children().size() == 1, "child");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record TestEvent(String value) {
    }
}
