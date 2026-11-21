package dev.foabic.loader.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.foabic.api.config.ConfigManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Gson-backed configuration store. Gson is already part of Minecraft, so Foabic
 * does not add a new runtime dependency.
 */
public final class ConfigManagerImpl implements ConfigManager {

    private final Path directory;
    private final System.Logger logger;
    private final Gson gson;
    private final Map<Class<?>, Object> instances = new HashMap<>();

    public ConfigManagerImpl(Path gameDirectory, String modId, System.Logger logger) {
        Path configRoot = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath().normalize().resolve("config").resolve("foabic");
        this.directory = configRoot.resolve(Objects.requireNonNull(modId, "modId")).normalize();
        if (!directory.startsWith(configRoot)) {
            throw new IllegalArgumentException("config path escapes config root");
        }
        this.logger = Objects.requireNonNull(logger, "logger");
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    @Override
    public synchronized <T> T register(Class<T> type) {
        Objects.requireNonNull(type, "type");
        @SuppressWarnings("unchecked")
        T existing = (T) instances.get(type);
        if (existing != null) return existing;

        validate(type);
        Path file = directory.resolve(type.getSimpleName() + ".json");
        T loaded;
        if (Files.isRegularFile(file)) {
            loaded = read(type, file);
            if (loaded == null) {
                loaded = createDefault(type);
                backup(file);
            }
        } else {
            loaded = createDefault(type);
        }
        write(file, loaded);
        instances.put(type, loaded);
        logger.log(System.Logger.Level.INFO, "FOABIC_CONFIG type={0} file={1}",
                type.getName(), file);
        return loaded;
    }

    @Override
    public synchronized <T> void save(T config) {
        Objects.requireNonNull(config, "config");
        Class<?> type = config.getClass();
        Path file = directory.resolve(type.getSimpleName() + ".json");
        write(file, config);
        instances.put(type, config);
    }

    private <T> T read(Class<T> type, Path file) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            T value = gson.fromJson(json, type);
            if (value == null) {
                logger.log(System.Logger.Level.ERROR,
                        "config {0} is empty; regenerating defaults", file);
            }
            return value;
        } catch (IOException | RuntimeException e) {
            logger.log(System.Logger.Level.ERROR,
                    "cannot read config {0}; regenerating defaults: {1}", file, e.getMessage());
            return null;
        }
    }

    private <T> T createDefault(Class<T> type) {
        try {
            if (type.isRecord()) {
                RecordComponent[] components = type.getRecordComponents();
                Class<?>[] parameterTypes = new Class<?>[components.length];
                Object[] arguments = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    parameterTypes[i] = components[i].getType();
                    arguments[i] = defaultValue(parameterTypes[i]);
                }
                Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            }
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("config type " + type.getName()
                    + " needs a no-arg constructor (or must be a record)", e);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private void write(Path file, Object value) {
        try {
            Files.createDirectories(directory);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, gson.toJson(value) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot write config " + file, e);
        }
    }

    private void backup(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(System.Logger.Level.WARNING, "cannot back up broken config {0}", file);
        }
    }

    private static void validate(Class<?> type) {
        if (type.isInterface() || type.isEnum() || type.isArray()
                || java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("unsupported config type: " + type.getName());
        }
    }
}
