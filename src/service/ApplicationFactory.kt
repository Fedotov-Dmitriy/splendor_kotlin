package service

import domain.GameRules
import java.nio.file.Path
import java.nio.file.Paths


data class ApplicationServices(
    val playerService: PlayerService,
    val gameService: GameService,
    val statisticsService: StatisticsService,
    val ratingService: RatingService,
    val paymentCalculator: PaymentCalculator,
)

object ApplicationFactory {
    fun create(rules: GameRules = GameRules()): ApplicationServices {
        val playerRepository = InMemoryPlayerRepository()
        val gameRepository = InMemoryGameRepository()
        val resultRepository = InMemoryGameResultRepository()
        val statsRepository = InMemoryPlayerStatisticsRepository()
        val statisticsService = StatisticsService(statsRepository)
        val scoreCalculator = ScoreCalculator()
        val paymentCalculator = PaymentCalculator()
        val moveProcessor = MoveProcessor(rules, paymentCalculator = paymentCalculator)
        val gameService = GameService(
            gameRepository = gameRepository,
            gameResultRepository = resultRepository,
            playerRepository = playerRepository,
            moveProcessor = moveProcessor,
            gameInitializer = GameInitializer(),
            scoreCalculator = scoreCalculator,
            statisticsService = statisticsService,
            rules = rules,
        )
        return ApplicationServices(
            playerService = PlayerService(playerRepository, statsRepository),
            gameService = gameService,
            statisticsService = statisticsService,
            ratingService = RatingService(statsRepository),
            paymentCalculator = paymentCalculator,
        )
    }

    fun createWithFiles(
        registryDir: Path = Paths.get(System.getProperty("user.home"), ".splendor_kotlin"),
        rules: GameRules = GameRules(),
    ): ApplicationServices {
        val playerRepository = FilePlayerRepository(registryDir.resolve("players.tsv"))
        val gameRepository = InMemoryGameRepository()
        val resultRepository = InMemoryGameResultRepository()
        val statsRepository = FilePlayerStatisticsRepository(registryDir.resolve("stats.tsv"))
        val statisticsService = StatisticsService(statsRepository)
        val scoreCalculator = ScoreCalculator()
        val paymentCalculator = PaymentCalculator()
        val moveProcessor = MoveProcessor(rules, paymentCalculator = paymentCalculator)
        val gameService = GameService(
            gameRepository = gameRepository,
            gameResultRepository = resultRepository,
            playerRepository = playerRepository,
            moveProcessor = moveProcessor,
            gameInitializer = GameInitializer(),
            scoreCalculator = scoreCalculator,
            statisticsService = statisticsService,
            rules = rules,
        )
        return ApplicationServices(
            playerService = PlayerService(playerRepository, statsRepository),
            gameService = gameService,
            statisticsService = statisticsService,
            ratingService = RatingService(statsRepository),
            paymentCalculator = paymentCalculator,
        )
    }
}
