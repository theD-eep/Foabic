package dev.foabic.loader.command;

import dev.foabic.api.command.ArgumentType;
import dev.foabic.api.command.CommandBuilder;
import dev.foabic.api.command.CommandHandler;
import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.command.SuggestionProvider;
import dev.foabic.api.game.CommandSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class CommandRegistryImpl implements CommandRegistry {

    private static final Pattern ROOT = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern NODE = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

    private final Map<String, MutableCommandNode> roots = new LinkedHashMap<>();

    @Override
    public CommandBuilder literal(String name) {
        Objects.requireNonNull(name, "name");
        if (!ROOT.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid command name: " + name);
        }
        if (roots.containsKey(name)) {
            throw new IllegalArgumentException("command '/" + name + "' is already registered");
        }
        MutableCommandNode node = new MutableCommandNode(name, null);
        roots.put(name, node);
        return node;
    }

    @Override
    public List<CommandBuilder> roots() {
        return List.copyOf(roots.values());
    }

    public static final class MutableCommandNode implements CommandBuilder {
        private final String name;
        private final ArgumentType argumentType;
        private final Map<String, MutableCommandNode> children = new LinkedHashMap<>();
        private CommandHandler executor;
        private Predicate<CommandSource> requirement = source -> true;
        private SuggestionProvider suggestionProvider;

        private MutableCommandNode(String name, ArgumentType argumentType) {
            this.name = name;
            this.argumentType = argumentType;
        }

        @Override public String name() { return name; }
        @Override public ArgumentType argumentType() { return argumentType; }

        @Override
        public CommandBuilder executes(CommandHandler handler) {
            executor = Objects.requireNonNull(handler, "handler");
            return this;
        }

        @Override
        public CommandBuilder requires(Predicate<CommandSource> requirement) {
            this.requirement = Objects.requireNonNull(requirement, "requirement");
            return this;
        }

        @Override
        public CommandBuilder suggests(SuggestionProvider provider) {
            suggestionProvider = Objects.requireNonNull(provider, "provider");
            return this;
        }

        @Override
        public CommandBuilder then(CommandBuilder child) {
            Objects.requireNonNull(child, "child");
            if (child == this) throw new IllegalArgumentException("node cannot be its own child");
            if (!NODE.matcher(child.name()).matches()) {
                throw new IllegalArgumentException("invalid command node name: " + child.name());
            }
            if (children.containsKey(child.name())) {
                throw new IllegalArgumentException("duplicate child '" + child.name() + "' under '/" + name + "'");
            }
            if (!(child instanceof MutableCommandNode mutable)) {
                throw new IllegalArgumentException("command nodes must come from this registry");
            }
            children.put(mutable.name, mutable);
            return this;
        }

        @Override
        public CommandBuilder literal(String childName) {
            MutableCommandNode child = new MutableCommandNode(requireNodeName(childName), null);
            then(child);
            return child;
        }

        @Override
        public CommandBuilder argument(String argumentName, ArgumentType type) {
            Objects.requireNonNull(type, "type");
            MutableCommandNode child = new MutableCommandNode(requireNodeName(argumentName), type);
            then(child);
            return child;
        }

        private static String requireNodeName(String value) {
            Objects.requireNonNull(value, "name");
            if (!NODE.matcher(value).matches()) {
                throw new IllegalArgumentException("invalid command node name: " + value);
            }
            return value;
        }

        @Override public List<CommandBuilder> children() { return List.copyOf(children.values()); }
        @Override public CommandHandler executor() { return executor; }
        @Override public Predicate<CommandSource> requirement() { return requirement; }
        @Override public SuggestionProvider suggestionProvider() { return suggestionProvider; }
    }
}
