package dev.foabic.mc26;

import dev.foabic.mc26.spi.HookBridge;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Loads the Minecraft-touching bridge in a small child-first class loader whose
 * parent is the class loader that actually owns Minecraft classes.
 */
final class AdapterLoader {

    private AdapterLoader() {
    }

    static HookBridge create(ClassLoader minecraftLoader) {
        URL source = sourceUrl();
        ClassLoader parent = minecraftLoader == null
                ? ClassLoader.getSystemClassLoader()
                : minecraftLoader;

        URLClassLoader loader = new URLClassLoader("FoabicMc26Bridge", new URL[]{source}, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("dev.foabic.mc26.bridge.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            try {
                                type = findClass(name);
                            } catch (ClassNotFoundException ignored) {
                                type = super.loadClass(name, false);
                            }
                        }
                        if (resolve) {
                            resolveClass(type);
                        }
                        return type;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };

        try {
            Class<?> bridgeType = Class.forName("dev.foabic.mc26.bridge.McBridge", true, loader);
            return (HookBridge) bridgeType.getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError failure) {
            try {
                loader.close();
            } catch (Exception ignored) {
                // Preserve the original failure.
            }
            throw new IllegalStateException("cannot load Minecraft 26.x bridge", failure);
        }
    }

    private static URL sourceUrl() {
        String configured = System.getProperty("foabic.agentJar");
        if (configured != null && !configured.isBlank()) {
            try {
                return java.nio.file.Path.of(configured).toAbsolutePath().toUri().toURL();
            } catch (Exception e) {
                throw new IllegalStateException("invalid foabic.agentJar: " + configured, e);
            }
        }
        var source = Mc26Hooks.class.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            throw new IllegalStateException(
                    "Foabic hook class has no code source; set -Dfoabic.agentJar=<foabic.jar>");
        }
        return source.getLocation();
    }
}
