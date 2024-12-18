package dev.mugur.btv.misc

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import dev.mugur.btv.Main
import dev.mugur.btv.utils.ChatCommand
import dev.mugur.btv.utils.ChatHelper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.*

class Graveyard : Listener {
    private val graveyardKey = NamespacedKey(Main.instance!!, "is-graveyard")
    private val killerKey = NamespacedKey(Main.instance!!, "killer")
    private val ownerKey = NamespacedKey(Main.instance!!, "owner")
    private val xpKey = NamespacedKey(Main.instance!!, "xp")
    private var enabled: Boolean = Main
        .instance!!
        .config
        .getBoolean("misc.enable-graveyard")
        set(value) {
            field = value
            Main.instance!!.config.set("misc.enable-graveyard", field)
        }


    companion object {
        lateinit var instance: Graveyard

        fun command(): ChatCommand {
            return ChatCommand("enable_graveyard")
                .require({ sender -> sender.isOp }, "misc.error.not_op")
                .argument("value", BoolArgumentType.bool())
                .executes { ctx ->
                    val value = BoolArgumentType.getBool(ctx, "value")
                    instance.enabled = value

                    ChatHelper.broadcastMessage(
                        if(instance.enabled)
                            "graveyard.success.enabled"
                        else
                            "graveyard.success.disabled"
                    )
                    Command.SINGLE_SUCCESS
                }
        }
    }

    init { instance = this }

    private fun getChestPosition(location: Location): Location? {
        val MAX_RADIUS = 5

        val list = mutableListOf<Vector>()
        for(dx in -MAX_RADIUS..MAX_RADIUS)
            for(dy in -MAX_RADIUS..MAX_RADIUS)
                for(dz in -MAX_RADIUS..MAX_RADIUS)
                    list.add(Vector(dx, dy, dz))

        list.sortBy { (it.x * it.x) + (it.y * it.y) + (it.z * it.z) }

        var i = 0
        var chestLoc: Location = location
        while(chestLoc.block.type != Material.AIR && i < list.size) {
            chestLoc = location.add(list[i])
            i++
        }

        return if(chestLoc.block.type == Material.AIR) chestLoc else null
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if(!enabled)
            return

        if(e.clickedBlock?.type != Material.CHEST)
            return

        val player = e.player
        val block = e.clickedBlock!!
        val chest = block.state as Chest
        val pdc = chest.persistentDataContainer
        if(!pdc.has(graveyardKey))
            return

        val ownerId = UUID.fromString(pdc.get(ownerKey, PersistentDataType.STRING)!!)
        val killerId =
            if(pdc.has(killerKey))
                UUID.fromString(pdc.get(killerKey, PersistentDataType.STRING))
            else
                null

        val owner = Bukkit.getOfflinePlayer(ownerId)
        val killer = if(killerId != null) Bukkit.getOfflinePlayer(killerId) else null
        if(player.uniqueId != ownerId && player.uniqueId != killerId) {
            ChatHelper.sendMessage(
                player,
                "graveyard.error.insufficient_permissions",
                if(killer != null)
                    "${owner.name}, ${killer.name}"
                else
                    owner.name
            )
            return
        }

        val xp = pdc.get(xpKey, PersistentDataType.INTEGER)!!
        player.giveExp(xp)

        val location = chest.location
        for(i in 0..<chest.blockInventory.size) {
            val item = chest.blockInventory.getItem(i) ?: continue
            location.world.dropItemNaturally(location, item.clone())
        }

        block.type = Material.AIR
        ChatHelper.sendMessage(player, "graveyard.success.claimed", owner.name)
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        if(!enabled)
            return

        val chestPosition = getChestPosition(e.entity.location)
        if(chestPosition == null) {
            ChatHelper.sendMessage(e.player, "graveyard.error.no_safe_spot")
            return
        }

        val player = e.player

        chestPosition.block.type = Material.CHEST
        val chest = chestPosition.block.state as Chest

        val pdc = chest.persistentDataContainer
        pdc.set(graveyardKey, PersistentDataType.BOOLEAN, true)
        pdc.set(ownerKey, PersistentDataType.STRING, player.uniqueId.toString())
        pdc.set(xpKey, PersistentDataType.INTEGER, e.droppedExp)

        val killer = e.player.killer
        if(killer != null) {
            pdc.set(killerKey, PersistentDataType.STRING, killer.uniqueId.toString())
            ChatHelper.sendMessage(
                killer,
                "graveyard.kill_reward",
                player.name,
                chestPosition.blockX, chestPosition.blockY, chestPosition.blockZ
            )
        }
        chest.update()

        for(item in e.drops)
            chest.blockInventory.addItem(item.clone())

        e.drops.clear()
        e.droppedExp = 0

        ChatHelper.sendMessage(
            player,
            "graveyard.items_saved",
            chestPosition.blockX, chestPosition.blockY, chestPosition.blockZ
        )
    }
}