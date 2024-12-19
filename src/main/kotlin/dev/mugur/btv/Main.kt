package dev.mugur.btv

import dev.mugur.btv.misc.ChatPrefix
import dev.mugur.btv.misc.EndDisabler
import dev.mugur.btv.misc.Graveyard
import dev.mugur.btv.misc.MiscCommands
import dev.mugur.btv.towns.*
import dev.mugur.btv.towns.interact.TownObjectListener
import dev.mugur.btv.utils.ChatHelper
import dev.mugur.btv.utils.Database
import dev.mugur.btv.utils.events.EventCaller
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    companion object {
        var instance: Main? = null
            private set
    }

    override fun onEnable() {
        instance = this

        saveDefaultConfig()

        Database.init()
        Database.sync()
        ChatHelper.init()
        TownManager.init()

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar = event.registrar()
            registrar.register(TownCommands.town())
            registrar.register(TownCommands.plot())
            registrar.register(MiscCommands.root())
        }

        val pluginManager = server.pluginManager
        pluginManager.registerEvents(EventCaller(), this)
        pluginManager.registerEvents(TownObjectListener(), this)
        pluginManager.registerEvents(PlotChecker(), this)
        pluginManager.registerEvents(PlotDisplayer(), this)
        pluginManager.registerEvents(MessageBroadcaster(), this)
        pluginManager.registerEvents(EndDisabler(), this)
        pluginManager.registerEvents(ChatPrefix(), this)
        pluginManager.registerEvents(Graveyard(), this)
    }

    override fun onDisable() {
        componentLogger.info(Component.text("Bye!"))
    }
}
