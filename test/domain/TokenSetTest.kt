package domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenSetTest {
    @Test
    fun `adds and subtracts token sets`() {
        val first = TokenSet.of(TokenColor.WHITE to 2, TokenColor.BLUE to 1)
        val second = TokenSet.of(TokenColor.WHITE to 1, TokenColor.GOLD to 1)

        val sum = first.plus(second)
        val difference = sum.minus(TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1))

        assertEquals(3, sum[TokenColor.WHITE])
        assertEquals(1, sum[TokenColor.GOLD])
        assertEquals(2, difference[TokenColor.WHITE])
        assertEquals(1, difference[TokenColor.GOLD])
    }

    @Test
    fun `rejects subtracting unavailable tokens`() {
        val tokens = TokenSet.of(TokenColor.RED to 1)

        assertThrows(IllegalArgumentException::class.java) {
            tokens.minus(TokenSet.of(TokenColor.RED to 2))
        }
    }

    @Test
    fun `checks containment and totals`() {
        val tokens = TokenSet.of(TokenColor.GREEN to 3, TokenColor.BLACK to 2)

        assertTrue(tokens.containsAtLeast(TokenSet.of(TokenColor.GREEN to 2)))
        assertFalse(tokens.containsAtLeast(TokenSet.of(TokenColor.BLUE to 1)))
        assertEquals(5, tokens.total())
    }
}

