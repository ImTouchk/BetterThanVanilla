package dev.mugur.btv.towns

import dev.mugur.btv.towns.interact.TownObject
import dev.mugur.btv.utils.ChatHelper
import dev.mugur.btv.utils.Database
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.sql.ResultSet
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

class Town(
    val id: UUID,
    val name: String,
    val founder: UUID,
    mayor: UUID?,
    money: Int,
    plots: Int,
    color: TextColor,
    val players: MutableList<UUID>,
    val objects: MutableList<TownObject>,
    active: Boolean
) {
    var mayor: UUID? = mayor
        set(value) {
            field = value
            Database.run("UPDATE town SET mayor = ${mayor ?: "NULL"} WHERE id = '$id'")
        }

    var money: Int = money
        set(value) {
            field = value
            Database.run("UPDATE town SET money = $money WHERE id = '$id'")
        }

    var plots: Int = plots
        set(value) {
            field = value
            Database.run("UPDATE town SET plots = $value WHERE id = '$id'")
        }

    var color: TextColor = color
        set(value) {
            field = value
            Database.run("UPDATE town SET color = '${value.asHexString()}' WHERE id = '$id'")
        }

    var active: Boolean = active
        set(value) {
            field = value
            Database.run("UPDATE town SET active = '${if(value) 1 else 0}' WHERE id = '$id';")
            if(!value)
                ChatHelper.broadcastMessage("town.abandoned", name)
        }

    fun getCostOfNewPlot(): Int {
        // logarithmic distribution/price reaches an equilibrium at some point
        // price is inversely proportional to number of town members (more members -> cheaper) to avoid wealth bubbles
        // ln (plotCount^2 / members) * baseCost^2
        // use desmos to visualise graph
        if(players.size == 0)
            return -1

        val baseCost = 3
        val baseCostSq = baseCost * baseCost
        val function = ln((plots * plots / players.size).toDouble()) * baseCostSq
        return max(0.0, function).roundToInt()
    }

    fun broadcast(message: Component, important: Boolean = false) {
        for(uuid in players) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.sendMessage(message)
        }

        if(important) {
            val stmt = Database.prepare("INSERT INTO town_message (town_id, message) VALUES (?, ?);")
            stmt.setString(1, id.toString())
            stmt.setString(2, ChatHelper.serialize(message))
            stmt.execute()
        }
    }

    fun removePlayer(player: Player, reasonMessage: String) {
        if(mayor == player.uniqueId) {
            mayor = null
        }

        Database.run("DELETE FROM \"town_member\" WHERE player_id = '${player.uniqueId}';")
        getScoreboardTeam()
            .removePlayer(player)

        broadcast(ChatHelper.getMessage(reasonMessage, player.name, name), important = true)

        players.remove(player.uniqueId)

        if(players.size == 0)
            active = false
    }

    fun addPlayer(player: Player) {
        if(players.size == 0) {
            active = true
            mayor = player.uniqueId
        }

        val stmt = Database.prepare(
            "REPLACE INTO \"town_member\" (player_id, town_id) VALUES (?, ?);"
        )

        stmt.setString(1, player.uniqueId.toString())
        stmt.setString(2, id.toString())
        stmt.execute()

        players.add(player.uniqueId)
        broadcast(ChatHelper.getMessage("town.player.joined", player.name, name), important = true)

        getScoreboardTeam()
            .addEntity(player)

    }

    constructor(query: ResultSet) : this(
        id = UUID.fromString(query.getString("id")),
        name = query.getString("name"),
        founder = UUID.fromString(query.getString("founder"))!!,
        mayor =
            if(query.getObject("mayor") != null)
                UUID.fromString(query.getString("mayor"))
            else
                null,
        money = query.getInt("money"),
        plots = query.getInt("plots"),
        color = TextColor.fromCSSHexString(query.getString("color"))!!,
        players = mutableListOf(),
        objects = mutableListOf(),
        active = query.getBoolean("active")
    ) {
        TownManager.towns.add(this)

        loadMembers()
        loadObjects()
    }

    private fun loadObjects() {
        val stmt = Database.prepare("SELECT * FROM town_object WHERE town_id = ?;")
        stmt.setString(1, id.toString())

        val list = stmt.executeQuery()
        while(list.next()) {
            objects.add(TownObject(list))
        }
    }

    private fun loadMembers() {
        val stmt = Database.prepare("SELECT * FROM \"town_member\" WHERE town_id = ?;")
        stmt.setString(1, id.toString())

        val list = stmt.executeQuery()
        while(list.next()) {
            players.add(UUID.fromString(
                list.getString("player_id")
            ))

            getScoreboardTeam()
                .addPlayer(Bukkit.getOfflinePlayer(players[players.size - 1]))
        }
    }

    constructor(name: String, founder: Player) : this(
        id = UUID.randomUUID(),
        name = name,
        founder = founder.uniqueId,
        mayor = founder.uniqueId,
        money = 0,
        plots = 0,
        color = NamedTextColor.AQUA,
        players = mutableListOf(),
        objects = mutableListOf(),
        active = true
    ) {
        val stmt = Database.prepare("INSERT INTO town (id, name, founder, mayor) VALUES (?, ?, ?, ?);")!!
        stmt.setString(1, id.toString())
        stmt.setString(2, name)
        stmt.setString(3, founder.uniqueId.toString())
        stmt.setString(4, founder.uniqueId.toString())
        stmt.executeUpdate()
        addPlayer(founder)

        TownManager.towns.add(this)
    }

    fun getClaimedPlots(): List<Chunk> {
        val stmt = Database.prepare("SELECT * FROM town_plot WHERE town_id = ?;")
        stmt.setString(1, id.toString())

        val list = mutableListOf<Chunk>()
        val res = stmt.executeQuery()
        while(res.next()) {
            val worldId = res.getString("world_id")
            val world = Bukkit.getWorld(worldId)!!
            val x = res.getInt("chunk_x")
            val z = res.getInt("chunk_z")
            list.add(world.getChunkAt(x, z))
        }
        return list
    }

    fun getScoreboardTeam(): Team {
        val scoreboard = Bukkit
            .getScoreboardManager()
            .mainScoreboard

        var team = scoreboard.getTeam(name)
        if(team == null) {
            team = scoreboard.registerNewTeam(name)
            team.prefix(
            Component
                .text(name)
                .color(NamedTextColor.AQUA)
                .append(
                Component
                    .text(" ")
                    .color(NamedTextColor.WHITE)
                )
            )
        }
        return team
    }
}

fun Player.getTown(): Town? {
    return TownManager.getTownOfPlayer(this)
}

fun Player.setTown(town: Town) {
    getTown()?.removePlayer(this, "town.player.left")
    town.addPlayer(this)
}

fun Player.isMayor(): Boolean {
    return getTown()?.mayor == uniqueId
}