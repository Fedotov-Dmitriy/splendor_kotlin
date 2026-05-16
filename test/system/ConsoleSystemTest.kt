package system

import main.ConsoleApplication
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import service.ApplicationFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsoleSystemTest {
    @Test
    fun `player can create game make moves and finish from console`() {
        val services = ApplicationFactory.create()
        val alice = services.playerService.addPlayer("Alice")
        val bob = services.playerService.addPlayer("Bob")
        val output = ByteArrayOutputStream()
        val app = ConsoleApplication(services, ByteArrayInputStream(ByteArray(0)), PrintStream(output))

        app.handle("new-game ${alice.id} ${bob.id}")
        val gameId = services.gameService.listSessions().single().id
        app.handle("take $gameId white=1 blue=1 green=1")
        app.handle("take $gameId white=1 blue=1 green=1")
        app.handle("finish $gameId")
        app.handle("rating")

        val text = output.toString()
        assertTrue(text.contains("Game created"))
        assertTrue(text.contains("Move accepted"))
        assertTrue(text.contains("Game finished"))
        assertTrue(text.contains("#1"))
    }
}

