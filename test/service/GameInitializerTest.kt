package service

import domain.GameRules
import domain.Player
import domain.TokenColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameInitializerTest {
    @Test
    fun `creates initial state for two players`() {
        val players = listOf(Player("p1", "Alice"), Player("p2", "Bob"))
        val state = GameInitializer().createInitialState(players, GameRules())

        assertEquals(0, state.currentPlayerIndex)
        assertEquals(2, state.players.size)
        assertEquals(4, state.board.bankTokens[TokenColor.WHITE])
        assertEquals(5, state.board.bankTokens[TokenColor.GOLD])
        assertEquals(4, state.board.market.levelOne.openCards.size)
        assertEquals(4, state.board.market.levelTwo.openCards.size)
        assertEquals(4, state.board.market.levelThree.openCards.size)
    }

    @Test
    fun `creates bank with five color tokens for three players`() {
        val players = listOf(Player("p1", "Alice"), Player("p2", "Bob"), Player("p3", "Clara"))

        val state = GameInitializer().createInitialState(players, GameRules())

        assertEquals(5, state.board.bankTokens[TokenColor.WHITE])
        assertEquals(5, state.board.bankTokens[TokenColor.BLUE])
        assertEquals(5, state.board.bankTokens[TokenColor.GREEN])
        assertEquals(5, state.board.bankTokens[TokenColor.RED])
        assertEquals(5, state.board.bankTokens[TokenColor.BLACK])
        assertEquals(5, state.board.bankTokens[TokenColor.GOLD])
    }

    @Test
    fun `creates bank with seven color tokens for four players`() {
        val players = listOf(
            Player("p1", "Alice"),
            Player("p2", "Bob"),
            Player("p3", "Clara"),
            Player("p4", "David"),
        )

        val state = GameInitializer().createInitialState(players, GameRules())

        assertEquals(7, state.board.bankTokens[TokenColor.WHITE])
        assertEquals(7, state.board.bankTokens[TokenColor.BLUE])
        assertEquals(7, state.board.bankTokens[TokenColor.GREEN])
        assertEquals(7, state.board.bankTokens[TokenColor.RED])
        assertEquals(7, state.board.bankTokens[TokenColor.BLACK])
        assertEquals(5, state.board.bankTokens[TokenColor.GOLD])
    }

    @Test
    fun `rejects unsupported number of players`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameInitializer().createInitialState(listOf(Player("p1", "Solo")), GameRules())
        }
    }
}
