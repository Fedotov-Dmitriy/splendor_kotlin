package service

import domain.BuyCardMove
import domain.CardSource
import domain.GameRules
import domain.TokenColor
import domain.TokenSet
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuyCardValidatorTest {
    private val players = listOf(domain.Player("p1", "Alice"), domain.Player("p2", "Bob"))
    private val state = GameInitializer().createInitialState(players, GameRules())
    private val validator = BuyCardValidator(PaymentCalculator())

    @Test
    fun `accepts correct payment`() {
        val stateWithTokens = state.replacePlayer(
            state.getCurrentPlayer().copy(tokens = TokenSet.of(TokenColor.BLUE to 1, TokenColor.GREEN to 1)),
        )
        val move = BuyCardMove("m1", "p1", "L1-W1", CardSource.MARKET, TokenSet.of(TokenColor.BLUE to 1, TokenColor.GREEN to 1))

        val result = validator.validate(move, stateWithTokens)

        assertTrue(result.valid)
    }

    @Test
    fun `rejects wrong payment`() {
        val move = BuyCardMove("m1", "p1", "L1-W1", CardSource.MARKET, TokenSet.empty)

        val result = validator.validate(move, state)

        assertFalse(result.valid)
    }
}

