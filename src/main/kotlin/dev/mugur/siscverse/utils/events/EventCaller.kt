package dev.mugur.siscverse.utils.events

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class EventCaller : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(e: PlayerMoveEvent) {
        val previous = e.from.chunk
        val next = e.to.chunk
        if(previous != next) {
            val chunkEvent = PlayerEnterChunkEvent(e.player, e.from, e.to, e.to.chunk)
            chunkEvent.callEvent()
        }
    }
}