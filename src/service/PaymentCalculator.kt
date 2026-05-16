package service

import domain.Card
import domain.PlayerState
import domain.TokenColor
import domain.TokenSet


class PaymentCalculator {
    fun requiredCost(player: PlayerState, card: Card): TokenSet {
        val bonuses = player.calculateBonuses()
        return TokenSet(
            TokenColor.entries
                .filter { it != TokenColor.GOLD }
                .associateWith { color -> (card.cost[color] - bonuses[color]).coerceAtLeast(0) }
                .filterValues { it > 0 },
        )
    }

    fun calculatePayment(player: PlayerState, card: Card): TokenSet? {
        val required = requiredCost(player, card)
        val payment = mutableMapOf<TokenColor, Int>()
        var goldNeeded = 0

        for (color in TokenColor.entries.filter { it != TokenColor.GOLD }) {
            val coloredPayment = minOf(player.tokens[color], required[color])
            if (coloredPayment > 0) payment[color] = coloredPayment
            goldNeeded += required[color] - coloredPayment
        }

        if (player.tokens[TokenColor.GOLD] < goldNeeded) return null
        if (goldNeeded > 0) payment[TokenColor.GOLD] = goldNeeded
        return TokenSet(payment)
    }

    fun paymentCovers(player: PlayerState, card: Card, payment: TokenSet): Boolean =
        calculatePayment(player, card) == payment
}
