package dev.foabic.api.command;

import dev.foabic.api.game.CommandSource;

import java.util.List;
import java.util.function.Predicate;

/**
 * A mutable command tree node. Build trees from
 * {@link CommandRegistry#literal(String)}, then add children with
 * {@link #literal(String)} or {@link #argument(String, ArgumentType)}.
 */
public interface CommandBuilder {

    String name();

    /**
     * @return the argument type, or {@code null} for a literal node
     */
    ArgumentType argumentType();

    CommandBuilder executes(CommandHandler handler);

    CommandBuilder requires(Predicate<CommandSource> requirement);

    /**
     * Adds tab-completion suggestions. Brigadier only exposes suggestions on
     * argument nodes; calling this on a literal is ignored by the adapter.
     */
    CommandBuilder suggests(SuggestionProvider provider);

    /**
     * Attaches an existing node as a child of this node.
     */
    CommandBuilder then(CommandBuilder child);

    /**
     * Creates and attaches a literal child.
     */
    CommandBuilder literal(String name);

    /**
     * Creates and attaches an argument child.
     */
    CommandBuilder argument(String name, ArgumentType type);

    List<CommandBuilder> children();

    CommandHandler executor();

    Predicate<CommandSource> requirement();

    SuggestionProvider suggestionProvider();
}
