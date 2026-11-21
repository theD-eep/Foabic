package dev.foabic.api.signal;

import java.util.Objects;

/**
 * Posted when a player chat message reaches the server.
 *
 * <p>The signal is plain data plus a platform output callback. A listener may
 * call {@link #replyAs(String, String)} to add a chat line without fabricating
 * a signed player chat packet. The adapters implement this as a server system
 * message formatted as {@code <name> message}.
 */
public final class ChatMessageSignal {

    private final String playerName;
    private final String message;
    private final ChatOutput output;

    public ChatMessageSignal(String playerName, String message, ChatOutput output) {
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.message = Objects.requireNonNull(message, "message");
        this.output = Objects.requireNonNull(output, "output");
    }

    public String playerName() {
        return playerName;
    }

    /** The raw chat text, without a leading slash and without formatting. */
    public String message() {
        return message;
    }

    /** Sends an additional chat line displayed as {@code <name> message}. */
    public void replyAs(String name, String message) {
        output.sendAs(Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(message, "message"));
    }

    @Override
    public String toString() {
        return "ChatMessageSignal[" + playerName + ": " + message + "]";
    }
}
