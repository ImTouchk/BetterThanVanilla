package dev.mugur.btv.towns

import dev.mugur.btv.Main
import dev.mugur.btv.utils.Misc
import dev.mugur.btv.utils.events.PlayerEnterChunkEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
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
    fun onPlayerMove(e: PlayerEnterChunkEvent) {
        if(!Misc.getPlayerPref(e.player, "prefs.plot.display")) {
            val entities = displayData[e.player.uniqueId]
            if(entities != null) {
                entities.forEach { data -> data.entities.forEach { it.remove() } }
                displayData.remove(e.player.uniqueId)
            }
            return
        }

        val entities = displayData.getOrPut(e.player.uniqueId) { mutableListOf() }
        val neighbors = Misc.getChunksInArea(e.chunk)
        for(chunkData in entities) {
            if(
                abs(chunkData.x - e.chunk.x) <= 3 &&
                abs(chunkData.z - e.chunk.z) <= 3
            ) continue

            chunkData.entities.forEach { it.remove() }
            entities.remove(chunkData)
        }

        val height =
            if(entities.isNotEmpty())
                entities[0].entities[0].height
            else
                e.player.y

        for(chunk in neighbors) {
            if(entities.find { it.x == chunk.x && it.z == chunk.z } != null)
                continue

            val town = PlotGuard.getChunkOwner(chunk) ?: continue
            val material = Misc.getColoredBlock(town.color)

            entities.add(ChunkDisplayData(
                chunk.x,
                chunk.z,
                Misc.displayChunkBorder(e.player, chunk, material, height, gap = 2)
            ))
        }
    }

    companion object {
        private fun makeDisplayEntity(
            world: World,
            location: Location,
            player: Player,
            scale: Vector3f,
            material: Material
        ): BlockDisplay {
            return world.spawn(location, BlockDisplay::class.java) { entity ->
                entity.block = material.createBlockData()
                entity.isPersistent = false
                entity.isVisibleByDefault = false
                entity.transformation = Transformation(
                    Vector3f(),
                    AxisAngle4f(),
                    scale,
                    AxisAngle4f()
                )

                player.showEntity(Main.instance!!, entity)
            }
        }

        fun render(
            player: Player,
            chunk: Chunk,
            material: Material = Material.TERRACOTTA
        ): List<BlockDisplay> {
            val world = player.world
            val owner = PlotGuard.getChunkOwner(chunk)
                ?: return listOf()

            val town = TownManager.getTownOfPlayer(player)
                ?: return listOf()

            if(town.id != owner.id)
                return listOf()

            val begX = chunk.x * 16.0
            val endX = (chunk.x + 1) * 16.0

            val begZ = chunk.z * 16.0
            val endZ = (chunk.z + 1) * 16.0

            val location1 = Location(player.world, begX, player.y, begZ)
            val location2 = Location(player.world, endX, player.y, endZ)

            return listOf(
                makeDisplayEntity(world, location1, player, Vector3f(16.0f, 0.250f, 0.250f), material),
                makeDisplayEntity(world, location1, player, Vector3f(0.250f, 0.250f, 16.0f), material),
                makeDisplayEntity(world, location2, player, Vector3f(-16.0f, 0.250f, 0.250f), material),
                makeDisplayEntity(world, location2, player, Vector3f(0.250f, 0.250f, -16.0f), material)
            )
        }
    }
}