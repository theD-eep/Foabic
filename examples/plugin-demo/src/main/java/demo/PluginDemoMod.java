package demo;

import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.signal.ServerStartedSignal;

/** A one-source-tree Foabic mod packaged by {@code dev.foabic.loom}. */
public final class PluginDemoMod implements ModInitializer {

    @Override
    public void onInitialize(ModContext context) {
        context.connect(ServerStartedSignal.class, signal ->
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_PLUGIN_DEMO platform={0} mod={1}",
                        signal.platform(), context.mod().id()));
        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_PLUGIN_READY platform={0}", context.platform());
    }
}
