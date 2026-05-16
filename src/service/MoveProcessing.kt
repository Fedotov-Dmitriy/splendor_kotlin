package service

import domain.*


interface MoveValidator<T : Move> {
    fun validate(move: T, state: GameState): ValidationResult
}

interface MoveApplier<T : Move> {
    fun apply(move: T, state: GameState): GameState
}

class TakeTokensValidator(private val rules: GameRules) : MoveValidator<TakeTokensMove> {
    override fun validate(move: TakeTokensMove, state: GameState): ValidationResult {
        if (move.tokens.isEmpty()) return ValidationResult.invalid("Choose at least one token")
        if (move.tokens[TokenColor.GOLD] > 0) return ValidationResult.invalid("Gold can only be taken by reserving a card")
        if (!state.board.bankTokens.containsAtLeast(move.tokens)) return ValidationResult.invalid("Bank does not have enough tokens")

        val colors = move.tokens.colorsWithTokens()
        val total = move.tokens.total()
        val sameColor = colors.size == 1
        val validPattern =
            if (sameColor) {
                total == 2 && state.board.bankTokens[colors.single()] >= 4
            } else {
                total == 3 && colors.size == 3 && colors.all { move.tokens[it] == 1 }
            }

        if (!validPattern) return ValidationResult.invalid("Take exactly 3 different colors or 2 tokens of one color with at least 4 in bank")

        val player = state.getCurrentPlayer()
        if (player.tokens.plus(move.tokens).total() > rules.maxTokensPerPlayer) {
            return ValidationResult.invalid("Player cannot hold more than ${rules.maxTokensPerPlayer} tokens")
        }

        return ValidationResult.ok
    }
}

class TakeTokensApplier : MoveApplier<TakeTokensMove> {
    override fun apply(move: TakeTokensMove, state: GameState): GameState {
        val player = state.getCurrentPlayer()
        val updatedPlayer = player.copy(tokens = player.tokens.plus(move.tokens))
        val updatedBoard = state.board.copy(bankTokens = state.board.bankTokens.minus(move.tokens))
        return state.copy(board = updatedBoard).replacePlayer(updatedPlayer)
    }
}

class ReturnTokensValidator : MoveValidator<ReturnTokensMove> {
    override fun validate(move: ReturnTokensMove, state: GameState): ValidationResult {
        val player = state.getCurrentPlayer()
        if (move.tokens.isEmpty()) return ValidationResult.invalid("Choose at least one token to return")
        if (!player.tokens.containsAtLeast(move.tokens)) return ValidationResult.invalid("Player does not have these tokens")
        return ValidationResult.ok
    }
}

class ReturnTokensApplier : MoveApplier<ReturnTokensMove> {
    override fun apply(move: ReturnTokensMove, state: GameState): GameState {
        val player = state.getCurrentPlayer()
        val updatedPlayer = player.copy(tokens = player.tokens.minus(move.tokens))
        val updatedBoard = state.board.copy(bankTokens = state.board.bankTokens.plus(move.tokens))
        return state.copy(board = updatedBoard).replacePlayer(updatedPlayer)
    }
}

class ReserveCardValidator(private val rules: GameRules) : MoveValidator<ReserveCardMove> {
    override fun validate(move: ReserveCardMove, state: GameState): ValidationResult {
        val player = state.getCurrentPlayer()
        if (player.reservedCards.size >= rules.maxReservedCards) {
            return ValidationResult.invalid("Player cannot reserve more than ${rules.maxReservedCards} cards")
        }

        val cardExists = when (move.source) {
            CardSource.MARKET -> state.board.market.getCard(move.cardId) != null
            CardSource.DECK -> move.cardId.toIntOrNull() in 1..3 && state.board.market.row(move.cardId.toInt()).deck.isNotEmpty()
            CardSource.RESERVED -> false
        }
        if (!cardExists) return ValidationResult.invalid("Card is not available for reservation")

        val takesGold = state.board.bankTokens[TokenColor.GOLD] > 0
        if (takesGold && player.tokens.total() + 1 > rules.maxTokensPerPlayer) {
            return ValidationResult.invalid("Player cannot hold more than ${rules.maxTokensPerPlayer} tokens")
        }

        return ValidationResult.ok
    }
}

class ReserveCardApplier : MoveApplier<ReserveCardMove> {
    override fun apply(move: ReserveCardMove, state: GameState): GameState {
        val (card, newMarket) = when (move.source) {
            CardSource.MARKET -> state.board.market.removeOpenCard(move.cardId) ?: error("Validated market card disappeared")
            CardSource.DECK -> state.board.market.drawFromDeck(move.cardId.toInt()) ?: error("Validated deck card disappeared")
            CardSource.RESERVED -> error("Cannot reserve reserved card")
        }

        val player = state.getCurrentPlayer()
        val gold = if (state.board.bankTokens[TokenColor.GOLD] > 0) TokenSet.single(TokenColor.GOLD) else TokenSet.empty
        val updatedPlayer = player.copy(tokens = player.tokens.plus(gold), reservedCards = player.reservedCards + card)
        val updatedBoard = state.board.copy(
            market = newMarket,
            bankTokens = if (gold.isEmpty()) state.board.bankTokens else state.board.bankTokens.minus(gold),
        )
        return state.copy(board = updatedBoard).replacePlayer(updatedPlayer)
    }
}

