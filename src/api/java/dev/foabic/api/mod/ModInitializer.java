package dev.foabic.api.mod;

/**
 * The single entrypoint type supported by Foabic.
 *
 * <p>The class named in {@code foabic.mod} must have a public no-argument
 * constructor and implement this interface. Initializers run once, on the
 * loader thread, before Minecraft starts. Keep this method cheap: register
 * listeners and command trees, then return. Do not touch worlds, players or
 * game registries here.
 */
@FunctionalInterface
public interface ModInitializer {

    void onInitialize(ModContext context) throws Exception;
}
