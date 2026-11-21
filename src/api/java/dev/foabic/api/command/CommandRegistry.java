package dev.foabic.api.command;

import java.util.List;

public interface CommandRegistry {
    CommandBuilder literal(String name);
    List<CommandBuilder> roots();
}
