package service

import domain.Player
import domain.PlayerStatistics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FileRepositoriesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `player repository saves players to file`() {
        val file = tempDir.resolve("players.tsv")
        val repository = FilePlayerRepository(file)

        repository.save(Player("p1", "Alice"))
        repository.save(Player("p2", "Bob"))

        val reloadedRepository = FilePlayerRepository(file)
        val players = reloadedRepository.findAll()

        assertEquals(listOf("Alice", "Bob"), players.map { it.name })
    }

    @Test
    fun `statistics repository saves local statistics to file`() {
        val file = tempDir.resolve("stats.tsv")
        val repository = FilePlayerStatisticsRepository(file)

        repository.save(PlayerStatistics(Player("p1", "Alice"), gamesPlayed = 3, wins = 2))

        val reloadedRepository = FilePlayerStatisticsRepository(file)
        val statistics = reloadedRepository.findByPlayerId("p1")

        assertEquals(3, statistics?.gamesPlayed)
        assertEquals(2, statistics?.wins)
    }
}

