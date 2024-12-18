package dev.mugur.btv.misc

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.mugur.btv.Main
import dev.mugur.btv.utils.ChatHelper
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent

class EndDisabler : Listener {
    private var enabled = Main
        .instance!!
        .config
        .getBoolean("misc.disable-end")
        set(value) {
            field = value
            Main
                .instance!!
                .config
                .set("misc.disable-end", value)
        }

    companion object {
        lateinit var instance: EndDisabler

        fun command(): LiteralArgumentBuilder<CommandSourceStack> {
            return Commands
                .literal("enable_end")
                .requires { ctx -> ctx.sender.isOp }
                .then(Commands
                    .argument("value", BoolArgumentType.bool())
                    .executes { ctx ->
                        val newValue = !BoolArgumentType.getBool(ctx, "value")
                        if(instance.enabled != newValue) {
                            instance.enabled = newValue
                        }

                        ChatHelper.broadcastMessage(
                            if(instance.enabled)
                                "misc.success.end_disabled"
                            else
                                "misc.success.end_enabled"
                        )

                        Command.SINGLE_SUCCESS
                    }
                )
        }
    }

    init { instance = this }

    @EventHandler
    fun onEnterPortal(e: PlayerPortalEvent) {
        if(e.cause != PlayerTeleportEvent.TeleportCause.END_PORTAL)
            return

        if(!enabled)
            return

        ChatHelper.sendActionBar(e.player, "misc.end_disabled")
        e.isCancelled = true
    }
}
