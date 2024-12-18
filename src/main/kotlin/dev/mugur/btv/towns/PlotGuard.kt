package dev.mugur.btv.towns

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import dev.mugur.btv.Main
import dev.mugur.btv.utils.Database
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

class PlotGuard {
    companion object {
        fun getChunkOwner(chunk: Chunk): Town? {
            val pdc = chunk.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, "chunk-owner")
            val ownerId = pdc.get(key, PersistentDataType.STRING)
                ?: return null

            return TownManager.getTownById(ownerId)
        }

        fun setChunkOwner(chunk: Chunk, town: Town?) {
            val pdc = chunk.persistentDataContainer
            val key = NamespacedKey(Main.instance!!, "chunk-owner")

            val previous = getChunkOwner(chunk)
            if(previous != null) {
                previous.plots--
            }

            val stmt = Database.prepare("REPLACE INTO town_plot (world_id, chunk_x, chunk_z, town_id) VALUES (?, ?, ?, ?);")
            stmt.setString(1, chunk.world.uid.toString())
            stmt.setInt(2, chunk.x)
            stmt.setInt(3, chunk.z)
            if(town == null) {
                pdc.remove(key)
                stmt.setNull(4, java.sql.Types.VARCHAR)
            } else {
                town.plots++
                pdc.set(key, PersistentDataType.STRING, town.id.toString())
                stmt.setString(4, town.id.toString())
            }

            stmt.execute()
            updateWorldGuardRegion(chunk)
        }

        private fun updateWorldGuardRegion(chunk: Chunk) {
            val owner = getChunkOwner(chunk)

            if(owner == null) {
                val container = WorldGuard
                    .getInstance()
                    .platform
                    .regionContainer

                val regions = container.get(BukkitAdapter.adapt(chunk.world))
                val regionId = "plot_${chunk.x}_${chunk.z}_${chunk.world.name}"
                regions?.removeRegion(regionId)
                return
            }

            val region = getWorldGuardRegion(chunk)
            region.members.clear()

            owner.players.forEach { member ->
                region.members.addPlayer(member)
            }
        }

        private fun getWorldGuardRegion(chunk: Chunk): ProtectedRegion {
            val regionId = "plot-${chunk.x}-${chunk.z}-${chunk.world.name}"
            val container = WorldGuard
                .getInstance()
                .platform
                .regionContainer

            val regions = container.get(BukkitAdapter.adapt(chunk.world))
            var region = regions?.getRegion(regionId)
            if(region == null) {
                val min = BlockVector3.at(chunk.x * 16, -100, chunk.z * 16)
                val max = BlockVector3.at(chunk.x * 16 + 16, 500, chunk.z * 16 + 16)
                region = ProtectedCuboidRegion(regionId, min, max)
                region.setFlag(Flags.PVP, StateFlag.State.DENY)
                region.setFlag(Flags.USE, StateFlag.State.DENY)
                region.setFlag(Flags.TNT, StateFlag.State.ALLOW)
                regions?.addRegion(region)
            }
            return region
        }
    }
}