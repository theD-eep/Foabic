package dev.foabic.api.mod;

import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.config.Config;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.scheduler.Scheduler;

/**
 * The per-mod view of Foabic, passed to the mod's initializer.
 */
public interface ModContext {

    ModContainer mod();

    EventBus events();

    Scheduler scheduler();

    CommandRegistry commands();

    Environment environment();

    System.Logger logger();

    default Config config() {
        return mod().config();
    }

    default ModResources resources() {
        return mod().resources();
    }
}
