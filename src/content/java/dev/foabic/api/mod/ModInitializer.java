package dev.foabic.api.mod;

/**
 * A Minecraft-facing mod entrypoint, loaded from {@code foabic.mod.json}.
 *
 * <p>Implementations need a public no-argument constructor and must be public.
 * A mod receives one {@link ModContext} for its lifetime.
 */
@FunctionalInterface
public interface ModInitializer {

    void onInitialize(ModContext context) throws Exception;
}
