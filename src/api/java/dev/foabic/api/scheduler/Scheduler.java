package dev.foabic.api.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Tick-based tasks. All actions run on the server thread when
 * {@link #tick(long)} is called by the platform adapter.
 *
 * <p>There is no wall-clock timer, no thread pool and no background thread.
 * A delay of zero means "next tick", not "immediately". Repeating tasks use a
 * fixed period and never try to catch up after a long tick.
 */
public final class Scheduler {

    private static final System.Logger LOG = System.getLogger("Foabic");

    private final Object lock = new Object();
    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();
    private final AtomicLong ids = new AtomicLong();
    private long tick;

    public Task schedule(long delayTicks, Runnable action) {
        Objects.requireNonNull(action, "action");
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks must be >= 0");
        }
        synchronized (lock) {
            ScheduledTask task = new ScheduledTask(ids.incrementAndGet(), tick + delayTicks, 0, action, null);
            queue.add(task);
            return task;
        }
    }

    public Task execute(Runnable action) {
        return schedule(0, action);
    }

    public Task scheduleRepeating(long initialDelayTicks, long periodTicks, Consumer<Task> action) {
        Objects.requireNonNull(action, "action");
        if (initialDelayTicks < 0) {
            throw new IllegalArgumentException("initialDelayTicks must be >= 0");
        }
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("periodTicks must be > 0");
        }
        synchronized (lock) {
            ScheduledTask task = new ScheduledTask(
                    ids.incrementAndGet(), tick + initialDelayTicks, periodTicks, null, action);
            queue.add(task);
            return task;
        }
    }

    /**
     * Advances the scheduler clock and runs every task whose due tick has
     * arrived. Called by the platform adapter at the start of a server tick.
     */
    public void tick(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("currentTick must be >= 0");
        }

        List<ScheduledTask> due = new ArrayList<>();
        synchronized (lock) {
            tick = currentTick;
            while (!queue.isEmpty() && queue.peek().dueTick <= currentTick) {
                due.add(queue.poll());
            }
        }

        for (ScheduledTask task : due) {
            if (task.cancelled()) {
                continue;
            }
            try {
                task.run();
            } catch (Throwable failure) {
                task.cancel();
                LOG.log(System.Logger.Level.ERROR,
                        "Scheduled task " + task.id() + " failed and was cancelled", failure);
            }

            if (!task.cancelled() && task.periodTicks > 0) {
                synchronized (lock) {
                    task.dueTick = currentTick + task.periodTicks;
                    queue.add(task);
                }
            }
        }
    }

    public long tick() {
        synchronized (lock) {
            return tick;
        }
    }

    public int pendingTasks() {
        synchronized (lock) {
            return queue.size();
        }
    }

    public void cancelAll() {
        synchronized (lock) {
            for (ScheduledTask task : queue) {
                task.cancel();
            }
            queue.clear();
        }
    }

    private final class ScheduledTask implements Task, Comparable<ScheduledTask> {
        private final long id;
        private final long periodTicks;
        private final Runnable once;
        private final Consumer<Task> repeating;
        private long dueTick;
        private volatile boolean cancelled;

        private ScheduledTask(long id, long dueTick, long periodTicks,
                              Runnable once, Consumer<Task> repeating) {
            this.id = id;
            this.dueTick = dueTick;
            this.periodTicks = periodTicks;
            this.once = once;
            this.repeating = repeating;
        }

        private void run() {
            if (once != null) {
                once.run();
            } else {
                repeating.accept(this);
            }
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int compareTo(ScheduledTask other) {
            int result = Long.compare(dueTick, other.dueTick);
            return result != 0 ? result : Long.compare(id, other.id);
        }
    }
}
