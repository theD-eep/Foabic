package dev.foabic.mc26.bridge;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import dev.foabic.api.game.CommandSource;
import net.minecraft.commands.CommandSourceStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class McCommandContext implements dev.foabic.api.command.CommandContext {

    private final McCommandSource source;
    private final Map<String, Object> arguments;

    McCommandContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, McServer server) {
        Objects.requireNonNull(context, "context");
        this.source = new McCommandSource(context.getSource(), server);
        Map<String, Object> parsed = new LinkedHashMap<>();
        for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
            CommandNode<CommandSourceStack> commandNode = node.getNode();
            if (commandNode instanceof ArgumentCommandNode<?, ?>) {
                parsed.put(commandNode.getName(),
                        context.getArgument(commandNode.getName(), Object.class));
            }
        }
        this.arguments = Map.copyOf(parsed);
    }

    @Override
    public CommandSource source() {
        return source;
    }

    @Override
    public Object get(String name) {
        return arguments.get(name);
    }

    @Override
    public String getString(String name) {
        Object value = required(name);
        return value instanceof String string ? string : String.valueOf(value);
    }

    @Override
    public int getInt(String name) {
        Object value = required(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    @Override
    public long getLong(String name) {
        Object value = required(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    @Override
    public float getFloat(String name) {
        Object value = required(name);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    @Override
    public double getDouble(String name) {
        Object value = required(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    @Override
    public boolean getBoolean(String name) {
        Object value = required(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        return switch (text.toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(
                    "argument '" + name + "' is not a boolean: " + value);
        };
    }

    @Override
    public Map<String, Object> arguments() {
        return arguments;
    }

    private Object required(String name) {
        Object value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("missing command argument '" + name + "'");
        }
        return value;
    }
}
