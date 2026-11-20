package dev.foabic.test;

public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) {
        // Keep the test transcript readable; tests deliberately exercise failure paths.
        java.util.logging.Logger.getLogger("Foabic").setLevel(java.util.logging.Level.OFF);

        int failures = 0;
        failures += run("versions", VersionTest::run);
        failures += run("events", EventBusTest::run);
        failures += run("scheduler", SchedulerTest::run);
        failures += run("commands", CommandTest::run);
        failures += run("bytecode", BytecodeTest::run);
        failures += run("loader", LoaderTest::run);

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST GROUP(S) FAILED");
            System.exit(1);
        }
    }

    private static int run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
            return 0;
        } catch (Throwable failure) {
            System.out.println("FAIL " + name);
            failure.printStackTrace(System.out);
            return 1;
        }
    }
}
