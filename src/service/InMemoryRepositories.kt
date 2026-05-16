package service

import domain.GameResult
import domain.GameSession
import domain.Player
import domain.PlayerStatistics


class InMemoryGameRepository : GameRepository {
    private val sessions = linkedMapOf<String, GameSession>()

    override fun save(session: GameSession): GameSession {
        sessions[session.id] = session
        return session
    }

    override fun findById(id: String): GameSession? = sessions[id]

    override fun findAll(): List<GameSession> = sessions.values.toList()
}

class InMemoryPlayerRepository : PlayerRepository {
    private val players = linkedMapOf<String, Player>()

    override fun save(player: Player): Player {
        players[player.id] = player
        return player
    }

    override fun findById(id: String): Player? = players[id]

    override fun findAll(): List<Player> = players.values.toList()
}

class InMemoryGameResultRepository : GameResultRepository {
    private val results = linkedMapOf<String, GameResult>()

    override fun save(result: GameResult): GameResult {
        results[result.gameId] = result
        return result
    }

    override fun findByPlayer(id: String): List<GameResult> =
        results.values.filter { result -> result.results.any { it.player.id == id } }

    override fun findByGameId(id: String): GameResult? = results[id]
}

class InMemoryPlayerStatisticsRepository : PlayerStatisticsRepository {
    private val statistics = linkedMapOf<String, PlayerStatistics>()

    override fun save(stats: PlayerStatistics): PlayerStatistics {
        statistics[stats.player.id] = stats
        return stats
    }

    override fun findByPlayerId(id: String): PlayerStatistics? = statistics[id]

    override fun findAll(): List<PlayerStatistics> = statistics.values.toList()
}
