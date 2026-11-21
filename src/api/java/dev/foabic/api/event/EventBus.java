package dev.foabic.api.event;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A small synchronous, exact-type event bus. No reflection or annotation
 * scanning; listeners run on the thread that posts the event.
 */
public final class EventBus {

    private static final System.Logger LOG = System.getLogger("Foabic");

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Listener<?>>> listeners =
            new ConcurrentHashMap<>();
    private final Consumer<Throwable> errorHandler;

    public EventBus() {
        this(error -> LOG.log(System.Logger.Level.ERROR, "event listener failed", error));
    }

    public EventBus(Consumer<Throwable> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public <E> EventSubscription subscribe(Class<E> type, EventListener<? super E> listener) {
        return subscribe(type, EventPriority.NORMAL, listener);
    }

    public <E> EventSubscription subscribe(Class<E> type, EventPriority priority,
                                           EventListener<? super E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Listener<?>> list =
                listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>());
        Listener<E> entry = new Listener<>(priority, listener);
        list.add(entry);
        list.sort(Comparator.comparingInt(value -> value.priority.ordinal()));
        return new Subscription(type, entry);
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        Objects.requireNonNull(event, "event");
        CopyOnWriteArrayList<Listener<?>> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (Listener<?> entry : list) {
            if (!entry.active) continue;
            try {
                ((EventListener<Object>) entry.action).onEvent(event);
            } catch (Throwable failure) {
                report(failure);
            }
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) break;
        }
        return event;
    }

    public boolean hasListeners(Class<?> type) {
        CopyOnWriteArrayList<Listener<?>> list = listeners.get(type);
        return list != null && !list.isEmpty();
    }

    public void clear() {
        for (CopyOnWriteArrayList<Listener<?>> list : listeners.values()) {
            for (Listener<?> entry : list) entry.active = false;
        }
        listeners.clear();
    }

    private void report(Throwable failure) {
        try {
            errorHandler.accept(failure);
        } catch (Throwable ignored) {
            LOG.log(System.Logger.Level.ERROR, "event error handler failed", ignored);
        }
    }

    private final class Subscription implements EventSubscription {
        private final Class<?> type;
        private final Listener<?> entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Subscription(Class<?> type, Listener<?> entry) {
            this.type = type;
            this.entry = entry;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) return;
            entry.active = false;
            CopyOnWriteArrayList<Listener<?>> list = listeners.get(type);
            if (list != null) {
                list.remove(entry);
                if (list.isEmpty()) listeners.remove(type, list);
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
}
