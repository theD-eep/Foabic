package dev.foabic.loader.agent;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.loader.FoabicException;
import dev.foabic.loader.FoabicLoader;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The {@code -javaagent} entrypoint. It loads mods before Minecraft's main
 * class initializes, then installs the version adapter's class-file hooks.
 */
public final class FoabicAgent {

    private static final System.Logger LOG = System.getLogger("Foabic");

    private FoabicAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        try {
            start(arguments, instrumentation);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "Foabic failed to start", failure);
            throw new IllegalStateException("Foabic failed to start", failure);
        }
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        premain(arguments, instrumentation);
    }

    private static void start(String arguments, Instrumentation instrumentation) throws Exception {
        Options options = Options.parse(arguments);
        FoabicLoader.load(options.gameDir(), options.modsDir(), options.environment());
        installAdapter(instrumentation);

        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} ready: {1} mod(s), agent jar {2}",
                Foabic.VERSION,
                Foabic.mods().size(),
                agentSize());
    }

    private static void installAdapter(Instrumentation instrumentation) throws ReflectiveOperationException {
        try {
            Class<?> adapter = Class.forName(
                    "dev.foabic.mc26.Mc26Adapter", true, FoabicAgent.class.getClassLoader());
            adapter.getMethod("install", Instrumentation.class).invoke(null, instrumentation);
        } catch (ClassNotFoundException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "Minecraft 26.x adapter is not present; core API loaded without game hooks");
        } catch (NoClassDefFoundError e) {
            LOG.log(System.Logger.Level.WARNING,
                    "Minecraft 26.x adapter could not be loaded: {0}", e.getMessage());
        }
    }

    private static String agentSize() {
        try {
            var source = FoabicAgent.class.getProtectionDomain().getCodeSource();
            if (source != null) {
                Path path = Path.of(source.getLocation().toURI());
                if (Files.isRegularFile(path)) {
                    return (Files.size(path) / 1024L) + " KiB";
                }
            }
        } catch (Exception ignored) {
            // A size is a diagnostic, not a requirement.
        }
        return "unknown";
    }

    private record Options(Path gameDir, Path modsDir, Environment environment) {

        private static Options parse(String raw) {
            String game = System.getProperty("foabic.gameDir", System.getProperty("user.dir", "."));
            String mods = System.getProperty("foabic.modsDir");
            String env = System.getProperty("foabic.env", "server");

            if (raw != null && !raw.isBlank()) {
                for (String pair : raw.split(";")) {
                    int equals = pair.indexOf('=');
                    if (equals <= 0) {
                        continue;
                    }
                    String key = pair.substring(0, equals).trim();
                    String value = pair.substring(equals + 1).trim();
                    switch (key) {
                        case "gameDir" -> game = value;
                        case "modsDir" -> mods = value;
                        case "env", "environment" -> env = value;
                        default -> LOG.log(System.Logger.Level.WARNING,
                                "unknown Foabic agent option: {0}", key);
                    }
                }
            }

            Path gameDir = Path.of(game).toAbsolutePath().normalize();
            Path modsDir = mods == null
                    ? gameDir.resolve("mods")
                    : Path.of(mods).toAbsolutePath().normalize();
            Environment environment = switch (env.toLowerCase(Locale.ROOT)) {
                case "client" -> Environment.CLIENT;
                case "server" -> Environment.SERVER;
                default -> throw new FoabicException("unknown Foabic environment: " + env);
            };
            return new Options(gameDir, modsDir, environment);
        }
    }
}
