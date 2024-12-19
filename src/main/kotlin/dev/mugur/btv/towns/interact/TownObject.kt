package dev.mugur.btv.towns.interact

import dev.mugur.btv.Main
import dev.mugur.btv.towns.Town
import dev.mugur.btv.towns.TownManager
import dev.mugur.btv.utils.Database
import org.bukkit.Bukkit
import org.bukkit.Location
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

class TownObject(
    id: Int,
    town: Town,
    type: TownObjectType,
    pos: Location
) {
    var id = id
        private set

    var town = town
        private set

    var type = type
        set(value) {
            field = value
            Database.run("UPDATE town_object SET type = '${type.id}' WHERE id = '$id'")
        }

    var pos = pos
        set(value) {
            field = value
            Database.run(
                "UPDATE town_object SET " +
                "world = '${pos.world.uid}', x = '${pos.blockX}, y = '${pos.blockY}', z = ${pos.blockZ}'" +
                "WHERE id = '${id}'"
            )
        }

    constructor(query: ResultSet) : this(
        id = query.getInt("id"),
        town = TownManager.getTownById(query.getString("town_id"))!!,
        type = TownObjectType.fromId(query.getInt("type")),
        pos =  Location(
            Bukkit.getWorld(UUID.fromString(query.getString("world"))),
            query.getInt("x").toDouble(),
            query.getInt("y").toDouble(),
            query.getInt("z").toDouble()
        )
    )

    constructor(town: Town, type: TownObjectType, pos: Location) : this(
        id = 0,
        town = town,
        type = type,
        pos = pos
    ) {
        val stmt = Database.prepare(
            "INSERT INTO town_object (town_id, type, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?);",
        )

        stmt.setString(1, town.id.toString())
        stmt.setInt(2, type.id)
        stmt.setString(3, pos.world.uid.toString())
        stmt.setInt(4, pos.blockX)
        stmt.setInt(5, pos.blockY)
        stmt.setInt(6, pos.blockZ)
        stmt.executeUpdate()

        val res = stmt.generatedKeys
        if(!res.next()) {
            Main.instance!!
                .componentLogger
                .error("No insert row")
        }

        this.id = res.getInt(1)

        town.objects.add(this)
    }
}