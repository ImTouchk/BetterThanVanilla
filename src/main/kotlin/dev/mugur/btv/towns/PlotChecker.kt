package dev.mugur.btv.towns

import dev.mugur.btv.Main
import dev.mugur.btv.utils.ChatHelper
import dev.mugur.btv.utils.Misc
import dev.mugur.btv.utils.events.PlayerEnterChunkEvent
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType

class PlotChecker : Listener {
    private val showPrefKey = NamespacedKey(Main.instance!!, "message-show-pref")

    @EventHandler
    fun onPlayerEnterChunk(e: PlayerEnterChunkEvent) {
        PlotGuard.updateWorldGuardRegion(e.chunk)
        tryShowAlert(e)
    }

    private fun tryShowAlert(e: PlayerEnterChunkEvent) {
        val owner = PlotGuard.getChunkOwner(e.chunk)
        val prevOwner = PlotGuard.getChunkOwner(e.prev)
        val differentChunkOwner = owner != prevOwner

        val player = e.player
        val autobuyEnabled = Misc.getPlayerPref(player, "prefs.plot.autobuy")

        if(differentChunkOwner && autobuyEnabled) {
            val town = TownManager.getTownOfPlayer(player)
            if(
                owner == null &&
                town != null &&
                town.money >= town.getCostOfNewPlot()
            ) player.performCommand("plot acquire")
        }

        val showPref = getShowPreference(player)
        if(showPref == -1 || (showPref == 0 && !differentChunkOwner))
            return

        val town = PlotGuard.getChunkOwner(e.chunk)
        val name = town?.name ?: "the Wilderness"
        val color = town?.color?.asHexString() ?: "aqua"

        ChatHelper.sendActionBar(player, "town.area.entered", color, name)
    }

    private fun getShowPreference(player: Player): Int {
        val pdc = player.persistentDataContainer
        return pdc.get(showPrefKey, PersistentDataType.INTEGER) ?: 0
    }
}