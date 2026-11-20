package dev.foabic.api.event;

/**
 * Listener order. {@link #HIGHEST} runs first, {@link #LOWEST} last.
 */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
