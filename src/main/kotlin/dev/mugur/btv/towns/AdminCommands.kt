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

                    if(town.mayor != player.uniqueId && !player.isOp)
                        return@executes ChatHelper.sendMessage(ctx, "misc.error.not_op")

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

        fun delete(): ChatCommand {
            return ChatCommand("delete")
                .requireOp()
                .argument("town", TownArgument())
                .executes { ctx ->
                    val town = ctx.getArgument("town", Town::class.java)

                    ChatHelper.broadcastMessage("town.success.deleted", town.name, ctx.source.sender.name)


                    Command.SINGLE_SUCCESS
                }
        }

        fun modify(): ChatCommand {
            return ChatCommand("modify")
                .argument("town", TownArgument())
                .then(mayor())
        }
    }
}