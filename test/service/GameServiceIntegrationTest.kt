package service

import domain.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameServiceIntegrationTest {
    @Test
    fun `game finishes only after round is completed`() {
        val rules = GameRules(pointsToEndGame = 1)
        val gameRepository = InMemoryGameRepository()
        val resultRepository = InMemoryGameResultRepository()
        val playerRepository = InMemoryPlayerRepository()
        val statsRepository = InMemoryPlayerStatisticsRepository()
        val statisticsService = StatisticsService(statsRepository)
        val gameService = GameService(
            gameRepository = gameRepository,
            gameResultRepository = resultRepository,
            playerRepository = playerRepository,
            moveProcessor = MoveProcessor(rules),
            gameInitializer = GameInitializer(),
            scoreCalculator = ScoreCalculator(),
            statisticsService = statisticsService,
            rules = rules,
        )

        val alice = playerRepository.save(Player("p1", "Alice"))
        val bob = playerRepository.save(Player("p2", "Bob"))
        val clara = playerRepository.save(Player("p3", "Clara"))
        val game = gameService.createSession(listOf(alice.id, bob.id, clara.id))
        val winningCard = Card("WIN", CardColor.WHITE, 1, 1, TokenSet.empty)
        val changedRow = game.state.board.market.levelOne.copy(openCards = listOf(winningCard))
        val changedMarket = game.state.board.market.withRow(changedRow)
        val changedState = game.state.copy(board = game.state.board.copy(market = changedMarket))
        gameRepository.save(game.copy(state = changedState))

        val aliceMove = gameService.makeMove(game.id, BuyCardMove("m1", alice.id, "WIN", CardSource.MARKET, TokenSet.empty))
        val afterAlice = gameService.getSession(game.id)!!
        val bobMove = gameService.makeMove(
            game.id,
            TakeTokensMove("m2", bob.id, TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1, TokenColor.GREEN to 1)),
        )
        val afterBob = gameService.getSession(game.id)!!
        val claraMove = gameService.makeMove(
            game.id,
            TakeTokensMove("m3", clara.id, TokenSet.of(TokenColor.WHITE to 1, TokenColor.BLUE to 1, TokenColor.GREEN to 1)),
        )
        val afterClara = gameService.getSession(game.id)!!

        assertTrue(aliceMove.success)
        assertEquals(GameStatus.IN_PROGRESS, afterAlice.status)
        assertTrue(afterAlice.finalRoundStarted)
        assertTrue(bobMove.success)
        assertEquals(GameStatus.IN_PROGRESS, afterBob.status)
        assertTrue(claraMove.success)
        assertEquals(GameStatus.FINISHED, afterClara.status)
    }
}

