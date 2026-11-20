package dev.foabic.api.command;

import java.util.List;

/**
 * The command roots registered by all mods. Commands are buffered until the
 * server has a Brigadier dispatcher, then registered once.
 */
public interface CommandRegistry {

    /**
     * Creates and registers a root literal command.
     *
     * @throws IllegalArgumentException when the name is invalid or already used
     */
    CommandBuilder literal(String name);

    List<CommandBuilder> roots();
}
