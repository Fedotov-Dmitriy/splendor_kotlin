package domain

import java.io.Serializable

enum class TokenColor {
    WHITE,
    BLUE,
    GREEN,
    RED,
    BLACK,
    GOLD,
}

enum class CardColor {
    WHITE,
    BLUE,
    GREEN,
    RED,
    BLACK,
}

data class TokenSet(private val values: Map<TokenColor, Int> = emptyMap()) : Serializable {
    init {
        require(values.values.all { it >= 0 }) { "Token counts cannot be negative" }
    }

    operator fun get(color: TokenColor): Int = values[color] ?: 0

    fun plus(other: TokenSet): TokenSet =
        TokenSet(TokenColor.entries.associateWith { this[it] + other[it] }.filterValues { it > 0 })

    fun minus(other: TokenSet): TokenSet {
        require(containsAtLeast(other)) { "Cannot subtract more tokens than available" }
        return TokenSet(TokenColor.entries.associateWith { this[it] - other[it] }.filterValues { it > 0 })
    }

    fun containsAtLeast(other: TokenSet): Boolean = TokenColor.entries.all { this[it] >= other[it] }

    fun total(): Int = TokenColor.entries.sumOf { this[it] }

    fun isEmpty(): Boolean = total() == 0

    fun withoutGold(): TokenSet = TokenSet(values - TokenColor.GOLD)

    fun colorsWithTokens(): List<TokenColor> = TokenColor.entries.filter { this[it] > 0 }

    fun asMap(): Map<TokenColor, Int> = values.toMap()

    override fun toString(): String {
        if (values.isEmpty()) return "-"
        return TokenColor.entries
            .filter { this[it] > 0 }
            .joinToString(" ") { "${it.name.lowercase()}=${this[it]}" }
    }

    companion object {
        val empty = TokenSet()

        fun of(vararg pairs: Pair<TokenColor, Int>): TokenSet =
            TokenSet(pairs.toMap().filterValues { it > 0 })

        fun single(color: TokenColor, count: Int = 1): TokenSet = of(color to count)

        fun fromCardColor(color: CardColor, count: Int): TokenSet =
            of(TokenColor.valueOf(color.name) to count)
    }
}

fun CardColor.toTokenColor(): TokenColor = TokenColor.valueOf(name)
