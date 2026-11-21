package dev.foabic.api.event;

@FunctionalInterface
public interface EventListener<E> {
    void onEvent(E event);
}
