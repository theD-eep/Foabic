package dev.foabic.test.fixture;

public class Target {

    public int value = 1;

    public int run(int input) {
        return input + value;
    }

    public int branch(int input) {
        if (input > 0) {
            return 1;
        }
        return -1;
    }

    public void noop() {
        value++;
    }
}
