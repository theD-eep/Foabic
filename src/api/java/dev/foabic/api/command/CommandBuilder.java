package dev.foabic.api.command;

import dev.foabic.api.game.CommandSource;
import java.util.List;
import java.util.function.Predicate;

public interface CommandBuilder {
    String name();
    ArgumentType argumentType();
    CommandBuilder executes(CommandHandler handler);
    CommandBuilder requires(Predicate<CommandSource> requirement);
    /** Suggestions apply to argument nodes; literal nodes ignore them in Brigadier. */
    CommandBuilder suggests(SuggestionProvider provider);
    CommandBuilder then(CommandBuilder child);
    CommandBuilder literal(String name);
    CommandBuilder argument(String name, ArgumentType type);
    List<CommandBuilder> children();
    CommandHandler executor();
    Predicate<CommandSource> requirement();
    SuggestionProvider suggestionProvider();
}
