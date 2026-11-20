package dev.foabic.api.command;

import java.util.List;

@FunctionalInterface
public interface SuggestionProvider {

    List<String> suggest(CommandContext context);
}
