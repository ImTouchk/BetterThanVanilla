package dev.mugur.btv.towns

import dev.mugur.btv.Main
import dev.mugur.btv.utils.ChatHelper
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType

class PlotChecker : Listener {
    private val autobuyKey = NamespacedKey(Main.instance!!, "plot-autobuy")
    private val showPrefKey = NamespacedKey(Main.instance!!, "message-show-pref")
    private val lastOwnerKey = NamespacedKey(Main.instance!!, "last-chunk-owner")
    private val chunkKey = NamespacedKey(Main.instance!!, "current-chunk")

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val player = e.player
        val showPref = getShowPreference(player)
        val changedChunks = didPlayerChangeChunks(player)
        val differentChunkOwner = hasDifferentChunkOwner(player)

        if(changedChunks && differentChunkOwner) {
            val town = TownManager.getTownOfPlayer(player)

            val pdc = player.persistentDataContainer
            val autobuy = pdc.get(autobuyKey, PersistentDataType.BOOLEAN)
            if(
                town != null &&
                town.money >= town.getCostOfNewPlot() &&
                autobuy == true
            ) {
                val owner = PlotGuard.getChunkOwner(player.chunk)
                if(owner == null)
                    player.performCommand("plot acquire")
            }
        }

        if(showPref == -1 || !changedChunks)
            return

        if(showPref != 1 && !differentChunkOwner)
            return

        val town = PlotGuard.getChunkOwner(player.chunk)
        val name = town?.name ?: "the Wilderness"
        val color = town?.color?.asHexString() ?: "aqua"

        ChatHelper.sendActionBar(player, "town.area.entered", color, name)
    }

    private fun getShowPreference(player: Player): Int {
        val pdc = player.persistentDataContainer
        return pdc.get(showPrefKey, PersistentDataType.INTEGER) ?: 0
    }

    private fun hasDifferentChunkOwner(player: Player): Boolean {
        val chunkOwner = PlotGuard.getChunkOwner(player.chunk)

        val pdc = player.persistentDataContainer

        val last = pdc.get(lastOwnerKey, PersistentDataType.STRING) ?: "wilderness"
        val current = chunkOwner?.id.toString() ?: "wilderness"
        pdc.set(lastOwnerKey, PersistentDataType.STRING, current)

        return !current.equals(last, ignoreCase = true)
    }

    private fun didPlayerChangeChunks(player: Player): Boolean {
        val chunk = player.chunk
        val pdc = player.persistentDataContainer
        val current = arrayOf(chunk.x, chunk.z)
        val last =
            if (pdc.has(chunkKey))
                pdc.get(chunkKey, PersistentDataType.INTEGER_ARRAY)?.toTypedArray()!!
            else
                current.clone()

        pdc.set(chunkKey, PersistentDataType.INTEGER_ARRAY, current.toIntArray())
        return current[0] != last[0] || current[1] != last[1]
    }
}