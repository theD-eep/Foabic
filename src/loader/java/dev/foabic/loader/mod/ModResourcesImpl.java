package dev.foabic.loader.mod;

import dev.foabic.api.mod.ModResources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A scoped view of one mod jar. Other mods' resources are not visible here.
 */
public final class ModResourcesImpl implements ModResources, AutoCloseable {

    private final Path jarPath;
    private JarFile jarFile;
    private boolean closed;

    public ModResourcesImpl(Path jarPath) {
        this.jarPath = Objects.requireNonNull(jarPath, "jarPath");
    }

    @Override
    public Optional<InputStream> open(String path) throws IOException {
        String normalized = normalize(path);
        synchronized (this) {
            if (closed) {
                throw new IOException("mod resources are closed: " + jarPath);
            }
            JarEntry entry = jar().getJarEntry(normalized);
            if (entry == null || entry.isDirectory()) {
                return Optional.empty();
            }
            return Optional.of(jar().getInputStream(entry));
        }
    }

    @Override
    public boolean contains(String path) {
        try {
            String normalized = normalize(path);
            synchronized (this) {
                if (closed) {
                    return false;
                }
                JarEntry entry = jar().getJarEntry(normalized);
                return entry != null && !entry.isDirectory();
            }
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private JarFile jar() throws IOException {
        if (jarFile == null) {
            jarFile = new JarFile(jarPath.toFile(), false);
        }
        return jarFile;
    }

    private static String normalize(String path) {
        Objects.requireNonNull(path, "path");
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("empty resource path");
        }
        for (String segment : normalized.split("/")) {
            if (segment.equals("..") || segment.isEmpty()) {
                throw new IllegalArgumentException("invalid resource path: " + path);
            }
        }
        return normalized;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException ignored) {
                // Nothing useful can be done while shutting down.
            }
            jarFile = null;
        }
    }
}
