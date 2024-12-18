package dev.mugur.btv.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class GuiUtils(
    val slot: (Int, ItemStack, InventoryClickEvent.() -> Unit) -> Unit
) {
    private var current = 1

    fun next(
        item: ItemStack,
        onClick: InventoryClickEvent.() -> Unit
    ) {
        slot(current, item, onClick)
        current++
    }

    fun conditional(
        condition: Boolean,
        item: ItemStack,
        onClick: InventoryClickEvent.() -> Unit
    ) {
        if(!condition)
            return

        next(item, onClick)
    }

    fun playerList(onClick: (Player) -> Unit): Int {
        var i = 0
        val players = Bukkit.getOnlinePlayers()
        for(player in players) {
            slot(i, playerHead(player)) { onClick(player) }
            i++
        }
        return i
    }

    companion object {
        fun detailed(item: ItemStack, name: String, description: String, vararg args: Any?): ItemStack {
            val meta = item.itemMeta!!
            meta.displayName(ChatHelper.getResource(name, *args))
            meta.lore(listOf(ChatHelper.getResource(description)))
            item.itemMeta = meta
            return item
        }

        fun detailed(material: Material, name: String, description: String): ItemStack {
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.displayName(ChatHelper.getResource(name))
            meta.lore(listOf(ChatHelper.getResource(description)))
            item.itemMeta = meta
            return item
        }

        fun named(material: Material, name: String, vararg args: Any?): ItemStack {
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.displayName(ChatHelper.getResource(name, *args))
            item.itemMeta = meta
            return item
        }

        fun playerHead(player: OfflinePlayer): ItemStack {
            val stack = ItemStack(Material.PLAYER_HEAD)
            val meta = stack.itemMeta as SkullMeta
            meta.displayName(Component
                .text(player.name!!)
                .decoration(TextDecoration.ITALIC, true)
            )
            meta.owningPlayer = player
            stack.itemMeta = meta
            return stack
        }

        fun playerHead(player: Player): ItemStack {
            return playerHead(player)
        }
    }
}