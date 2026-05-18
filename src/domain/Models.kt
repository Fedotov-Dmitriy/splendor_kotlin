package domain

import java.io.Serializable

data class Player(
    val id: String,
    val name: String,
) : Serializable

data class Card(
    val id: String,
    val color: CardColor,
    val level: Int,
    val points: Int,
    val cost: TokenSet,
) : Serializable

data class Noble(
    val id: String,
    val points: Int,
    val requirement: TokenSet,
) : Serializable

data class PlayerState(
    val player: Player,
    val tokens: TokenSet = TokenSet.empty,
    val purchasedCards: List<Card> = emptyList(),
    val reservedCards: List<Card> = emptyList(),
    val nobles: List<Noble> = emptyList(),
) : Serializable {
    fun calculateBonuses(): TokenSet =
        purchasedCards.fold(TokenSet.empty) { bonuses, card ->
            bonuses.plus(TokenSet.fromCardColor(card.color, 1))
        }

    fun score(): Int = purchasedCards.sumOf { it.points } + nobles.sumOf { it.points }
}

enum class GameStatus {
    CREATED,
    IN_PROGRESS,
    FINISHED,
}

data class CardRow(
    val level: Int,
    val deck: List<Card>,
    val openCards: List<Card>,
) : Serializable {
    fun refill(openSlots: Int = 4): CardRow {
        var newDeck = deck
        val newOpen = openCards.toMutableList()
        while (newOpen.size < openSlots && newDeck.isNotEmpty()) {
            newOpen += newDeck.first()
            newDeck = newDeck.drop(1)
        }
        return copy(deck = newDeck, openCards = newOpen)
    }

    fun removeOpenCard(cardId: String): Pair<Card, CardRow>? {
        val card = openCards.firstOrNull { it.id == cardId } ?: return null
        val row = copy(openCards = openCards.filterNot { it.id == cardId }).refill()
        return card to row
    }

    fun drawFromDeck(): Pair<Card, CardRow>? {
        val card = deck.firstOrNull() ?: return null
        return card to copy(deck = deck.drop(1))
    }
}

data class CardMarket(
    val levelOne: CardRow,
    val levelTwo: CardRow,
    val levelThree: CardRow,
) : Serializable {
    fun row(level: Int): CardRow = when (level) {
        1 -> levelOne
        2 -> levelTwo
        3 -> levelThree
        else -> error("Unsupported card level: $level")
    }

    fun withRow(row: CardRow): CardMarket = when (row.level) {
        1 -> copy(levelOne = row)
        2 -> copy(levelTwo = row)
        3 -> copy(levelThree = row)
        else -> error("Unsupported card level: ${row.level}")
    }

    fun getCard(cardId: String): Card? =
        listOf(levelOne, levelTwo, levelThree).flatMap { it.openCards }.firstOrNull { it.id == cardId }

    fun removeOpenCard(cardId: String): Pair<Card, CardMarket>? {
        for (row in listOf(levelOne, levelTwo, levelThree)) {
            val removed = row.removeOpenCard(cardId)
            if (removed != null) {
                val (card, newRow) = removed
                return card to withRow(newRow)
            }
        }
        return null
    }

    fun drawFromDeck(level: Int): Pair<Card, CardMarket>? {
        val result = row(level).drawFromDeck() ?: return null
        val (card, newRow) = result
        return card to withRow(newRow)
    }
}

data class BoardState(
    val market: CardMarket,
    val nobles: List<Noble>,
    val bankTokens: TokenSet,
) : Serializable

data class GameState(
    val currentPlayerIndex: Int,
    val board: BoardState,
    val players: List<PlayerState>,
) : Serializable {
    fun getCurrentPlayer(): PlayerState = players[currentPlayerIndex]

    fun replacePlayer(playerState: PlayerState): GameState =
        copy(players = players.map { if (it.player.id == playerState.player.id) playerState else it })

    fun nextTurn(): GameState = copy(currentPlayerIndex = (currentPlayerIndex + 1) % players.size)

    fun isFinished(rules: GameRules): Boolean = rules.isGameOver(this)
}

data class GameSession(
    val id: String,
    val players: List<Player>,
    val state: GameState,
    val status: GameStatus = GameStatus.CREATED,
    val moves: List<Move> = emptyList(),
    val finalRoundStarted: Boolean = false,
) : Serializable
