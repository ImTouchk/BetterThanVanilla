package dev.mugur.btv.utils

import dev.mugur.btv.Main
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

class Misc {
    companion object {
        fun getChunksInArea(chunk: Chunk, area: Int = 3): List<Chunk> {
            val x = chunk.x
            val z = chunk.z
            val world = chunk.world
            val list = mutableListOf<Chunk>()
            for(dx in -area..area)
                for(dz in -area..area)
                    list.add(world.getChunkAt(x + dx, z + dz))

            return list
        }

        fun displayChunkBorder(
            to: Player,
            chunk: Chunk,
            material: Material,
            height: Double,
            gap: Int
        ): List<BlockDisplay> {
            val location = Location(
                chunk.world,
                (chunk.x * 16).toDouble(), height, (chunk.z * 16).toDouble()
            )

            val list = mutableListOf<BlockDisplay>()
            for(x in 0 until 16 step gap) {
                val side1 = location.clone().add(x.toDouble(), 0.0, 0.0)
                val side2 = location.clone().add(x.toDouble(), 0.0, 16.0)
                list.add(displayEntity(to, side1, material))
                list.add(displayEntity(to, side2, material))
            }

            for(z in 0 until 16 step gap) {
                val side1 = location.clone().add(0.0, 0.0, z.toDouble())
                val side2 = location.clone().add(16.0, 0.0, z.toDouble())
                list.add(displayEntity(to, side1, material))
                list.add(displayEntity(to, side2, material))
            }

            return list
        }

        fun displayEntity(
            to: Player,
            location: Location,
            material: Material,
            additional: ((BlockDisplay) -> Unit)? = null
        ): BlockDisplay {
            val world = location.world!!
            return world.spawn(location, BlockDisplay::class.java) { entity ->
                entity.block = material.createBlockData()
                entity.isPersistent = false
                entity.isVisibleByDefault = false
                entity.transformation = Transformation(
                    Vector3f(),
                    AxisAngle4f(),
                    Vector3f(0.25f, 0.25f, 0.25f),
                    AxisAngle4f()
                )

                if(additional != null)
                    additional(entity)

                to.showEntity(Main.instance!!, entity)
            }
        }

        fun getColoredBlock(from: TextColor): Material {
            val color = NamedTextColor
                .nearestTo(from)
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
                Main.instance?.componentLogger?.warn("Tried to get invalid material (town color: $color, item: '${blockColor}_TERRACOTTA')")
                return Material.TERRACOTTA
            }

            return material
        }

        fun <P, C> setPlayerPref(
            player: Player,
            name: String,
            type: PersistentDataType<P, C>,
            value: C & Any
        ) {
            val pdc = player.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, name)
            pdc.set(key, type, value)
        }

        fun getPlayerPref(player: Player, name: String): Boolean {
            return getPlayerPref(player, name, PersistentDataType.BOOLEAN)
                ?: false
        }

        fun <P, C> getPlayerPref(
            player: Player,
            name: String,
            type: PersistentDataType<P, C>
        ): C? {
            val pdc = player.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, name)
            return pdc.get(key, type)
        }
    }
}