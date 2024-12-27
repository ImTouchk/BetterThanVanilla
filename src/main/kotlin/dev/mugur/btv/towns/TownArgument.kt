package dev.mugur.btv.towns

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import java.util.concurrent.CompletableFuture

@Suppress("UnstableApiUsage")
class TownArgument : CustomArgumentType.Converted<Town, String>  {
    override fun convert(input: String): Town {
        val message = MessageComponentSerializer
            .message()
            .serialize(Component.text("Town $input does not exist"))

        val town = TownManager.getTownByName(input)
            ?: throw CommandSyntaxException(SimpleCommandExceptionType(message), message)

        return town
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.string()
    }

    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val towns = TownManager.getActiveTowns()
        for(town in towns)
            builder.suggest(town.name)

        return builder.buildFuture()
    }
}