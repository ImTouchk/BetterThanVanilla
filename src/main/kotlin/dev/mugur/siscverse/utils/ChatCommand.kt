package dev.mugur.siscverse.utils

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class ChatCommand(name: String) {
    private data class ArgumentData<T>(
        val name: String,
        val type: ArgumentType<T>
    )

    private data class SenderRequirement(
        val condition: (sender: CommandSender) -> Boolean,
        val errorMessage: String,
        val errorArgs: List<Any?>?
    )

    private var commandName: String = name
        private set

    private var then: ChatCommand? = null
    private var infoMessage: String? = null
    private var infoArgs: List<Any?>? = null
    private var requirePlayer: Boolean = false
    private val requirements = mutableListOf<SenderRequirement>()
    private val arguments = mutableListOf<ArgumentData<*>>()
    private val subcommands = mutableListOf<ChatCommand>()
    private var executor: ((ctx: CommandContext<CommandSourceStack>) -> Int)? = null

    constructor() : this("_uninit") {}

    fun name(name: String): ChatCommand {
        this.commandName = name
        return this
    }

    fun require(
        condition: (sender: CommandSender) -> Boolean,
        errorMessage: String,
        vararg errorArgs: Any?
    ): ChatCommand {
        requirements.add(SenderRequirement(
            condition,
            errorMessage,
            listOf(*errorArgs)
        ))
        return this
    }

    fun executes(fn: (ctx: CommandContext<CommandSourceStack>) -> Int): ChatCommand {
        executor = fn
        return this
    }

    fun subcommand(command: ChatCommand): ChatCommand {
        subcommands.add(command)
        return this
    }

    fun requirePlayerSender(): ChatCommand {
        requirePlayer = true
        return this
    }

    fun argument(name: String, type: ArgumentType<*>): ChatCommand {
        arguments.add(ArgumentData(name, type))
        return this
    }

    fun then(cmd: ChatCommand): ChatCommand {
        this.then = cmd
        return this
    }

    fun info(message: String, vararg args: Any?): ChatCommand {
        infoMessage = message
        infoArgs = listOf(*args)
        return this
    }

    fun build(): LiteralCommandNode<CommandSourceStack> {
        var res = Commands.literal(commandName)
        if(requirePlayer)
            res = res.requires { src -> src.sender is Player }

        for(command in subcommands)
            res = res.then(command.build())

        res =
            if(arguments.isNotEmpty())
                res.then(getArgumentTree())
            else if(executor != null)
                res.executes(getExecutor())
            else if(this.then != null)
                res.then(this.then!!.build())
            else
                res

        return res.build()
    }

    private fun getArgumentTree(i: Int = 0): ArgumentBuilder<CommandSourceStack, *> {
        return if(i < arguments.size - 1) {
            Commands
                .argument(arguments[i].name, arguments[i].type)
                .then(getArgumentTree(i + 1))
                .executes(getHelpMessage())
        } else {
            Commands
                .argument(arguments[i].name, arguments[i].type)
                .executes(getExecutor())
        }

    }

    private fun getExecutor(): (ctx: CommandContext<CommandSourceStack>) -> Int {
        return execLambda@{ ctx ->
            for(req in requirements) {
                if(!req.condition(ctx.source.sender))
                    return@execLambda ChatHelper.sendMessage(
                        ctx,
                        req.errorMessage,
                        *req.errorArgs!!.toTypedArray()
                    )
            }

            executor!!(ctx)
        }
    }

    private fun getHelpMessage(): (ctx: CommandContext<CommandSourceStack>) -> Int {
        return { ctx ->
            if(infoMessage != null)
                ChatHelper.sendMessage(ctx, infoMessage!!, *infoArgs!!.toTypedArray())
            else
                Command.SINGLE_SUCCESS
        }
    }
}
