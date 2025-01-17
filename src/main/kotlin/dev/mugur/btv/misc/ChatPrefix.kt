package dev.mugur.btv.misc

import dev.mugur.btv.towns.TownManager
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ChatPrefix : Listener, ChatRenderer {
    @EventHandler
    fun onChat(e: AsyncChatEvent) {
        e.renderer(this)
    }

    @Override
    override fun render(
        source: Player,
        sourceDisplayName: Component,
        message: Component,
        viewer: Audience
    ): Component {
        var finalText = Component.text("")

        if(source.isOp)
            finalText = finalText.append(
                Component
                    .text("[OP] ")
                    .decoration(TextDecoration.BOLD, true)
                    .color(NamedTextColor.RED)
            )

        finalText = finalText.append(
            Component
                .text("<")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false)
        )

        val dept = source.getDepartment()
        if(dept != Department.None) {
            finalText = finalText.append(
                Component
                    .text("${dept.name} ")
                    .color(dept.color)
                    .decoration(TextDecoration.BOLD, true)
            )
        }

        val town = TownManager.getTownOfPlayer(source)
        if(town != null) {
            finalText = finalText.append(
                Component
                    .text("${town.name} ")
                    .color(town.color)
                    .decoration(TextDecoration.BOLD, false)
            )
        }

        finalText = finalText.append(
            sourceDisplayName
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false)
        )
        finalText = finalText.append(Component.text("> "))

        val msg = PlainTextComponentSerializer
            .plainText()
            .serialize(message)

        finalText = if(msg.startsWith('>'))
            finalText.append(message.color(NamedTextColor.GREEN))
        else
            finalText.append(message)
        return finalText
    }
}