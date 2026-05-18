package system

import main.ConsoleApplication
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import service.ApplicationFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

class DatabaseConsoleSystemTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `console application saves players and rating after restart`() {
        val database = tempDir.resolve("splendor.db")
        val firstStart = ApplicationFactory.createWithDatabase(database)
        val output = ByteArrayOutputStream()
        val app = ConsoleApplication(firstStart, ByteArrayInputStream(ByteArray(0)), PrintStream(output))

        app.handle("add-player Alice")
        app.handle("add-player Bob")
        val players = firstStart.playerService.listPlayers()
        app.handle("new-game ${players[0].id} ${players[1].id}")
        val gameId = firstStart.gameService.listSessions().single().id
        app.handle("finish $gameId")

        val secondStart = ApplicationFactory.createWithDatabase(database)
        val secondOutput = ByteArrayOutputStream()
        val secondApp = ConsoleApplication(secondStart, ByteArrayInputStream(ByteArray(0)), PrintStream(secondOutput))
        secondApp.handle("players")
        secondApp.handle("rating")

        val text = secondOutput.toString()
        assertTrue(text.contains("Alice"))
        assertTrue(text.contains("Bob"))
        assertTrue(text.contains("#1"))
    }
}
