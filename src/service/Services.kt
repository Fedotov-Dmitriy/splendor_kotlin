package service

import domain.*
import java.time.Clock
import java.util.UUID

class PlayerService(
    private val playerRepository: PlayerRepository,
    private val statisticsRepository: PlayerStatisticsRepository? = null,
) {
    fun addPlayer(name: String): Player {
        require(name.isNotBlank()) { "Player name cannot be blank" }
        val player = Player(UUID.randomUUID().toString(), name.trim())
        val savedPlayer = playerRepository.save(player)
        statisticsRepository?.save(PlayerStatistics(savedPlayer))
        return savedPlayer
    }

    fun listPlayers(): List<Player> = playerRepository.findAll()
}

class GameService(
    private val gameRepository: GameRepository,
    private val gameResultRepository: GameResultRepository,
    private val playerRepository: PlayerRepository,
    private val moveProcessor: MoveProcessor,
    private val gameInitializer: GameInitializer,
    private val scoreCalculator: ScoreCalculator,
    private val statisticsService: StatisticsService,
    private val rules: GameRules,
) {
    fun createSession(playerIds: List<String>): GameSession {
        require(playerIds.size in 2..4) { "A game needs 2 to 4 players" }
        require(playerIds.distinct().size == playerIds.size) { "Players in a session must be unique" }
        val players = playerIds.map { id ->
            playerRepository.findById(id) ?: throw IllegalArgumentException("Unknown player: $id")
        }
        val initialState = gameInitializer.createInitialState(players, rules)
        val session = GameSession(
            id = UUID.randomUUID().toString(),
            players = players,
            state = initialState,
            status = GameStatus.IN_PROGRESS,
        )
        return gameRepository.save(session)
    }

    fun makeMove(sessionId: String, move: Move): MoveResult {
        val session = gameRepository.findById(sessionId) ?: throw IllegalArgumentException("Unknown session: $sessionId")
        if (session.status != GameStatus.IN_PROGRESS) {
            return MoveResult(false, "Game is not in progress", session.state)
        }

        val result = moveProcessor.process(move, session.state)
        if (!result.success) return result

        val finalRoundStarted = session.finalRoundStarted || result.newState.isFinished(rules)
        val roundIsFinished = finalRoundStarted && result.newState.currentPlayerIndex == 0
        val newStatus = if (roundIsFinished) GameStatus.FINISHED else GameStatus.IN_PROGRESS
        val updated = session.copy(
            state = result.newState,
            status = newStatus,
            moves = session.moves + move,
            finalRoundStarted = finalRoundStarted,
        )
        gameRepository.save(updated)
        if (newStatus == GameStatus.FINISHED && gameResultRepository.findByGameId(session.id) == null) {
            val gameResult = scoreCalculator.calculateGameResult(updated)
            gameResultRepository.save(gameResult)
            statisticsService.updateStatistics(gameResult)
        }

        return result
    }

    fun finishGame(sessionId: String): GameResult {
        val session = gameRepository.findById(sessionId) ?: throw IllegalArgumentException("Unknown session: $sessionId")
        val result = scoreCalculator.calculateGameResult(session.copy(status = GameStatus.FINISHED))
        gameRepository.save(session.copy(status = GameStatus.FINISHED))
        gameResultRepository.save(result)
        statisticsService.updateStatistics(result)
        return result
    }

    fun getSession(sessionId: String): GameSession? = gameRepository.findById(sessionId)

    fun listSessions(): List<GameSession> = gameRepository.findAll()
}

class ScoreCalculator(private val clock: Clock = Clock.systemUTC()) {
    fun calculatePlayerScore(playerState: PlayerState): Int = playerState.score()

    fun calculateGameResult(session: GameSession): GameResult {
        val sorted = session.state.players
            .sortedWith(
                compareByDescending<PlayerState> { it.score() }
                    .thenBy { it.purchasedCards.size },
            )

        val finals = sorted.mapIndexed { index, playerState ->
            PlayerFinalResult(
                player = playerState.player,
                score = playerState.score(),
                noblesCount = playerState.nobles.size,
                place = index + 1,
            )
        }
        return GameResult(
            gameId = session.id,
            winner = finals.first().player,
            finishedAt = clock.millis(),
            results = finals,
        )
    }
}

class StatisticsService(
    private val statisticsRepository: PlayerStatisticsRepository,
) {
    fun updateStatistics(result: GameResult) {
        result.results.forEach { final ->
            val current = statisticsRepository.findByPlayerId(final.player.id) ?: PlayerStatistics(final.player)
            statisticsRepository.save(
                current.copy(
                    gamesPlayed = current.gamesPlayed + 1,
                    wins = current.wins + if (final.place == 1) 1 else 0,
                ),
            )
        }
    }

    fun getStatistics(playerId: String): PlayerStatistics? = statisticsRepository.findByPlayerId(playerId)

    fun listStatistics(): List<PlayerStatistics> = statisticsRepository.findAll()
}

class RatingService(private val statisticsRepository: PlayerStatisticsRepository) {
    fun recalculate(): List<Rating> = getRating()

    fun getRating(): List<Rating> =
        statisticsRepository.findAll()
            .sortedWith(compareByDescending<PlayerStatistics> { it.winRate }.thenByDescending { it.gamesPlayed })
            .mapIndexed { index, stats ->
                Rating(stats.player, value = stats.winRate * 100.0 + stats.gamesPlayed, position = index + 1)
            }
}
