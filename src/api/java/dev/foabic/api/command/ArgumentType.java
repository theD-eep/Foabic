package dev.foabic.api.command;

/**
 * The argument types Foabic can translate to Brigadier without pulling a
 * command parser into the API jar.
 */
public enum ArgumentType {
    STRING,
    WORD,
    GREEDY_STRING,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN
}
