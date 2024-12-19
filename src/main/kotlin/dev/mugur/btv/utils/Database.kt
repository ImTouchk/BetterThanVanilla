package dev.mugur.btv.utils

import dev.mugur.btv.Main
import java.sql.*

class Database {
    companion object {
        private lateinit var connection: Connection

        fun init() {
            Class.forName("org.sqlite.JDBC")
            this.connection = DriverManager.getConnection("jdbc:sqlite:plugins/BetterThanVanilla/database.db")
        }

        fun sync() {
            runFile("db/towns/sync.sql")
        }

        fun run(sql: String): Boolean {
            try {
                return connection
                    .prepareStatement(sql)
                    .execute()
            }
            catch (e: SQLException) {
                Main
                    .instance
                    ?.componentLogger
                    ?.error(e.message!!)
                return false
            }
        }

        fun query(sql: String): ResultSet? {
            try {
                return connection
                    .prepareStatement(sql)
                    .executeQuery()
            } catch (e: SQLException) {
                Main
                    .instance
                    ?.componentLogger
                    ?.error(e.message!!)
                return null
            }
        }

        fun prepare(sql: String): PreparedStatement {
            return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        }

        fun runFile(res: String) {
            val file = Main
                .instance
                ?.getResource(res)
                ?.readAllBytes()

            if(file == null) {
                Main
                    .instance
                    ?.componentLogger
                    ?.error("SQL resource \"$res\" not found.")
                return
            }

            val content = String(file)
            val separated = content.split(';')
            val stmt = connection
                .createStatement()

            for(str in separated) {
                try {
                    val final = str.trim()
                    if(final.isEmpty())
                        continue

                    stmt.execute(final)
                } catch (e: SQLException) {
                    Main
                        .instance
                        ?.componentLogger
                        ?.error(e.message!!)
                }
            }
            stmt.close()

            Main
                .instance
                ?.componentLogger
                ?.debug("SQL resource \"$res\" executed.")
        }
    }
}