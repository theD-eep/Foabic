package dev.foabic.fabric.command;

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

public final class FabricCommands {

    private static final System.Logger LOG = System.getLogger("Foabic/commands");

    private FabricCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (CommandBuilder root : dev.foabic.api.Foabic.commands().roots()) {
            if (dispatcher.getRoot().getChild(root.name()) != null) continue;
            dispatcher.register(buildLiteral(root));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildLiteral(CommandBuilder node) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(node.name());
        applyCommon(builder, node);
        for (CommandBuilder child : node.children()) builder.then(build(child));
        return builder;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> buildArgument(
            CommandBuilder node) {
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
        applyCommon(builder, node);
        if (node.suggestionProvider() != null) {
            builder.suggests((context, suggestions) -> {
                try {
                    FabricCommandContext wrapped = new FabricCommandContext(context);
                    for (String suggestion : node.suggestionProvider().suggest(wrapped)) {
                        if (suggestion != null && !suggestion.isEmpty()) suggestions.suggest(suggestion);
                    }
                } catch (Throwable failure) {
                    LOG.log(System.Logger.Level.ERROR, "command suggestions failed", failure);
                }
                return suggestions.buildFuture();
            });
        }
        for (CommandBuilder child : node.children()) builder.then(build(child));
        return builder;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> build(CommandBuilder node) {
        return node.argumentType() == null ? buildLiteral(node) : buildArgument(node);
    }

    private static void applyCommon(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            CommandBuilder node
    ) {
        builder.requires(source -> {
            try {
                return node.requirement().test(new FabricCommandSource(source));
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "command requirement failed", failure);
                return false;
            }
        });
        if (node.executor() != null) {
            builder.executes(context -> {
                try {
                    return node.executor().execute(new FabricCommandContext(context));
                } catch (Throwable failure) {
                    LOG.log(System.Logger.Level.ERROR, "command '/" + node.name() + "' failed", failure);
                    context.getSource().sendFailure(Component.literal(
                            "Command failed: " + failure.getClass().getSimpleName()));
                    return 0;
                }
            });
        }
    }
}
