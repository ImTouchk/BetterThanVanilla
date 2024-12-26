package dev.mugur.btv.towns.interact

import com.mojang.brigadier.Command
import dev.mugur.btv.utils.ChatCommand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class TownTracker : Listener {

    companion object {
        fun command(): ChatCommand {
            return ChatCommand("track")
                .requirePlayerSender()
                .executes { ctx ->

                    Command.SINGLE_SUCCESS
                }
        }
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {

    }
}