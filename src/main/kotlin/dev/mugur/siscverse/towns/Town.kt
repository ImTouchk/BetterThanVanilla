package dev.mugur.siscverse.towns

import dev.mugur.siscverse.utils.ChatHelper
import dev.mugur.siscverse.utils.Database
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
    val players: MutableList<UUID>
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
            Database.run("UPDATE TOWN SET color = '${value.asHexString()}' WHERE id = '$id'")
        }

    fun getCostOfNewPlot(): Int {
        // logarithmic distribution/price reaches an equilibrium at some point
        // price is inversely proportional to number of town members (more members -> cheaper) to avoid wealth bubbles
        // ln (plotCount^2 / members) * baseCost^2
        // use desmos to visualise graph
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
        Database.run("DELETE FROM \"town_member\" WHERE player_id = '${player.uniqueId}';")
        getScoreboardTeam()
            .removePlayer(player)

        broadcast(ChatHelper.getMessage(reasonMessage, player.name, name), important = true)
    }

    fun addPlayer(player: Player) {
        val stmt = Database.prepare(
            "REPLACE INTO \"town_member\" (player_id, town_id) VALUES (?, ?);"
        )

        stmt.setString(1, player.uniqueId.toString())
        stmt.setString(2, id.toString())
        stmt.execute()

        broadcast(ChatHelper.getMessage("town.player.joined", player.name, name), important = true)

        players.add(player.uniqueId)
        getScoreboardTeam()
            .addEntity(player)

    }

    constructor(query: ResultSet) : this(
        id = UUID.fromString(query.getString("id")),
        name = query.getString("name"),
        founder = UUID.fromString(query.getString("founder"))!!,
        mayor = UUID.fromString(query.getString("mayor")) ?: null,
        money = query.getInt("money"),
        plots = query.getInt("plots"),
        color = TextColor.fromCSSHexString(query.getString("color"))!!,
        players = mutableListOf()
    ) {
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

        TownManager.towns.add(this)
    }

    constructor(name: String, founder: Player) : this(
        id = UUID.randomUUID(),
        name = name,
        founder = founder.uniqueId,
        mayor = founder.uniqueId,
        money = 0,
        plots = 0,
        color = NamedTextColor.AQUA,
        players = mutableListOf()
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