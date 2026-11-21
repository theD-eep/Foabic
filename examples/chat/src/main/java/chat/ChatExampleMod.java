package chat;

import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.signal.ChatMessageSignal;
import dev.foabic.api.signal.ServerStartedSignal;

/**
 * Foabic chat example.
 *
 * <p>When a player says {@code aaawww123}, the mod adds a server chat line
 * displayed as {@code <foabic> wwwaaa}. It uses only the common Foabic API;
 * there is no Fabric or NeoForge import.
 */
public final class ChatExampleMod implements ModInitializer {

    static final String TRIGGER = "aaawww123";
    static final String REPLY_NAME = "foabic";
    static final String REPLY_MESSAGE = "wwwaaa";

    @Override
    public void onInitialize(ModContext context) {
        context.connect(ServerStartedSignal.class, signal ->
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_CHAT_READY platform={0} mc={1}",
                        signal.platform(), signal.minecraftVersion()));

        context.connect(ChatMessageSignal.class, signal -> {
            if (!TRIGGER.equals(signal.message())) return;
            signal.replyAs(REPLY_NAME, REPLY_MESSAGE);
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_CHAT_REPLY player={0} text={1}",
                    signal.playerName(), REPLY_MESSAGE);
        });
    }
}
