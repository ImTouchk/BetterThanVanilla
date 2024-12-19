package dev.mugur.btv.utils.events

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerMoveEvent

class PlayerEnterChunkEvent(
    player: Player,
    from: Location,
    to: Location,
    val chunk: Chunk,
    val prev: Chunk
) : PlayerMoveEvent(player, from, to) {
    companion object {
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        @Override
        @Suppress("ACCIDENTAL_OVERRIDE")
        fun getHandlerList(): HandlerList {
            return HANDLER_LIST
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}