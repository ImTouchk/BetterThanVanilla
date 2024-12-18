package dev.mugur.btv.misc

import com.mojang.brigadier.tree.LiteralCommandNode
import dev.mugur.btv.utils.ChatHelper
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