package dev.foabic.mc26.bridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.foabic.api.command.CommandBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

/**
 * Translates Foabic's small command tree into Brigadier exactly once.
 */
final class McCommands {

    private static final System.Logger LOG = System.getLogger("Foabic/commands");

    private McCommands() {
    }

    static int register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            List<CommandBuilder> roots,
            McServer server,
            Set<String> registered
    ) {
        int added = 0;
        for (CommandBuilder root : roots) {
            if (registered.contains(root.name())) {
                continue;
            }
            if (dispatcher.getRoot().getChild(root.name()) != null) {
                LOG.log(System.Logger.Level.WARNING,
                        "command '/{0}' collides with an existing command and was skipped",
                        root.name());
                registered.add(root.name());
                continue;
            }
            dispatcher.register(buildLiteral(root, server));
            registered.add(root.name());
            added++;
        }
        return added;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildLiteral(
            CommandBuilder node,
            McServer server
    ) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(node.name());
        applyCommon(builder, node, server);
        for (CommandBuilder child : node.children()) {
            builder.then(build(child, server));
        }
        return builder;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> buildArgument(
            CommandBuilder node,
            McServer server
    ) {
        RequiredArgumentBuilder<CommandSourceStack, ?> builder = switch (node.argumentType()) {
            case STRING -> Commands.argument(node.name(), StringArgumentType.string());
            case WORD -> Commands.argument(node.name(), StringArgumentType.word());
            case GREEDY_STRING -> Commands.argument(node.name(), StringArgumentType.greedyString());
            case INTEGER -> Commands.argument(node.name(), IntegerArgumentType.integer());
            case LONG -> Commands.argument(node.name(), LongArgumentType.longArg());
            case FLOAT -> Commands.argument(node.name(), FloatArgumentType.floatArg());
            case DOUBLE -> Commands.argument(node.name(), DoubleArgumentType.doubleArg());
            case BOOLEAN -> Commands.argument(node.name(), BoolArgumentType.bool());
        };
        applyCommon(builder, node, server);
        if (node.suggestionProvider() != null) {
            builder.suggests((context, suggestions) -> {
                try {
                    McCommandContext wrapped = new McCommandContext(context, server);
                    for (String suggestion : node.suggestionProvider().suggest(wrapped)) {
                        if (suggestion != null && !suggestion.isEmpty()) {
                            suggestions.suggest(suggestion);
                        }
                    }
                } catch (Throwable failure) {
                    LOG.log(System.Logger.Level.ERROR,
                            "command suggestions failed for '/" + node.name() + "'", failure);
                }
                return suggestions.buildFuture();
            });
        }
        for (CommandBuilder child : node.children()) {
            builder.then(build(child, server));
        }
        return builder;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> build(CommandBuilder node, McServer server) {
        return node.argumentType() == null
                ? buildLiteral(node, server)
                : buildArgument(node, server);
    }

    private static void applyCommon(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            CommandBuilder node,
            McServer server
    ) {
        builder.requires(source -> {
            try {
                return node.requirement().test(new McCommandSource(source, server));
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR,
                        "command requirement failed for '/" + node.name() + "'", failure);
                return false;
            }
        });

        if (node.executor() != null) {
            builder.executes(context -> {
                try {
                    return node.executor().execute(new McCommandContext(context, server));
                } catch (Throwable failure) {
                    LOG.log(System.Logger.Level.ERROR,
                            "command '/" + node.name() + "' failed", failure);
                    context.getSource().sendFailure(Component.literal(
                            "Command failed: " + failure.getClass().getSimpleName()));
                    return 0;
                }
            });
        }
    }
}
