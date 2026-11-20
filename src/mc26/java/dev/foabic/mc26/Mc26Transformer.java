package dev.foabic.mc26;

import dev.foabic.loader.BytecodeInjector;

import java.lang.constant.ClassDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;

final class Mc26Transformer implements ClassFileTransformer {

    private static final System.Logger LOG = System.getLogger("Foabic/mc26");

    private static final ClassDesc HOOKS = ClassDesc.of("dev.foabic.mc26.Mc26Hooks");

    private static final Set<String> TARGETS = Set.of(
            "net/minecraft/server/MinecraftServer",
            "net/minecraft/server/players/PlayerList",
            "net/minecraft/server/level/ServerLevel"
    );

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) throws IllegalClassFormatException {
        if (className == null || !TARGETS.contains(className)) {
            return null;
        }

        List<BytecodeInjector.Injection> injections = switch (className) {
            case "net/minecraft/server/MinecraftServer" -> List.of(
                    new BytecodeInjector.Injection(
                            "runServer", 0, "serverStarting", "serverStopped"),
                    new BytecodeInjector.Injection(
                            "tickServer", 0, "serverTickStart", "serverTickEnd"),
                    new BytecodeInjector.Injection(
                            "stopServer", 0, "serverStopping", null)
            );
            case "net/minecraft/server/players/PlayerList" -> List.of(
                    new BytecodeInjector.Injection(
                            "placeNewPlayer", 2, null, "playerJoin"),
                    new BytecodeInjector.Injection(
                            "remove", 1, "playerLeave", null)
            );
            case "net/minecraft/server/level/ServerLevel" -> List.of(
                    new BytecodeInjector.Injection(
                            "tick", 0, "levelTickStart", "levelTickEnd")
            );
            default -> List.of();
        };

        try {
            byte[] transformed = BytecodeInjector.inject(classfileBuffer, HOOKS, injections);
            LOG.log(System.Logger.Level.INFO, "installed Foabic hooks in {0}", className);
            return transformed;
        } catch (Throwable failure) {
            IllegalClassFormatException error = new IllegalClassFormatException(
                    "Foabic could not transform " + className);
            error.initCause(failure);
            throw error;
        }
    }
}
