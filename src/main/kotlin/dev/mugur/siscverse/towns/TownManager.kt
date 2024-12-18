package dev.mugur.siscverse.towns

import dev.mugur.siscverse.Main
import dev.mugur.siscverse.utils.Database
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.*

class TownManager {
    companion object {
        val INVITE_EXPIRATION_TIME = 1000 * 60 * 15
        val towns = mutableListOf<Town>()

        fun getPlayerTownInvites(player: Player): List<Town> {
            val list = mutableListOf<Town>()
            val pdc = player.persistentDataContainer
            for (town in towns) {
                val key = NamespacedKey(Main.instance!!, town.id.toString())
                val time = pdc.get(key, PersistentDataType.LONG)
                    ?: continue

                val current = System.currentTimeMillis()
                if(current - time > INVITE_EXPIRATION_TIME)
                    continue

                list.add(town)
            }
            return list
        }

        fun isTownMayor(player: Player): Boolean {
            val town = getTownOfPlayer(player) ?: return false
            return town.mayor == player.uniqueId
        }

        fun getTownOfPlayer(player: Player): Town? {
            return towns.find { it.players.contains(player.uniqueId) }
        }

        fun getTownById(id: String): Town? {
            val converted = UUID.fromString(id)
            return towns.find { it.id == converted }
        }

        fun getTownByName(name: String) : Town? {
            return towns.find { it.name == name }
        }

        fun init() {
            towns.clear()

            val res = Database.query("SELECT * FROM town;") ?: return
            while(res.next()) {
                val town = Town(res)
                towns.add(town)
            }
        }
    }
}