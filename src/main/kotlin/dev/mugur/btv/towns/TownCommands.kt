package dev.mugur.btv.towns

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.mugur.btv.Main
import dev.mugur.btv.towns.interact.TownObjectType
import dev.mugur.btv.utils.ChatCommand
import dev.mugur.btv.utils.ChatHelper
import dev.mugur.btv.utils.Misc
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

@Suppress("UnstableApiUsage")
class TownCommands {
    companion object {
        private fun leave(): ChatCommand {
            return ChatCommand("leave")
                .requirePlayerSender()
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val town = TownManager.getTownOfPlayer(player)
                        ?: return@executes ChatHelper.sendMessage(ctx, "town.error.not_in_a_town")

                    town.removePlayer(player, "town.player.left")

                    Command.SINGLE_SUCCESS
                }
        }

        private fun decline(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("decline")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("town", StringArgumentType.string())
                    .executes { ctx ->
                        val townName = StringArgumentType.getString(ctx, "town")
                        val town = TownManager.getTownByName(townName)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.name_not_found", townName)

                        val player = ctx.source.sender as Player
                        val pdc = player.persistentDataContainer
                        val key = NamespacedKey(Main.instance!!, town.name)
                        val timestamp = pdc.get(key, PersistentDataType.LONG)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.invalid_invite")

                        val current = System.currentTimeMillis()
                        if(current - timestamp > 1000 * 60 * 15)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.invalid_invite")

                        pdc.remove(key)
                        ChatHelper.sendMessage(ctx, "town.success.invite_declined", town.name)
                    }
                )
        }

        private fun join(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("join")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("town", StringArgumentType.string())
                    .executes { ctx ->
                        val townName = StringArgumentType.getString(ctx, "town")
                        val town = TownManager.getTownByName(townName)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.name_not_found", townName)

                        var hasValidInvite = true

                        val player = ctx.source.sender as Player
                        val pdc = player.persistentDataContainer
                        val key = NamespacedKey(Main.instance!!, town.id.toString())
                        val timestamp = pdc.get(key, PersistentDataType.LONG)
                        if(timestamp == null)
                            hasValidInvite = false
                        else {
                            val current = System.currentTimeMillis()
                            if (current - timestamp > 1000 * 60 * 15)
                                hasValidInvite = false
                        }

                        if(town.players.size == 0 && town.founder == player.uniqueId)
                            hasValidInvite = true

                        if(!hasValidInvite && !player.isOp)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.invalid_invite")

                        TownManager
                            .getTownOfPlayer(player)
                            ?.removePlayer(player, "town.player.left")

                        town.addPlayer(player)
                        pdc.remove(key)

                        Command.SINGLE_SUCCESS
                    }
                )
        }

        private fun invite(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("invite")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("player", ArgumentTypes.player())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val town = TownManager.getTownOfPlayer(player)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.not_in_a_town")

                        if(town.mayor != player.uniqueId)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.insufficient_permissions")

                        val resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                        val target = resolver.resolve(ctx.source)[0]
                        if(target == player || TownManager.getTownOfPlayer(target)?.id == town.id)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.invalid_player", target.name)

                        val pdc = target.persistentDataContainer
                        val key = NamespacedKey(Main.instance!!, town.id.toString())
                        pdc.set(key, PersistentDataType.LONG, System.currentTimeMillis())

                        ChatHelper.sendMessage(target, "town.invited", town.name, town.name, town.name)
                        ChatHelper.sendMessage(ctx, "town.success.invited", target.name, town.name)
                    }
                )

        }

        private fun invites(): LiteralCommandNode<CommandSourceStack> {
            return ChatCommand("invites")
                .requirePlayerSender()
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val invites = TownManager.getPlayerTownInvites(player)
                    for(invite in invites) {
                        ChatHelper.sendMessage(
                            player,
                            "town.player.invitation",
                            invite.name, invite.name, invite.name
                        )
                    }

                    if(invites.isEmpty())
                        ChatHelper.sendMessage(player, "town.error.no_invites")

                    Command.SINGLE_SUCCESS
                }
                .build()
        }

        private fun donate(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("donate")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("amount", IntegerArgumentType.integer(1))
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val town = TownManager.getTownOfPlayer(player)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.not_in_a_town")

                        val amount = IntegerArgumentType.getInteger(ctx, "amount")
                        if(!Misc.tryTakeCurrency(player, amount)) {
                            return@executes ChatHelper.sendMessage(ctx, "town.error.insufficient_funds")
                        }
                        town.money += amount
                        town.broadcast(ChatHelper.getMessage("town.success.donated", player.name, amount)!!)
                        return@executes Command.SINGLE_SUCCESS
                    }
                )
                .executes { ctx -> ChatHelper.sendMessage(ctx, "town.help.donate") }
        }

        private fun color(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("color")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("color", ArgumentTypes.namedColor())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        val town = TownManager.getTownOfPlayer(player)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.not_in_a_town")

                        if(town.mayor != player.uniqueId)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.insufficient_permissions")

                        val color = ctx.getArgument("color", NamedTextColor::class.java)
                        town.color = color
                        town.getScoreboardTeam().prefix(
                            Component
                                .text(town.name)
                                .color(color)
                                .append(
                                    Component
                                        .text(" ")
                                        .color(NamedTextColor.WHITE)
                                )
                        )

                        ChatHelper.sendMessage(ctx, "town.success.color_changed")
                    }
                )
        }

        private fun create(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("create")
                .requires { source -> source.sender is Player }
                .then(Commands
                    .argument("name", StringArgumentType.string())
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        if(TownManager.isTownMayor(player))
                            return@executes ChatHelper.sendMessage(ctx, "town.error.already_mayor")

                        val name = StringArgumentType.getString(ctx, "name")
                        if(TownManager.getTownByName(name) != null)
                            return@executes ChatHelper.sendMessage(ctx, "town.error.name_already_exists", name)

                        Town(name, player)
                        player.inventory.addItem(TownObjectType.DonateChest.toItemStack())

                        ChatHelper.broadcastMessage("town.success.created", name, player.name)

                        return@executes Command.SINGLE_SUCCESS
                    }
                )
                .executes { ctx -> ChatHelper.sendMessage(ctx, "town.help.create") }
        }

        private fun info(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("info")
                .then(Commands
                    .argument("name", StringArgumentType.string())
                    .executes { ctx ->
                        val name = StringArgumentType.getString(ctx, "name")
                        val town = TownManager.getTownByName(name)
                            ?: return@executes ChatHelper.sendMessage(ctx, "town.error.name_not_found", name)

                        ChatHelper.sendMessage(
                            ctx,
                            "town.info",
                            town.name,
                            if(town.mayor != null)
                                Bukkit.getOfflinePlayer(town.mayor!!).name
                            else
                                "No-one",
                            Bukkit.getOfflinePlayer(town.founder).name,
                            town.money,
                            town.getCostOfNewPlot(),
                            town.players.size
                        )
                    })
                .executes { ctx ->
                    if (ctx.source.sender is Player) {
                        val player = ctx.source.sender as Player
                        val town = TownManager.getTownOfPlayer(player)
                        if(town != null) {
                            ChatHelper.sendMessage(
                                ctx,
                                "town.info",
                                town.name,
                                if(town.mayor != null)
                                    Bukkit.getOfflinePlayer(town.mayor!!).name
                                else
                                    "No-one",
                                Bukkit.getOfflinePlayer(town.founder).name,
                                town.money,
                                town.getCostOfNewPlot(),
                                town.players.size
                            )
                            return@executes Command.SINGLE_SUCCESS
                        }
                    }
                    ChatHelper.sendMessage(ctx, "town.help.info")
                }
        }

        fun town(): LiteralCommandNode<CommandSourceStack> {
            return Commands
                .literal("town")
                .then(create())
                .then(donate())
                .then(invites())
                .then(invite())
                .then(join())
                .then(decline())
                .then(leave().build())
                .then(info())
                .then(color())
                .then(AdminCommands.modify().build())
                .executes { ctx -> ChatHelper.sendMessage(ctx, "town.help") }
                .build()
        }

        fun acquire(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("acquire")
                .requires { source -> source.sender is Player }
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val town = TownManager.getTownOfPlayer(player)
                        ?: return@executes ChatHelper.sendMessage(ctx, "town.error.not_in_a_town")

                    val owner = PlotGuard.getChunkOwner(player.chunk)
                    if(owner != null)
                        return@executes ChatHelper.sendMessage(ctx, "town.error.plot_already_occupied", owner.name)

                    val cost = town.getCostOfNewPlot()
                    if(town.money < cost)
                        return@executes ChatHelper.sendMessage(ctx, "town.error.insufficient_funds")

                    PlotGuard.setChunkOwner(player.chunk, town)
                    town.money -= cost
                    town.plots++
                    town.broadcast(ChatHelper.getMessage("town.success.plot_acquired", player.name)!!)

                    val render = Misc.displayChunkBorder(
                        player,
                        player.chunk,
                        Misc.getColoredBlock(town.color),
                        player.y,
                        gap = 2
                    )

                    val scheduler = Bukkit.getScheduler()
                    scheduler.runTaskLater(Main.instance!!, Runnable {
                        render.forEach { entity -> entity.remove() }
                    }, 20 * 5)
                    player.playSound(Sound
                        .sound()
                        .type(Key.key("minecraft:entity.firework_rocket.launch"))
                        .source(Sound.Source.PLAYER)
                        .volume(100.0f)
                        .pitch(1.0f)
                        .build()
                    )

                    return@executes Command.SINGLE_SUCCESS
                }
        }

        private fun toggle_display(): LiteralCommandNode<CommandSourceStack> {
            return ChatCommand("toggle_display")
                .requirePlayerSender()
                .argument("enabled", BoolArgumentType.bool())
                .executes { ctx ->
                    val enabled = BoolArgumentType.getBool(ctx, "enabled")
                    val player = ctx.source.sender as Player
                    val pdc = player.persistentDataContainer
                    val key = NamespacedKey(Main.instance!!, "prefs.plot.display")
                    pdc.set(key, PersistentDataType.BOOLEAN, enabled)

                    ChatHelper.sendMessage(ctx, "plot.success.toggled_display", enabled)
                }
                .build()
        }

        private fun toggle_alert(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("toggle_alert")
                .requires { ctx -> ctx.sender is Player }
                .then(Commands
                    .argument("value", StringArgumentType.string())
                    .executes { ctx ->
                        val value = StringArgumentType.getString(ctx, "value")
                        if(
                            !value.equals("always", ignoreCase = true) &&
                            !value.equals("never", ignoreCase = true) &&
                            !value.equals("changed", ignoreCase = true)
                        )
                            return@executes ChatHelper.sendMessage(ctx, "plot.help.toggle_alert")

                        val player = ctx.source.sender as Player
                        val pdc = player.persistentDataContainer
                        val key = NamespacedKey(Main.instance!!, "message-show-pref")
                        pdc.set(
                            key,
                            PersistentDataType.INTEGER,
                            when(value.lowercase()) {
                                "changed" -> 0
                                "always" -> 1
                                "never" -> -1
                                else -> 0
                            }
                        )

                        ChatHelper.sendMessage(ctx, "plot.success.toggled_alert", value)
                    })
        }

        private fun toggle_autobuy(): LiteralCommandNode<CommandSourceStack> {
            return ChatCommand("toggle_autobuy")
                .argument("enabled", BoolArgumentType.bool())
                .requirePlayerSender()
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val pdc = player.persistentDataContainer
                    val key = NamespacedKey(Main.instance!!, "prefs.plot.autobuy")
                    val enabled = BoolArgumentType.getBool(ctx, "enabled")
                    pdc.set(key, PersistentDataType.BOOLEAN, enabled)

                    ChatHelper.sendMessage(
                        ctx,
                        if(enabled)
                            "plot.success.autobuy_enabled"
                        else
                            "plot.success.autobuy_disabled"
                    )
                }
                .build()
        }

        fun plot(): LiteralCommandNode<CommandSourceStack> {
            return Commands
                .literal("plot")
                .then(acquire())
                .then(toggle_alert())
                .then(toggle_display())
                .then(toggle_autobuy())
                .executes { ctx -> ChatHelper.sendMessage(ctx, "town.help") }
                .build()
        }
    }
}