package domain

import java.io.Serializable
import java.time.Clock

enum class MoveType {
    TAKE_TOKENS,
    BUY_CARD,
    RESERVE_CARD,
    RETURN_TOKENS,
}

enum class CardSource {
    MARKET,
    RESERVED,
    DECK,
}

sealed class Move(
    open val id: String,
    open val playerId: String,
    open val type: MoveType,
    open val createdAt: Long,
) : Serializable {
    companion object {
        fun now(clock: Clock = Clock.systemUTC()): Long = clock.millis()
    }
}

data class TakeTokensMove(
    override val id: String,
    override val playerId: String,
    val tokens: TokenSet,
    override val createdAt: Long = Move.now(),
) : Move(id, playerId, MoveType.TAKE_TOKENS, createdAt)

data class BuyCardMove(
    override val id: String,
    override val playerId: String,
    val cardId: String,
    val source: CardSource,
    val payment: TokenSet,
    override val createdAt: Long = Move.now(),
) : Move(id, playerId, MoveType.BUY_CARD, createdAt)

data class ReserveCardMove(
    override val id: String,
    override val playerId: String,
    val cardId: String,
    val source: CardSource,
    override val createdAt: Long = Move.now(),
) : Move(id, playerId, MoveType.RESERVE_CARD, createdAt)

data class ReturnTokensMove(
    override val id: String,
    override val playerId: String,
    val tokens: TokenSet,
    override val createdAt: Long = Move.now(),
) : Move(id, playerId, MoveType.RETURN_TOKENS, createdAt)
