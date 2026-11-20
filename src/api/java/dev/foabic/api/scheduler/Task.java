package dev.foabic.api.scheduler;

/**
 * A handle to a scheduled task.
 */
public interface Task {

    long id();

    boolean cancelled();

    void cancel();
}
