package dev.foabic.api.command;

@FunctionalInterface
public interface CommandHandler {

    /**
     * @return a Brigadier-style result; 0 means failure, positive means success
     */
    int execute(CommandContext context);
}
