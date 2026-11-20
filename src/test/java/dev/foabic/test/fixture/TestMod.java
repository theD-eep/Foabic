package dev.foabic.test.fixture;

import dev.foabic.api.event.ServerStartedEvent;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestMod implements ModInitializer {

    public static final List<String> ORDER = new ArrayList<>();
    public static final AtomicInteger STARTED = new AtomicInteger();
    public static volatile boolean initialized;
    public static volatile String resourceText;
    public static volatile ModContext context;

    public static void reset() {
        ORDER.clear();
        STARTED.set(0);
        initialized = false;
        resourceText = null;
        context = null;
    }

    @Override
    public void onInitialize(ModContext context) {
        initialized = true;
        TestMod.context = context;
        ORDER.add("testmod");
        try {
            resourceText = new String(
                    context.mod().resources().read("data/hello.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("missing test resource", e);
        }
        context.events().subscribe(ServerStartedEvent.class, event -> STARTED.incrementAndGet());
        context.commands().literal("testmod").executes(command -> 1);
    }
}
