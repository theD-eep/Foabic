package dev.foabic.test;

import dev.foabic.api.event.CancellableEvent;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventPriority;
import dev.foabic.api.event.EventSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class EventBusTest {

    private EventBusTest() {
    }

    static void run() {
        priorities();
        cancellation();
        isolation();
        unsubscribe();
    }

    private static void priorities() {
        EventBus bus = new EventBus(error -> {
        });
        List<String> order = new ArrayList<>();
        bus.subscribe(TestEvent.class, EventPriority.LOW, event -> order.add("low"));
        bus.subscribe(TestEvent.class, EventPriority.HIGHEST, event -> order.add("highest"));
        bus.subscribe(TestEvent.class, EventPriority.NORMAL, event -> order.add("normal"));
        bus.post(new TestEvent("x"));
        Assertions.assertEquals(List.of("highest", "normal", "low"), order, "priority order");
    }

    private static void cancellation() {
        EventBus bus = new EventBus(error -> {
        });
        AtomicInteger later = new AtomicInteger();
        bus.subscribe(CancelEvent.class, EventPriority.HIGH, CancelEvent::cancel);
        bus.subscribe(CancelEvent.class, EventPriority.LOW, event -> later.incrementAndGet());
        CancelEvent event = new CancelEvent();
        bus.post(event);
        Assertions.assertTrue(event.isCancelled(), "event cancelled");
        Assertions.assertEquals(0, later.get(), "later listener skipped");
    }

    private static void isolation() {
        AtomicInteger errors = new AtomicInteger();
        EventBus bus = new EventBus(error -> errors.incrementAndGet());
        AtomicInteger later = new AtomicInteger();
        bus.subscribe(TestEvent.class, event -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe(TestEvent.class, event -> later.incrementAndGet());
        bus.post(new TestEvent("x"));
        Assertions.assertEquals(1, errors.get(), "error handler called");
        Assertions.assertEquals(1, later.get(), "later listener still runs");
    }

    private static void unsubscribe() {
        EventBus bus = new EventBus(error -> {
        });
        AtomicInteger calls = new AtomicInteger();
        EventSubscription subscription = bus.subscribe(TestEvent.class, event -> calls.incrementAndGet());
        bus.post(new TestEvent("a"));
        subscription.close();
        subscription.close();
        bus.post(new TestEvent("b"));
        Assertions.assertEquals(1, calls.get(), "unsubscribed listener not called");
        Assertions.assertFalse(bus.hasListeners(TestEvent.class), "no listeners left");
    }

    private record TestEvent(String value) {
    }

    private static final class CancelEvent extends CancellableEvent {
    }
}
