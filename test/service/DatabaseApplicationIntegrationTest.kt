package service

import domain.GameStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DatabaseApplicationIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `application saves finished game history and statistics in database`() {
        val database = tempDir.resolve("splendor.db")
        val firstStart = ApplicationFactory.createWithDatabase(database)
        val alice = firstStart.playerService.addPlayer("Alice")
        val bob = firstStart.playerService.addPlayer("Bob")
        val game = firstStart.gameService.createSession(listOf(alice.id, bob.id))

        firstStart.gameService.finishGame(game.id)

        val secondStart = ApplicationFactory.createWithDatabase(database)
        val players = secondStart.playerService.listPlayers()
        val sessions = secondStart.gameService.listSessions()
        val statistics = secondStart.statisticsService.listStatistics()

        assertEquals(listOf("Alice", "Bob"), players.map { it.name })
        assertEquals(1, sessions.size)
        assertEquals(GameStatus.FINISHED, sessions.single().status)
        assertEquals(2, statistics.size)
        assertTrue(statistics.all { it.gamesPlayed == 1 })
        assertEquals(1, statistics.count { it.wins == 1 })
        assertEquals(1, statistics.count { it.wins == 0 })
    }
}
