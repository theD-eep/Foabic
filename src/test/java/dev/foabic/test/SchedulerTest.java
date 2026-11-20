package dev.foabic.test;

import dev.foabic.api.scheduler.Scheduler;
import dev.foabic.api.scheduler.Task;

import java.util.ArrayList;
import java.util.List;

final class SchedulerTest {

    private SchedulerTest() {
    }

    static void run() {
        Scheduler scheduler = new Scheduler();
        List<String> ran = new ArrayList<>();

        scheduler.schedule(2, () -> ran.add("delayed"));
        scheduler.schedule(0, () -> ran.add("next-tick"));
        scheduler.tick(1);
        Assertions.assertEquals(List.of("next-tick"), ran, "zero delay runs on the next tick");

        scheduler.tick(2);
        Assertions.assertEquals(List.of("next-tick", "delayed"), ran, "two-tick delay runs on tick two");

        scheduler.tick(3);
        Assertions.assertEquals(List.of("next-tick", "delayed"), ran, "one-shot task does not repeat");

        List<Integer> repeating = new ArrayList<>();
        Task task = scheduler.scheduleRepeating(1, 2, self -> repeating.add((int) scheduler.tick()));
        scheduler.tick(4);
        scheduler.tick(5);
        scheduler.tick(6);
        Assertions.assertEquals(List.of(4, 6), repeating, "repeating task period");
        task.cancel();
        scheduler.tick(8);
        Assertions.assertEquals(List.of(4, 6), repeating, "cancelled repeating task stops");

        Task failing = scheduler.schedule(0, () -> {
            throw new IllegalStateException("expected");
        });
        scheduler.tick(9);
        Assertions.assertTrue(failing.cancelled(), "failing task is cancelled");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> scheduler.schedule(-1, () -> {
                }), "negative delay rejected");
    }
}
