package dev.foabic.api.command;

import dev.foabic.api.game.CommandSource;
import java.util.Map;

public interface CommandContext {
    CommandSource source();
    Object get(String name);
    String getString(String name);
    int getInt(String name);
    long getLong(String name);
    float getFloat(String name);
    double getDouble(String name);
    boolean getBoolean(String name);
    Map<String, Object> arguments();
}
