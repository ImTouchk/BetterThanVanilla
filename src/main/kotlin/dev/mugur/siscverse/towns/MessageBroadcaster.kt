package dev.mugur.siscverse.towns

import dev.mugur.siscverse.Main
import dev.mugur.siscverse.utils.ChatHelper
import dev.mugur.siscverse.utils.Database
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType

class MessageBroadcaster : Listener {
    private val key = NamespacedKey(Main.instance!!, "last-online")

    private fun getMissedMessages(lastOnline: Long, town: Town): List<Component> {
        val stmt = Database.prepare("SELECT * FROM town_message WHERE town_id = ? AND created_at > ?;")
        stmt.setString(1, town.id.toString())
        stmt.setLong(2, lastOnline)

        val res = stmt.executeQuery()
        val list = mutableListOf<Component>()
        while(res.next()) {
            val message = res.getString("message")
            list.add(MiniMessage
                .miniMessage()
                .deserialize(message)
            )
        }
        return list
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val player = e.player
        val pdc = player.persistentDataContainer
        pdc.set(key, PersistentDataType.LONG, System.currentTimeMillis())
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val player = e.player
        val town = TownManager.getTownOfPlayer(player) ?: return

        val pdc = player.persistentDataContainer
        val lastOnline = pdc.get(key, PersistentDataType.LONG) ?: 0
        val messages = getMissedMessages(lastOnline, town)

        ChatHelper.sendMessage(player, "town.player.missed_messages", messages.size)
        for(msg in messages)
            player.sendMessage(msg)
    }
}

