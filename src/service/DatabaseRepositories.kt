package service

import domain.GameResult
import domain.GameSession
import domain.GameStatus
import domain.MoveType
import domain.Player
import domain.PlayerFinalResult
import domain.PlayerStatistics
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DatabaseConnectionFactory(private val databaseFile: Path) {
    init {
        databaseFile.parent?.let { Files.createDirectories(it) }
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("PRAGMA foreign_keys = ON")
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS players (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS player_statistics (
                        player_id TEXT PRIMARY KEY,
                        player_name TEXT NOT NULL,
                        games_played INTEGER NOT NULL,
                        wins INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS game_sessions (
                        id TEXT PRIMARY KEY,
                        status TEXT NOT NULL,
                        session_blob BLOB NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS game_moves (
                        game_id TEXT NOT NULL,
                        move_order INTEGER NOT NULL,
                        move_id TEXT NOT NULL,
                        player_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        PRIMARY KEY (game_id, move_order)
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS game_results (
                        game_id TEXT PRIMARY KEY,
                        winner_id TEXT NOT NULL,
                        winner_name TEXT NOT NULL,
                        finished_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS game_result_players (
                        game_id TEXT NOT NULL,
                        player_id TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        nobles_count INTEGER NOT NULL,
                        place INTEGER NOT NULL,
                        PRIMARY KEY (game_id, player_id)
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    fun connection(): Connection =
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.toAbsolutePath()}")
}

class DatabasePlayerRepository(private val connectionFactory: DatabaseConnectionFactory) : PlayerRepository {
    override fun save(player: Player): Player {
        connectionFactory.connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO players(id, name)
                VALUES (?, ?)
                ON CONFLICT(id) DO UPDATE SET name = excluded.name
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, player.id)
                statement.setString(2, player.name)
                statement.executeUpdate()
            }
        }
        return player
    }

    override fun findById(id: String): Player? =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement("SELECT id, name FROM players WHERE id = ?").use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toPlayer() else null
                }
            }
        }

    override fun findAll(): List<Player> =
        connectionFactory.connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id, name FROM players ORDER BY rowid").use { result ->
                    buildList {
                        while (result.next()) add(result.toPlayer())
                    }
                }
            }
        }
}

class DatabasePlayerStatisticsRepository(
    private val connectionFactory: DatabaseConnectionFactory,
) : PlayerStatisticsRepository {
    override fun save(stats: PlayerStatistics): PlayerStatistics {
        connectionFactory.connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO player_statistics(player_id, player_name, games_played, wins)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_id) DO UPDATE SET
                    player_name = excluded.player_name,
                    games_played = excluded.games_played,
                    wins = excluded.wins
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, stats.player.id)
                statement.setString(2, stats.player.name)
                statement.setInt(3, stats.gamesPlayed)
                statement.setInt(4, stats.wins)
                statement.executeUpdate()
            }
        }
        return stats
    }

    override fun findByPlayerId(id: String): PlayerStatistics? =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement(
                "SELECT player_id, player_name, games_played, wins FROM player_statistics WHERE player_id = ?",
            ).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toStatistics() else null
                }
            }
        }

    override fun findAll(): List<PlayerStatistics> =
        connectionFactory.connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT player_id, player_name, games_played, wins FROM player_statistics ORDER BY rowid",
                ).use { result ->
                    buildList {
                        while (result.next()) add(result.toStatistics())
                    }
                }
            }
        }
}

class DatabaseGameRepository(private val connectionFactory: DatabaseConnectionFactory) : GameRepository {
    override fun save(session: GameSession): GameSession {
        connectionFactory.connection().use { connection ->
            connection.inTransaction {
                connection.prepareStatement(
                    """
                    INSERT INTO game_sessions(id, status, session_blob, updated_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        status = excluded.status,
                        session_blob = excluded.session_blob,
                        updated_at = excluded.updated_at
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, session.id)
                    statement.setString(2, session.status.name)
                    statement.setBytes(3, serialize(session))
                    statement.setLong(4, System.currentTimeMillis())
                    statement.executeUpdate()
                }

                connection.prepareStatement("DELETE FROM game_moves WHERE game_id = ?").use { statement ->
                    statement.setString(1, session.id)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    """
                    INSERT INTO game_moves(game_id, move_order, move_id, player_id, type, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).use { statement ->
                    session.moves.forEachIndexed { index, move ->
                        statement.setString(1, session.id)
                        statement.setInt(2, index)
                        statement.setString(3, move.id)
                        statement.setString(4, move.playerId)
                        statement.setString(5, move.type.name)
                        statement.setLong(6, move.createdAt)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
        return session
    }

    override fun findById(id: String): GameSession? =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement("SELECT session_blob FROM game_sessions WHERE id = ?").use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) deserialize(result.getBytes("session_blob")) else null
                }
            }
        }

    override fun findAll(): List<GameSession> =
        connectionFactory.connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT session_blob FROM game_sessions ORDER BY rowid").use { result ->
                    buildList {
                        while (result.next()) add(deserialize(result.getBytes("session_blob")))
                    }
                }
            }
        }

    fun countMoves(gameId: String): Int =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM game_moves WHERE game_id = ?").use { statement ->
                statement.setString(1, gameId)
                statement.executeQuery().use { result ->
                    if (result.next()) result.getInt(1) else 0
                }
            }
        }

    fun countFinishedGames(): Int =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM game_sessions WHERE status = ?").use { statement ->
                statement.setString(1, GameStatus.FINISHED.name)
                statement.executeQuery().use { result ->
                    if (result.next()) result.getInt(1) else 0
                }
            }
        }
}

