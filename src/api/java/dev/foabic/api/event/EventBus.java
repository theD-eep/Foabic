package dev.foabic.api.event;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A small synchronous, exact-type event bus. No reflection or annotation
 * scanning; listeners run on the thread that posts the event.
 *
 * <p>{@link #clear()} removes every listener and is called by the platform
 * adapters on server stop. A cleared subscription releases its listener even
 * if the mod still holds the handle.
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

    public <E> FoabicSubscription subscribe(Class<E> type, Consumer<? super E> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Listener<?>> list =
                listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>());
        Listener<E> entry = new Listener<>(listener);
        list.add(entry);
        return new Subscription(type, entry);
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        Objects.requireNonNull(event, "event");
        CopyOnWriteArrayList<Listener<?>> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (Listener<?> entry : list) {
            Consumer<Object> action = (Consumer<Object>) entry.action;
            if (action == null) continue;
            try {
                action.accept(event);
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

    /** Removes every listener and releases their callbacks. */
    public void clear() {
        for (CopyOnWriteArrayList<Listener<?>> list : listeners.values()) {
            for (Listener<?> entry : list) entry.action = null;
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

    private final class Subscription implements FoabicSubscription {
        private final Class<?> type;
        private final Listener<?> entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Subscription(Class<?> type, Listener<?> entry) {
            this.type = type;
            this.entry = entry;
        }

        @Override
        public void unsubscribe() {
            if (!closed.compareAndSet(false, true)) return;
            entry.action = null;
            CopyOnWriteArrayList<Listener<?>> list = listeners.get(type);
            if (list != null) {
                list.remove(entry);
                if (list.isEmpty()) listeners.remove(type, list);
            }
        }
    }

    private static final class Listener<E> {
        private volatile Consumer<? super E> action;

        private Listener(Consumer<? super E> action) {
            this.action = action;
        }
    }
}
