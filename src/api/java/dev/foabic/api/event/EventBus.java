package dev.foabic.api.event;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A tiny synchronous, type-keyed event bus.
 *
 * <p>Listeners are invoked on the thread that posts the event. There is no
 * reflection, no annotation scanning and no classpath walking. A listener that
 * throws does not prevent other listeners from running; the exception is
 * passed to the configured error handler and never reaches the game loop.
 *
 * <p>Dispatch is exact-type: subscribing to {@code ServerTickEvent} does not
 * receive subclasses. Event classes are final records, so this is not a
 * limitation in practice.
 */
public final class EventBus {

    private static final System.Logger LOG = System.getLogger("Foabic");

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Listener<?>>> listeners =
            new ConcurrentHashMap<>();
    private final Consumer<Throwable> errorHandler;

    public EventBus() {
        this(error -> LOG.log(System.Logger.Level.ERROR,
                "Uncaught exception in event listener", error));
    }

    public EventBus(Consumer<Throwable> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public <E> EventSubscription subscribe(Class<E> eventType, EventListener<? super E> listener) {
        return subscribe(eventType, EventPriority.NORMAL, listener);
    }

    public <E> EventSubscription subscribe(
            Class<E> eventType,
            EventPriority priority,
            EventListener<? super E> listener
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");

        CopyOnWriteArrayList<Listener<?>> list =
                listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>());
        Listener<E> entry = new Listener<>(priority, listener);
        list.add(entry);
        list.sort(Comparator.comparingInt(value -> value.priority.ordinal()));
        return new Subscription(eventType, entry);
    }

    /**
     * Dispatches an event and returns the same instance for chaining.
     */
    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        Objects.requireNonNull(event, "event");
        CopyOnWriteArrayList<Listener<?>> list = listeners.get(event.getClass());
        if (list == null) {
            return event;
        }

        for (Listener<?> entry : list) {
            if (!entry.active) {
                continue;
            }
            try {
                ((EventListener<Object>) entry.action).onEvent(event);
            } catch (Throwable failure) {
                report(new ListenerFailure(event.getClass(), entry.action.getClass(), failure));
            }
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                break;
            }
        }
        return event;
    }

    public boolean hasListeners(Class<?> eventType) {
        CopyOnWriteArrayList<Listener<?>> list = listeners.get(eventType);
        return list != null && !list.isEmpty();
    }

    public void clear() {
        for (CopyOnWriteArrayList<Listener<?>> list : listeners.values()) {
            for (Listener<?> entry : list) {
                entry.active = false;
            }
        }
        listeners.clear();
    }

    private void report(Throwable failure) {
        try {
            errorHandler.accept(failure);
        } catch (Throwable ignored) {
            LOG.log(System.Logger.Level.ERROR, "Event error handler failed", ignored);
        }
    }

    private final class Subscription implements EventSubscription {
        private final Class<?> eventType;
        private final Listener<?> entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Subscription(Class<?> eventType, Listener<?> entry) {
            this.eventType = eventType;
            this.entry = entry;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            entry.active = false;
            CopyOnWriteArrayList<Listener<?>> list = listeners.get(eventType);
            if (list != null) {
                list.remove(entry);
                if (list.isEmpty()) {
                    listeners.remove(eventType, list);
                }
            }
        }
    }

    private static final class Listener<E> {
        private final EventPriority priority;
        private final EventListener<? super E> action;
        private volatile boolean active = true;

        private Listener(EventPriority priority, EventListener<? super E> action) {
            this.priority = priority;
            this.action = action;
        }
    }

    /**
     * Diagnostic wrapper passed to the error handler.
     */
    public static final class ListenerFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final Class<?> eventType;
        private final Class<?> listenerType;

        private ListenerFailure(Class<?> eventType, Class<?> listenerType, Throwable cause) {
            super("event " + eventType.getName() + " failed in listener " + listenerType.getName(), cause);
            this.eventType = eventType;
            this.listenerType = listenerType;
        }

        public Class<?> eventType() {
            return eventType;
        }

        public Class<?> listenerType() {
            return listenerType;
        }
    }
}
