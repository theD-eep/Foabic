package dev.foabic.mc26;

import java.lang.instrument.Instrumentation;

/**
 * The only class the agent loads by name. Keeping the adapter separate means
 * the API/loader can also run without a Minecraft jar present.
 */
public final class Mc26Adapter {

    private static final System.Logger LOG = System.getLogger("Foabic/mc26");

    private Mc26Adapter() {
    }

    public static void install(Instrumentation instrumentation) {
        instrumentation.addTransformer(new Mc26Transformer(), false);
        LOG.log(System.Logger.Level.INFO, "Minecraft 26.x adapter installed");
    }
}
