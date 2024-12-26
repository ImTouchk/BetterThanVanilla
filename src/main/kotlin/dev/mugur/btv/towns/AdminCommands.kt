package dev.mugur.btv.towns

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import dev.mugur.btv.utils.ChatCommand
import dev.mugur.btv.utils.ChatHelper
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.entity.Player

class AdminCommands {
    companion object {
        private fun mayor(): ChatCommand {
            return ChatCommand("mayor")
                .argument("player", ArgumentTypes.player())
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val townName = StringArgumentType.getString(ctx, "town")
                    val town = TownManager.getTownByName(townName)
                        ?: return@executes ChatHelper.sendMessage(ctx, "town.error.name_not_found", townName)

                    val resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                    val target = resolver.resolve(ctx.source)[0]
                    val targetTown = TownManager.getTownOfPlayer(target)
                    if(targetTown != null && targetTown.id != town.id)
                        return@executes ChatHelper.sendMessage(ctx, "town.error.invalid_player", target.name)

                    town.mayor = target.uniqueId
                    player.sendMessage("Ok")
                    Command.SINGLE_SUCCESS
                }
        }

        fun modify(): ChatCommand {
            return ChatCommand("modify")
                .require({ sender -> sender.isOp }, "misc.error.not_op")
                .argument("town", StringArgumentType.string())
                .then(mayor())
        }
    }
}