package service

import domain.*


class GameInitializer {
    fun createInitialState(players: List<Player>, rules: GameRules): GameState {
        require(players.size in 2..4) { "Splendor supports 2 to 4 players" }

        val market = CardMarket(
            levelOne = CardRow(1, levelOneDeck(), emptyList()).refill(rules.openCardsPerRow),
            levelTwo = CardRow(2, levelTwoDeck(), emptyList()).refill(rules.openCardsPerRow),
            levelThree = CardRow(3, levelThreeDeck(), emptyList()).refill(rules.openCardsPerRow),
        )

        val colorTokens = when (players.size) {
            2 -> 4
            3 -> 5
            else -> 7
        }

        val bank = TokenSet.of(
            TokenColor.WHITE to colorTokens,
            TokenColor.BLUE to colorTokens,
            TokenColor.GREEN to colorTokens,
            TokenColor.RED to colorTokens,
            TokenColor.BLACK to colorTokens,
            TokenColor.GOLD to 5,
        )

        return GameState(
            currentPlayerIndex = 0,
            board = BoardState(market, nobles(), bank),
            players = players.map { PlayerState(it) },
        )
    }

    private fun levelOneDeck(): List<Card> = listOf(
        card("L1-W1", CardColor.WHITE, 1, 0, TokenColor.BLUE to 1, TokenColor.GREEN to 1),
        card("L1-U1", CardColor.BLUE, 1, 0, TokenColor.RED to 1, TokenColor.BLACK to 1),
        card("L1-G1", CardColor.GREEN, 1, 0, TokenColor.WHITE to 1, TokenColor.BLUE to 1),
        card("L1-R1", CardColor.RED, 1, 0, TokenColor.GREEN to 1, TokenColor.BLACK to 1),
        card("L1-B1", CardColor.BLACK, 1, 0, TokenColor.WHITE to 1, TokenColor.RED to 1),
        card("L1-W2", CardColor.WHITE, 1, 1, TokenColor.BLUE to 2, TokenColor.GREEN to 1, TokenColor.RED to 1),
        card("L1-U2", CardColor.BLUE, 1, 1, TokenColor.WHITE to 1, TokenColor.RED to 2, TokenColor.BLACK to 1),
        card("L1-G2", CardColor.GREEN, 1, 1, TokenColor.WHITE to 1, TokenColor.BLUE to 1, TokenColor.BLACK to 2),
        card("L1-R2", CardColor.RED, 1, 1, TokenColor.WHITE to 2, TokenColor.GREEN to 1, TokenColor.BLACK to 1),
        card("L1-B2", CardColor.BLACK, 1, 1, TokenColor.WHITE to 1, TokenColor.BLUE to 2, TokenColor.RED to 1),
    )

    private fun levelTwoDeck(): List<Card> = listOf(
        card("L2-W1", CardColor.WHITE, 2, 1, TokenColor.BLUE to 2, TokenColor.GREEN to 2, TokenColor.RED to 2),
        card("L2-U1", CardColor.BLUE, 2, 1, TokenColor.WHITE to 2, TokenColor.RED to 2, TokenColor.BLACK to 2),
        card("L2-G1", CardColor.GREEN, 2, 2, TokenColor.WHITE to 3, TokenColor.BLUE to 2, TokenColor.BLACK to 2),
        card("L2-R1", CardColor.RED, 2, 2, TokenColor.WHITE to 2, TokenColor.GREEN to 3, TokenColor.BLACK to 2),
        card("L2-B1", CardColor.BLACK, 2, 2, TokenColor.WHITE to 2, TokenColor.BLUE to 3, TokenColor.RED to 2),
        card("L2-W2", CardColor.WHITE, 2, 3, TokenColor.BLUE to 4, TokenColor.GREEN to 2),
        card("L2-U2", CardColor.BLUE, 2, 3, TokenColor.RED to 4, TokenColor.BLACK to 2),
    )

    private fun levelThreeDeck(): List<Card> = listOf(
        card("L3-W1", CardColor.WHITE, 3, 3, TokenColor.BLUE to 3, TokenColor.GREEN to 3, TokenColor.RED to 3),
        card("L3-U1", CardColor.BLUE, 3, 4, TokenColor.WHITE to 3, TokenColor.RED to 3, TokenColor.BLACK to 4),
        card("L3-G1", CardColor.GREEN, 3, 4, TokenColor.WHITE to 4, TokenColor.BLUE to 3, TokenColor.BLACK to 3),
        card("L3-R1", CardColor.RED, 3, 5, TokenColor.WHITE to 3, TokenColor.GREEN to 4, TokenColor.BLACK to 3),
        card("L3-B1", CardColor.BLACK, 3, 5, TokenColor.WHITE to 4, TokenColor.BLUE to 4),
        card("L3-W2", CardColor.WHITE, 3, 5, TokenColor.BLUE to 5, TokenColor.GREEN to 3),
    )

    private fun nobles(): List<Noble> = listOf(
        Noble("N1", 3, TokenSet.of(TokenColor.WHITE to 3, TokenColor.BLUE to 3, TokenColor.GREEN to 3)),
        Noble("N2", 3, TokenSet.of(TokenColor.RED to 3, TokenColor.BLACK to 3, TokenColor.WHITE to 3)),
        Noble("N3", 3, TokenSet.of(TokenColor.BLUE to 4, TokenColor.GREEN to 4)),
        Noble("N4", 3, TokenSet.of(TokenColor.RED to 4, TokenColor.BLACK to 4)),
        Noble("N5", 3, TokenSet.of(TokenColor.WHITE to 4, TokenColor.BLACK to 4)),
    )

    private fun card(
        id: String,
        color: CardColor,
        level: Int,
        points: Int,
        vararg cost: Pair<TokenColor, Int>,
    ): Card = Card(id, color, level, points, TokenSet.of(*cost))
}
