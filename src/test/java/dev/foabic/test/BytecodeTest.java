package dev.foabic.test;

import dev.foabic.loader.BytecodeInjector;
import dev.foabic.test.fixture.Hook;
import dev.foabic.test.fixture.Target;

import java.io.IOException;
import java.io.InputStream;
import java.lang.constant.ClassDesc;
import java.util.List;

final class BytecodeTest {

    private BytecodeTest() {
    }

    static void run() {
        byte[] original = read(Target.class);
        byte[] transformed = BytecodeInjector.inject(
                original,
                ClassDesc.of("dev.foabic.test.fixture.Hook"),
                List.of(new BytecodeInjector.Injection("run", 0, "enter", "exit")));

        Hook.reset();
        Class<?> type = new TestLoader(BytecodeTest.class.getClassLoader())
                .define("dev.foabic.test.fixture.Target", transformed);
        try {
            Object target = type.getConstructor().newInstance();
            Object result = type.getMethod("run", int.class).invoke(target, 41);
            Assertions.assertEquals(42, result, "method result preserved");
            Assertions.assertEquals(1, Hook.ENTRIES.get(), "entry hook executed");
            Assertions.assertEquals(1, Hook.EXITS.get(), "return hook executed");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("transformed class failed", e);
        }

        byte[] branching = BytecodeInjector.inject(
                read(Target.class),
                ClassDesc.of("dev.foabic.test.fixture.Hook"),
                List.of(new BytecodeInjector.Injection("branch", 0, null, "exit")));
        Hook.reset();
        Class<?> branchType = new TestLoader(BytecodeTest.class.getClassLoader())
                .define("dev.foabic.test.fixture.Target", branching);
        try {
            Object target = branchType.getConstructor().newInstance();
            Object positive = branchType.getMethod("branch", int.class).invoke(target, 1);
            Object negative = branchType.getMethod("branch", int.class).invoke(target, -1);
            Assertions.assertEquals(1, positive, "positive branch result");
            Assertions.assertEquals(-1, negative, "negative branch result");
            Assertions.assertEquals(2, Hook.EXITS.get(), "each return path is hooked");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("branching transformed class failed", e);
        }
    }

    private static byte[] read(Class<?> type) {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            if (in == null) {
                throw new AssertionError("class resource not found: " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("cannot read class resource", e);
        }
    }

    private static final class TestLoader extends ClassLoader {
        private TestLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