class BuyCardValidator(private val paymentCalculator: PaymentCalculator) : MoveValidator<BuyCardMove> {
    override fun validate(move: BuyCardMove, state: GameState): ValidationResult {
        val player = state.getCurrentPlayer()
        val card = findCard(move, state) ?: return ValidationResult.invalid("Card is not available for purchase")
        if (!player.tokens.containsAtLeast(move.payment)) return ValidationResult.invalid("Player does not have the payment tokens")
        if (!paymentCalculator.paymentCovers(player, card, move.payment)) {
            val expected = paymentCalculator.calculatePayment(player, card)
            return ValidationResult.invalid("Payment does not match required cost; expected ${expected ?: "unaffordable"}")
        }
        return ValidationResult.ok
    }

    private fun findCard(move: BuyCardMove, state: GameState): Card? = when (move.source) {
        CardSource.MARKET -> state.board.market.getCard(move.cardId)
        CardSource.RESERVED -> state.getCurrentPlayer().reservedCards.firstOrNull { it.id == move.cardId }
        CardSource.DECK -> null
    }
}

class BuyCardApplier : MoveApplier<BuyCardMove> {
    override fun apply(move: BuyCardMove, state: GameState): GameState {
        val player = state.getCurrentPlayer()
        val (card, market) = when (move.source) {
            CardSource.MARKET -> {
                val (card, newMarket) = state.board.market.removeOpenCard(move.cardId) ?: error("Validated market card disappeared")
                card to newMarket
            }
            CardSource.RESERVED -> {
                val card = player.reservedCards.firstOrNull { it.id == move.cardId } ?: error("Validated reserved card disappeared")
                card to state.board.market
            }
            CardSource.DECK -> error("Cannot buy directly from deck")
        }

        val updatedPlayer = player.copy(
            tokens = player.tokens.minus(move.payment),
            purchasedCards = player.purchasedCards + card,
            reservedCards = player.reservedCards.filterNot { it.id == card.id },
        )
        val updatedBoard = state.board.copy(
            market = market,
            bankTokens = state.board.bankTokens.plus(move.payment),
        )
        return state.copy(board = updatedBoard).replacePlayer(updatedPlayer)
    }
}

class NobleEvaluator {
    fun evaluate(playerState: PlayerState, nobles: List<Noble>): List<Noble> {
        val bonuses = playerState.calculateBonuses()
        return nobles.filter { bonuses.containsAtLeast(it.requirement) }.take(1)
    }
}

class MoveProcessor(
    private val rules: GameRules,
    private val nobleEvaluator: NobleEvaluator = NobleEvaluator(),
    private val paymentCalculator: PaymentCalculator = PaymentCalculator(),
) {
    private val takeTokensValidator = TakeTokensValidator(rules)
    private val takeTokensApplier = TakeTokensApplier()
    private val buyCardValidator = BuyCardValidator(paymentCalculator)
    private val buyCardApplier = BuyCardApplier()
    private val reserveCardValidator = ReserveCardValidator(rules)
    private val reserveCardApplier = ReserveCardApplier()
    private val returnTokensValidator = ReturnTokensValidator()
    private val returnTokensApplier = ReturnTokensApplier()

    fun process(move: Move, state: GameState): MoveResult {
        if (state.getCurrentPlayer().player.id != move.playerId) {
            return MoveResult(false, "It is not this player's turn", state)
        }

        val validation = validate(move, state)
        if (!validation.valid) return MoveResult(false, validation.reason, state)

        val applied = apply(move, state)
        val withNobles = assignNobles(applied)
        val next = if (move is ReturnTokensMove) withNobles else withNobles.nextTurn()
        return MoveResult(true, "Move accepted", next)
    }

    private fun validate(move: Move, state: GameState): ValidationResult = when (move) {
        is TakeTokensMove -> takeTokensValidator.validate(move, state)
        is BuyCardMove -> buyCardValidator.validate(move, state)
        is ReserveCardMove -> reserveCardValidator.validate(move, state)
        is ReturnTokensMove -> returnTokensValidator.validate(move, state)
    }

    private fun apply(move: Move, state: GameState): GameState = when (move) {
        is TakeTokensMove -> takeTokensApplier.apply(move, state)
        is BuyCardMove -> buyCardApplier.apply(move, state)
        is ReserveCardMove -> reserveCardApplier.apply(move, state)
        is ReturnTokensMove -> returnTokensApplier.apply(move, state)
    }

    private fun assignNobles(state: GameState): GameState {
        val player = state.getCurrentPlayer()
        val availableNobles = state.board.nobles
        val earned = nobleEvaluator.evaluate(player, availableNobles)
        if (earned.isEmpty()) return state

        val updatedPlayer = player.copy(nobles = player.nobles + earned)
        val updatedBoard = state.board.copy(nobles = availableNobles - earned.toSet())
        return state.copy(board = updatedBoard).replacePlayer(updatedPlayer)
    }
}
