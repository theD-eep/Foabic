package dev.foabic.api.mod;

@FunctionalInterface
public interface ModInitializer {

    void onInitialize(ModContext context) throws Exception;
}
