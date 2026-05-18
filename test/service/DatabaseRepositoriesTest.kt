package service

import domain.CardSource
import domain.GameResult
import domain.GameRules
import domain.GameSession
import domain.GameStatus
import domain.Player
import domain.PlayerFinalResult
import domain.PlayerStatistics
import domain.TakeTokensMove
import domain.TokenColor
import domain.TokenSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DatabaseRepositoriesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `player and statistics repositories save data to database`() {
        val database = tempDir.resolve("splendor.db")
        val firstConnectionFactory = DatabaseConnectionFactory(database)
        val playerRepository = DatabasePlayerRepository(firstConnectionFactory)
        val statisticsRepository = DatabasePlayerStatisticsRepository(firstConnectionFactory)

        playerRepository.save(Player("p1", "Alice"))
        statisticsRepository.save(PlayerStatistics(Player("p1", "Alice"), gamesPlayed = 2, wins = 1))

        val secondConnectionFactory = DatabaseConnectionFactory(database)
        val reloadedPlayerRepository = DatabasePlayerRepository(secondConnectionFactory)
        val reloadedStatisticsRepository = DatabasePlayerStatisticsRepository(secondConnectionFactory)

        assertEquals("Alice", reloadedPlayerRepository.findById("p1")?.name)
        assertEquals(2, reloadedStatisticsRepository.findByPlayerId("p1")?.gamesPlayed)
        assertEquals(1, reloadedStatisticsRepository.findByPlayerId("p1")?.wins)
    }

    @Test
    fun `game repository saves session and move history`() {
        val connectionFactory = DatabaseConnectionFactory(tempDir.resolve("splendor.db"))
        val repository = DatabaseGameRepository(connectionFactory)
        val players = listOf(Player("p1", "Alice"), Player("p2", "Bob"))
        val state = GameInitializer().createInitialState(players, GameRules())
        val move = TakeTokensMove(
            id = "m1",
            playerId = "p1",
            tokens = TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1, TokenColor.GREEN to 1),
        )
        val session = GameSession("g1", players, state, GameStatus.IN_PROGRESS, moves = listOf(move))

        repository.save(session)
        val reloaded = repository.findById("g1")

        assertNotNull(reloaded)
        assertEquals(GameStatus.IN_PROGRESS, reloaded?.status)
        assertEquals(listOf("m1"), reloaded?.moves?.map { it.id })
        assertEquals(1, repository.countMoves("g1"))
    }

    @Test
    fun `game result repository saves finished game table`() {
        val connectionFactory = DatabaseConnectionFactory(tempDir.resolve("splendor.db"))
        val repository = DatabaseGameResultRepository(connectionFactory)
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")
        val result = GameResult(
            gameId = "g1",
            winner = alice,
            finishedAt = 1000L,
            results = listOf(
                PlayerFinalResult(alice, score = 15, noblesCount = 1, place = 1),
                PlayerFinalResult(bob, score = 10, noblesCount = 0, place = 2),
            ),
        )

        repository.save(result)

        assertEquals("Alice", repository.findByGameId("g1")?.winner?.name)
        assertEquals(listOf(1, 2), repository.findByPlayer("p1").single().results.map { it.place })
    }
}
