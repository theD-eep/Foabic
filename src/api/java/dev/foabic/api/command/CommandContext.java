package dev.foabic.api.command;

import dev.foabic.api.game.CommandSource;

import java.util.Map;

/**
 * The parsed arguments and source of one command invocation.
 */
public interface CommandContext {

    CommandSource source();

    /**
     * Returns the raw parsed value, or {@code null} when the argument was not
     * supplied. Prefer the typed getters when the type is known.
     */
    Object get(String name);

    String getString(String name);

    int getInt(String name);

    long getLong(String name);

    float getFloat(String name);

    double getDouble(String name);

    boolean getBoolean(String name);

    Map<String, Object> arguments();
}
