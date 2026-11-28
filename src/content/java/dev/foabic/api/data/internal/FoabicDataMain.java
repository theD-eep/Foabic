package dev.foabic.api.data.internal;

import dev.foabic.api.data.FoabicDataProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone entry point used by the {@code foabicData} Gradle task.
 *
 * <p>It deliberately has no Minecraft dependency: providers receive only the
 * Foabic data API and plain strings, so data generation can run before the
 * game launches.
 */
public final class FoabicDataMain {

    private FoabicDataMain() {
    }

    public static void main(String[] args) throws Exception {
        String modId = null;
        Path output = null;
        List<String> providers = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--id" -> modId = require(args, ++i, "--id");
                case "--output" -> output = Path.of(require(args, ++i, "--output"));
                case "--provider" -> providers.add(require(args, ++i, "--provider"));
                default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
            }
        }
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("--id is required");
        }
        if (output == null) {
            throw new IllegalArgumentException("--output is required");
        }
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("at least one --provider is required");
        }

        FoabicDataContextImpl context = new FoabicDataContextImpl(modId);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String name : providers) {
            Class<?> type = Class.forName(name, true, loader);
            if (!FoabicDataProvider.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("data provider " + name
                        + " does not implement " + FoabicDataProvider.class.getName());
            }
            FoabicDataProvider provider = (FoabicDataProvider) type.getDeclaredConstructor().newInstance();
            provider.generate(context);
        }
        int files = context.write(output);
        System.out.println("FOABIC_DATAGEN mod=" + modId + " providers=" + providers.size()
                + " files=" + files + " data=" + context.dataFileCount()
                + " assets=" + context.assetFileCount() + " output=" + output.toAbsolutePath());
    }

    private static String require(String[] args, int index, String name) {
        if (index >= args.length || args[index].isBlank()) {
            throw new IllegalArgumentException(name + " needs a value");
        }
        return args[index];
    }
}
