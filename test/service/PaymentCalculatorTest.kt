package service

import domain.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PaymentCalculatorTest {
    private val calculator = PaymentCalculator()

    @Test
    fun `calculates exact payment with bonuses and gold`() {
        val bonusCard = Card("bonus", CardColor.WHITE, 1, 0, TokenSet.empty)
        val target = Card(
            "target",
            CardColor.BLUE,
            1,
            0,
            TokenSet.of(TokenColor.WHITE to 2, TokenColor.BLUE to 1),
        )
        val player = PlayerState(
            player = Player("p1", "Alice"),
            tokens = TokenSet.of(TokenColor.GOLD to 1, TokenColor.BLUE to 1),
            purchasedCards = listOf(bonusCard),
        )

        val payment = calculator.calculatePayment(player, target)

        assertEquals(TokenSet.of(TokenColor.GOLD to 1, TokenColor.BLUE to 1), payment)
    }

    @Test
    fun `returns null when card is unaffordable`() {
        val target = Card("target", CardColor.BLUE, 1, 0, TokenSet.of(TokenColor.WHITE to 3))
        val player = PlayerState(Player("p1", "Alice"), tokens = TokenSet.of(TokenColor.WHITE to 1))

        assertNull(calculator.calculatePayment(player, target))
    }
}
