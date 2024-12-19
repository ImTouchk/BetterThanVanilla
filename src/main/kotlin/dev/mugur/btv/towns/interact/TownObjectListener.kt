package dev.mugur.btv.towns.interact

import dev.mugur.btv.Main
import dev.mugur.btv.towns.TownManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class TownObjectListener : Listener {
    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        tryPlaceObject(e)
        tryUseObject(e)
    }

    private fun tryUseObject(e: PlayerInteractEvent): Boolean {
        val clickedBlock = e.clickedBlock ?: return false
        if(clickedBlock.type == Material.AIR)
            return false

        val state = clickedBlock.state
        if(state !is TileState)
            return false

        val pdc = state.persistentDataContainer
        val townKey = NamespacedKey(Main.instance!!, "town-id")

        val townId = pdc.get(townKey, PersistentDataType.STRING) ?: return false
        val town = TownManager.getTownById(townId) ?: return false

        val loc = clickedBlock.location
        for(obj in town.objects) {
            if(obj.pos.toBlockLocation() != loc.toBlockLocation())
                continue

            if(e.action == Action.LEFT_CLICK_BLOCK) {
                obj.type.showInfo(e.player)
            } else {
                obj.type.onInteract?.invoke(e.player, obj)
            }

            e.isCancelled = true
            return true
        }

        Main.instance!!.componentLogger.error("Couldn't find TOWN_OBJECT")
        return false
    }

    private fun tryPlaceObject(e: PlayerInteractEvent): Boolean {
        val clickedBlock = e.clickedBlock ?: return false
        if(clickedBlock.type == Material.AIR)
            return false

        val inventory = e.player.inventory
        val type = TownObjectType.fromItemStack(inventory.itemInMainHand)
            ?: return false

        val player = e.player
        val town = TownManager.getTownOfPlayer(player) ?: return false

        val dest = clickedBlock.getRelative(e.blockFace)
        dest.type = type.material

        val state = dest.state as TileState
        val pdc = state.persistentDataContainer
        val key = NamespacedKey(Main.instance!!, "town-id")
        pdc.set(key, PersistentDataType.STRING, town.id.toString())
        state.update()

        TownObject(town, type, dest.location)

        inventory.setItemInMainHand(null)
        e.isCancelled = true
        return true
    }
}