package dev.mugur.btv.misc

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.mugur.btv.utils.ChatCommand
import dev.mugur.btv.utils.ChatHelper
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class MiscCommands {
    companion object {
        fun nickname(): ChatCommand {
            return ChatCommand("nickname")
                .requirePlayerSender()
                .argument("value", StringArgumentType.string())
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val displayName = StringArgumentType.getString(ctx, "value")
                    player.displayName(Component.text(displayName))
                    ChatHelper.sendMessage(ctx, "misc.success.nickname_set", displayName)
                }
        }

        fun root(): LiteralCommandNode<CommandSourceStack> {
            return Commands
                .literal("misc")
                .then(EndDisabler.command().build())
                .then(Graveyard.command().build())
                .then(nickname().build())
                .executes { ctx -> ChatHelper.sendMessage(ctx, "misc.help") }
                .build()
        }
    }
}