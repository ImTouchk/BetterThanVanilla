package dev.mugur.btv.towns

import dev.mugur.btv.utils.Misc
import dev.mugur.btv.utils.events.PlayerEnterChunkEvent
import org.bukkit.entity.BlockDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import kotlin.math.abs

class PlotDisplayer : Listener {
    private data class ChunkDisplayData(
        val x: Int,
        val z: Int,
        val entities: List<BlockDisplay>
    )

    private val displayData = mutableMapOf<UUID, MutableList<ChunkDisplayData>>()

    @EventHandler
    fun onPlayerEnterChunk(e: PlayerEnterChunkEvent) {
        if(!Misc.getPlayerPref(e.player, "prefs.plot.display")) {
            val entities = displayData[e.player.uniqueId]
            if(entities != null) {
                entities.forEach { data -> data.entities.forEach { it.remove() } }
                displayData.remove(e.player.uniqueId)
            }
            return
        }

        val entities = displayData.getOrPut(e.player.uniqueId) { mutableListOf() }
        entities.forEach { chunkData -> chunkData.entities.forEach { it.remove() }}
        entities.clear()

        val neighbors = Misc.getChunksInArea(e.chunk)

        for(chunk in neighbors) {
            val town = PlotGuard.getChunkOwner(chunk) ?: continue
            val material = Misc.getColoredBlock(town.color)
            entities.add(ChunkDisplayData(
                chunk.x,
                chunk.z,
                Misc.displayChunkBorder(e.player, chunk, material, e.player.y, gap = 2)
            ))
        }
    }
}