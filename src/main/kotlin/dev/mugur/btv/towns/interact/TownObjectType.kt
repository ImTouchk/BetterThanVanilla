package dev.mugur.btv.towns.interact

import dev.mugur.btv.Main
import dev.mugur.btv.towns.Town
import dev.mugur.btv.utils.ChatHelper
import dev.mugur.btv.utils.Misc
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

enum class TownObjectType(
    val id: Int,
    val resName: String,
    val material: Material,
    val onInteract: ((Player, TownObject) -> Unit)? = null
) {
    DonateChest(1, "donate_chest", Material.BARREL, handler@{ player, obj ->
        val item = player.inventory.itemInMainHand
        if(item.type != Misc.getCurrencyItem()) {
            obj.type.showInfo(player)
            return@handler
        }

        player.performCommand("town donate ${item.amount}")
    });

    fun showInfo(to: Player) {
        to.sendMessage(
            Component.text("Clicked object:")
                .append(ChatHelper.getResource("town.objects.${resName}.desc"))
        )
    }

    fun toItemStack(): ItemStack {
        val item = ItemStack(Material.TURTLE_EGG)
        val meta = item.itemMeta!!
        meta.displayName(ChatHelper.getResource("town.objects.${resName}.title"))

        val lore = meta.lore()
        lore?.clear()
        lore?.add(ChatHelper.getResource("town.objects.${resName}.desc"))

        val pdc = meta.persistentDataContainer
        val key = NamespacedKey(Main.instance!!, "town-object")
        pdc.set(key, PersistentDataType.INTEGER, id)

        item.itemMeta = meta
        return item
    }

    companion object {
        fun fromItemStack(item: ItemStack): TownObjectType? {
            val meta = item.itemMeta ?: return null
            val pdc = meta.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, "town-object")
            val id = pdc.get(key, PersistentDataType.INTEGER) ?: return null
            return fromId(id)
        }

        fun fromId(id: Int): TownObjectType {
            return entries.first { it.id == id }
        }
    }
}