package dev.foabic.test;

import dev.foabic.api.command.ArgumentType;
import dev.foabic.api.command.CommandBuilder;
import dev.foabic.loader.command.CommandRegistryImpl;

final class CommandTest {

    private CommandTest() {
    }

    static void run() {
        CommandRegistryImpl registry = new CommandRegistryImpl();
        CommandBuilder home = registry.literal("home");
        home.executes(context -> 1);
        CommandBuilder name = home.argument("name", ArgumentType.STRING);
        name.suggests(context -> java.util.List.of("base", "farm"));
        name.executes(context -> 1);

        Assertions.assertEquals(1, registry.roots().size(), "one root");
        Assertions.assertEquals("home", registry.roots().get(0).name(), "root name");
        Assertions.assertEquals(1, home.children().size(), "one child");
        Assertions.assertEquals(ArgumentType.STRING, name.argumentType(), "argument type");
        Assertions.assertTrue(name.children().isEmpty(), "leaf has no children");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> registry.literal("home"), "duplicate root");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> registry.literal("Bad Name"), "invalid root");
    }
}
