package dev.mugur.siscverse.towns

import dev.mugur.siscverse.Main
import dev.mugur.siscverse.utils.events.PlayerEnterChunkEvent
import net.kyori.adventure.text.format.NamedTextColor
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

    private val displayKey = NamespacedKey(Main.instance!!, "prefs.plot.display")
    private val displayEntities = mutableMapOf<UUID, MutableList<ChunkDisplayData>>()

    private fun hasEntityForChunk(list: List<ChunkDisplayData>, x: Int, z: Int): Boolean {
        for(entity in list)
            if(entity.x == x && entity.z == z)
                return true

        return false
    }

    @EventHandler
    fun onPlayerMove(e: PlayerEnterChunkEvent) {
        val pdc = e.player.persistentDataContainer
        if(pdc.get(displayKey, PersistentDataType.BOOLEAN) != true)
            return

        val world = e.player.world
        val chunk = e.player.chunk
        val x = chunk.x
        val z = chunk.z

        val chunks = displayEntities.getOrPut(e.player.uniqueId) { mutableListOf() }

        val MAX_RADIUS = 3
        for(dx in -MAX_RADIUS..MAX_RADIUS) {
            for(dz in -MAX_RADIUS..MAX_RADIUS) {
                val currentChunk = world.getChunkAt(x + dx, z + dz)
                if(hasEntityForChunk(chunks, currentChunk.x, currentChunk.z))
                    continue

                val town = PlotGuard.getChunkOwner(currentChunk)
                    ?: continue

                val material = getColoredBlock(town)
                chunks.add(ChunkDisplayData(
                    chunk.x,
                    chunk.z,
                    render(e.player, chunk, material)
                ))
            }
        }

        for(currentChunk in chunks) {
            if(
                abs(currentChunk.x - x) <= MAX_RADIUS &&
                abs(currentChunk.z - z) <= MAX_RADIUS
            ) continue

            for(entity in chunk.entities)
                entity.remove()

            chunks.remove(currentChunk)
        }
    }

    companion object {
        private fun getColoredBlock(town: Town): Material {
            val color = NamedTextColor
                .nearestTo(town.color)
                .toString()
                .uppercase()

            val blockColor = when(color) {
                "AQUA" -> "CYAN"
                "DARK_AQUA" -> "CYAN_GLAZED"
                "GRAY" -> "LIGHT_GRAY"
                "DARK_GRAY" -> "GRAY"
                "GREEN" -> "LIME"
                "DARK_GREEN" -> "GREEN"
                "RED" -> "PINK"
                "DARK_RED" -> "RED"
                "BLUE" -> "LIGHT_BLUE"
                "DARK_BLUE" -> "BLUE"
                "LIGHT_PURPLE" -> "MAGENTA"
                "DARK_PURPLE" -> "PURPLE"
                "GOLD" -> "YELLOW_GLAZED"
                else -> color
            }

            val material = Material.getMaterial("${blockColor}_TERRACOTTA")
            if(material == null) {
                Main.instance?.componentLogger?.warn("Invalid material $material (town color: $color, item: '${blockColor}_TERRACOTA')")
                return Material.TERRACOTTA
            }

            return material
        }

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