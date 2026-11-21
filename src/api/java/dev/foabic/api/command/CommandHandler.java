package dev.foabic.api.command;

@FunctionalInterface
public interface CommandHandler {
    int execute(CommandContext context);
}
