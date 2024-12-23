package dev.mugur.btv.misc

import dev.mugur.btv.Main
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

enum class Department(val id: Int, val color: TextColor) {
    None(-1, NamedTextColor.WHITE),
    EDU(1, NamedTextColor.BLUE),
    HR(2, NamedTextColor.GOLD),
    FR(3, NamedTextColor.DARK_PURPLE),
    IP(4, NamedTextColor.RED),
    IT(5, NamedTextColor.GREEN);

    companion object {
        fun getFromName(name: String): Department? {
            return try { valueOf(name.uppercase()) }
            catch (_: IllegalArgumentException) { null }
        }

        fun getFromId(id: Int): Department {
            return entries.find { it.id == id }!!
        }

        fun getPlayerDept(player: Player): Department {
            val pdc = player.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, "dept")
            return if(!pdc.has(key))
                None
            else
                getFromId(pdc.get(key, PersistentDataType.INTEGER)!!)
        }
    }

    fun setForPlayer(player: Player) {
        val pdc = player.persistentDataContainer
        val key = NamespacedKey(Main.instance!!, "dept")
        pdc.set(key, PersistentDataType.INTEGER, id)
    }
}

fun Player.setDepartment(dept: Department) {
    dept.setForPlayer(this)
}

fun Player.getDepartment(): Department {
    return Department.getPlayerDept(this)
}