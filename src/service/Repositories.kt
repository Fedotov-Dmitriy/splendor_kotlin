package service

import domain.GameResult
import domain.GameSession
import domain.Player
import domain.PlayerStatistics


interface GameRepository {
    fun save(session: GameSession): GameSession
    fun findById(id: String): GameSession?
    fun findAll(): List<GameSession>
}

interface PlayerRepository {
    fun save(player: Player): Player
    fun findById(id: String): Player?
    fun findAll(): List<Player>
}

interface GameResultRepository {
    fun save(result: GameResult): GameResult
    fun findByPlayer(id: String): List<GameResult>
    fun findByGameId(id: String): GameResult?
}

interface PlayerStatisticsRepository {
    fun save(stats: PlayerStatistics): PlayerStatistics
    fun findByPlayerId(id: String): PlayerStatistics?
    fun findAll(): List<PlayerStatistics>
}
