package domain

data class GameRules(
    val maxTokensPerPlayer: Int = 10,
    val pointsToEndGame: Int = 15,
    val maxReservedCards: Int = 3,
    val openCardsPerRow: Int = 4,
) {
    fun isGameOver(state: GameState): Boolean = state.players.any { it.score() >= pointsToEndGame }
}

data class ValidationResult(
    val valid: Boolean,
    val reason: String = "",
) {
    companion object {
        val ok = ValidationResult(true)
        fun invalid(reason: String) = ValidationResult(false, reason)
    }
}

data class MoveResult(
    val success: Boolean,
    val message: String,
    val newState: GameState,
)

data class PlayerFinalResult(
    val player: Player,
    val score: Int,
    val noblesCount: Int,
    val place: Int,
)

data class GameResult(
    val gameId: String,
    val winner: Player,
    val finishedAt: Long,
    val results: List<PlayerFinalResult>,
)

data class PlayerStatistics(
    val player: Player,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
) {
    val winRate: Double = if (gamesPlayed == 0) 0.0 else wins.toDouble() / gamesPlayed
}

data class Rating(
    val player: Player,
    val value: Double,
    val position: Int,
)

