package service

import domain.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveProcessorTest {
    private val players = listOf(Player("p1", "Alice"), Player("p2", "Bob"))
    private val rules = GameRules()
    private val initialState = GameInitializer().createInitialState(players, rules)
    private val processor = MoveProcessor(rules)

    @Test
    fun `accepts taking three different tokens and advances turn`() {
        val result = processor.process(
            TakeTokensMove("m1", "p1", TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1, TokenColor.GREEN to 1)),
            initialState,
        )

        assertTrue(result.success)
        assertEquals(1, result.newState.currentPlayerIndex)
        assertEquals(1, result.newState.players.first { it.player.id == "p1" }.tokens[TokenColor.WHITE])
        assertEquals(3, result.newState.board.bankTokens[TokenColor.WHITE])
    }

    @Test
    fun `rejects taking two same tokens when bank has fewer than four`() {
        val state = initialState.copy(board = initialState.board.copy(bankTokens = initialState.board.bankTokens.minus(TokenSet.of(TokenColor.RED to 1))))

        val result = processor.process(
            TakeTokensMove("m1", "p1", TokenSet.of(TokenColor.RED to 2)),
            state,
        )

        assertFalse(result.success)
    }

    @Test
    fun `rejects taking one token`() {
        val result = processor.process(
            TakeTokensMove("m1", "p1", TokenSet.of(TokenColor.WHITE to 1)),
            initialState,
        )

        assertFalse(result.success)
    }

    @Test
    fun `rejects taking two different tokens`() {
        val result = processor.process(
            TakeTokensMove("m1", "p1", TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1)),
            initialState,
        )

        assertFalse(result.success)
    }

    @Test
    fun `rejects move by non current player`() {
        val result = processor.process(
            TakeTokensMove("m1", "p2", TokenSet.of(TokenColor.WHITE to 1)),
            initialState,
        )

        assertFalse(result.success)
        assertEquals("It is not this player's turn", result.message)
    }

    @Test
    fun `buys available card and refills market`() {
        val stateWithTokens = initialState.replacePlayer(
            initialState.getCurrentPlayer().copy(tokens = TokenSet.of(TokenColor.BLUE to 1, TokenColor.GREEN to 1)),
        )
        val card = stateWithTokens.board.market.getCard("L1-W1")!!
        val payment = PaymentCalculator().calculatePayment(stateWithTokens.getCurrentPlayer(), card)!!

        val result = processor.process(
            BuyCardMove("m1", "p1", "L1-W1", CardSource.MARKET, payment),
            stateWithTokens,
        )

        val alice = result.newState.players.first { it.player.id == "p1" }
        assertTrue(result.success)
        assertEquals(listOf("L1-W1"), alice.purchasedCards.map { it.id })
        assertEquals(4, result.newState.board.market.levelOne.openCards.size)
    }

    @Test
    fun `gives only one noble per move`() {
        val cards = listOf(
            Card("w1", CardColor.WHITE, 1, 0, TokenSet.empty),
            Card("w2", CardColor.WHITE, 1, 0, TokenSet.empty),
            Card("w3", CardColor.WHITE, 1, 0, TokenSet.empty),
            Card("u1", CardColor.BLUE, 1, 0, TokenSet.empty),
            Card("u2", CardColor.BLUE, 1, 0, TokenSet.empty),
            Card("u3", CardColor.BLUE, 1, 0, TokenSet.empty),
        )
        val player = PlayerState(Player("p1", "Alice"), purchasedCards = cards)
        val nobles = listOf(
            Noble("n1", 3, TokenSet.of(TokenColor.WHITE to 3)),
            Noble("n2", 3, TokenSet.of(TokenColor.BLUE to 3)),
        )

        val earned = NobleEvaluator().evaluate(player, nobles)

        assertEquals(1, earned.size)
    }
}
