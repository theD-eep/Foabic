package dev.foabic.api.scheduler;

public interface Task {
    long id();
    boolean cancelled();
    void cancel();
}