class DatabaseGameResultRepository(private val connectionFactory: DatabaseConnectionFactory) : GameResultRepository {
    override fun save(result: GameResult): GameResult {
        connectionFactory.connection().use { connection ->
            connection.inTransaction {
                connection.prepareStatement(
                    """
                    INSERT INTO game_results(game_id, winner_id, winner_name, finished_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(game_id) DO UPDATE SET
                        winner_id = excluded.winner_id,
                        winner_name = excluded.winner_name,
                        finished_at = excluded.finished_at
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, result.gameId)
                    statement.setString(2, result.winner.id)
                    statement.setString(3, result.winner.name)
                    statement.setLong(4, result.finishedAt)
                    statement.executeUpdate()
                }

                connection.prepareStatement("DELETE FROM game_result_players WHERE game_id = ?").use { statement ->
                    statement.setString(1, result.gameId)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    """
                    INSERT INTO game_result_players(game_id, player_id, player_name, score, nobles_count, place)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).use { statement ->
                    result.results.forEach { playerResult ->
                        statement.setString(1, result.gameId)
                        statement.setString(2, playerResult.player.id)
                        statement.setString(3, playerResult.player.name)
                        statement.setInt(4, playerResult.score)
                        statement.setInt(5, playerResult.noblesCount)
                        statement.setInt(6, playerResult.place)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
        return result
    }

    override fun findByPlayer(id: String): List<GameResult> =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement("SELECT game_id FROM game_result_players WHERE player_id = ? ORDER BY game_id")
                .use { statement ->
                    statement.setString(1, id)
                    statement.executeQuery().use { result ->
                        buildList {
                            while (result.next()) {
                                findByGameId(result.getString("game_id"))?.let { add(it) }
                            }
                        }
                    }
                }
        }

    override fun findByGameId(id: String): GameResult? =
        connectionFactory.connection().use { connection ->
            connection.prepareStatement(
                "SELECT winner_id, winner_name, finished_at FROM game_results WHERE game_id = ?",
            ).use { resultStatement ->
                resultStatement.setString(1, id)
                resultStatement.executeQuery().use { result ->
                    if (!result.next()) return@use null
                    val winner = Player(result.getString("winner_id"), result.getString("winner_name"))
                    val finishedAt = result.getLong("finished_at")
                    val players = loadPlayerResults(connection, id)
                    GameResult(id, winner, finishedAt, players)
                }
            }
        }

    private fun loadPlayerResults(connection: Connection, gameId: String): List<PlayerFinalResult> =
        connection.prepareStatement(
            """
            SELECT player_id, player_name, score, nobles_count, place
            FROM game_result_players
            WHERE game_id = ?
            ORDER BY place
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, gameId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            PlayerFinalResult(
                                player = Player(result.getString("player_id"), result.getString("player_name")),
                                score = result.getInt("score"),
                                noblesCount = result.getInt("nobles_count"),
                                place = result.getInt("place"),
                            ),
                        )
                    }
                }
            }
        }
}

private fun ResultSet.toPlayer(): Player =
    Player(getString("id"), getString("name"))

private fun ResultSet.toStatistics(): PlayerStatistics =
    PlayerStatistics(
        player = Player(getString("player_id"), getString("player_name")),
        gamesPlayed = getInt("games_played"),
        wins = getInt("wins"),
    )

private fun <T> Connection.inTransaction(block: () -> T): T {
    val previousAutoCommit = autoCommit
    autoCommit = false
    try {
        val result = block()
        commit()
        return result
    } catch (ex: Exception) {
        rollback()
        throw ex
    } finally {
        autoCommit = previousAutoCommit
    }
}

private fun serialize(session: GameSession): ByteArray {
    val output = ByteArrayOutputStream()
    ObjectOutputStream(output).use { it.writeObject(session) }
    return output.toByteArray()
}

private fun deserialize(bytes: ByteArray): GameSession =
    ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as GameSession }
