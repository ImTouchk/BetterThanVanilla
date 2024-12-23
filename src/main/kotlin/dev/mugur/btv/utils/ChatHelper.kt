package dev.mugur.btv.utils

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import dev.mugur.btv.Main
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ChatHelper {
    companion object {
        private val list = mutableListOf<Document>()

        fun init() {
            loadResource("messages/town.xml")
            loadResource("messages/town/objects.xml")
            loadResource("messages/misc.xml")
            loadResource("messages/graveyard.xml")
            loadResource("messages/dept.xml")

            Main
                .instance
                ?.componentLogger
                ?.debug("Loaded ${list.size} XML chat resources")
        }

        fun sendActionBar(player: Player, name: String, vararg args: Any?) {
            val message = getMessage(name, *args)
            player.sendActionBar(message)
        }

        fun broadcastMessage(name: String, vararg args: Any?) {
            val message = getMessage(name, *args)
            Bukkit.broadcast(message)
        }

        fun sendMessage(
            ctx: CommandContext<CommandSourceStack>,
            name: String,
            vararg args: Any?
        ): Int {
            val message = getMessage(name, *args)
            ctx
                .source
                .sender
                .sendMessage(message)

            return Command.SINGLE_SUCCESS
        }

        fun serialize(message: Component): String {
            return MiniMessage
                .miniMessage()
                .serialize(message)
        }

        fun sendMessage(player: Player, name: String, vararg args: Any?) {
            val message = getMessage(name, *args)
            player.sendMessage(message)
        }

        fun getResource(name: String, vararg args: Any?): Component {
            return getMessage(name, *args)
        }

        fun getMessage(name: String, vararg args: Any?): Component {
            val raw = getRawMessage(name)
                ?.replace("<newline>\\s+","<newline>")
                ?.replace("\\s+".toRegex(), " ")
                ?.replace("\n+".toRegex(), "")

            if(raw == null) {
                Main.instance!!
                    .componentLogger
                    .error("Tried to retrieve unknown chat message '$name'.")

                return Component
                    .text("Internal error: $name")
                    .color(NamedTextColor.DARK_RED)
            }
            try {
                val formatted = String.format(raw, *args)
                return MiniMessage
                    .miniMessage()
                    .deserialize(formatted)
            } catch (e: Exception) {
                e.printStackTrace()
                return Component
                    .text("Internal error: $name")
                    .color(NamedTextColor.DARK_RED)
            }
        }

        private fun getRawMessage(name: String): String? {
            list.forEach { doc ->
                val root = doc
                    .getElementsByTagName("messages")
                    .item(0)
                    as Element

                val messages = root.getElementsByTagName("message")
                for(i in 0 until messages.length) {
                    val message = messages.item(i) as Element
                    val id = message
                        .getElementsByTagName("id")
                        .item(0)
                        .textContent

                    if(!name.equals(id, ignoreCase = true))
                        continue

                    return message
                        .getElementsByTagName("value")
                        .item(0)
                        .textContent
                }
            }

            Main
                .instance
                ?.componentLogger
                ?.error("Could not find message string \"$name\".")

            return null
        }

        fun loadResource(name: String) {
            val stream = Main.instance?.getResource(name) ?: return
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(stream)
            doc.normalize()
            list.add(doc)

            Main.instance
                ?.componentLogger
                ?.debug("Loaded XML resource \"$name\"")
        }
    }
}