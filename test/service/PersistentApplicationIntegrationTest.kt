package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PersistentApplicationIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `application saves players and statistics between starts`() {
        val firstStart = ApplicationFactory.createWithFiles(tempDir)
        val alice = firstStart.playerService.addPlayer("Alice")
        val bob = firstStart.playerService.addPlayer("Bob")
        val game = firstStart.gameService.createSession(listOf(alice.id, bob.id))

        firstStart.gameService.finishGame(game.id)

        val secondStart = ApplicationFactory.createWithFiles(tempDir)
        val players = secondStart.playerService.listPlayers()
        val statistics = secondStart.statisticsService.listStatistics()

        assertEquals(listOf("Alice", "Bob"), players.map { it.name })
        assertEquals(2, statistics.size)
        assertTrue(statistics.all { it.gamesPlayed == 1 })
        assertEquals(1, statistics.count { it.wins == 1 })
        assertEquals(1, statistics.count { it.wins == 0 })
    }
}
