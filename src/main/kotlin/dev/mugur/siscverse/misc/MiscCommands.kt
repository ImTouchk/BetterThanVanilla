package dev.mugur.siscverse.misc

import com.mojang.brigadier.tree.LiteralCommandNode
import dev.mugur.siscverse.towns.TownCommands.Companion.acquire
import dev.mugur.siscverse.utils.ChatHelper
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

class MiscCommands {
    companion object {
        fun root(): LiteralCommandNode<CommandSourceStack> {
            return Commands
                .literal("misc")
                .then(EndDisabler.command())
                .then(Graveyard.command().build())
                .executes { ctx -> ChatHelper.sendMessage(ctx, "misc.help") }
                .build()
        }
    }
}