package dev.foabic.api.data;

/**
 * A build-time and runtime data generator.
 *
 * <p>A provider is a normal public class with a public no-argument constructor.
 * It is named by {@code data} (or {@code dataProviders}) in
 * {@code foabic.mod.json}. The same class can also be invoked from
 * {@code context.data().provider(...)} during mod initialization.
 *
 * <p>Providers should only use Foabic's data API and plain Java strings. They
 * must not require a live Minecraft server; this is what lets the Gradle
 * {@code foabicData} task run them in a plain JVM.
 */
@FunctionalInterface
public interface FoabicDataProvider {

    void generate(FoabicDataContext context);
}
