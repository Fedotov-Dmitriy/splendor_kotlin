package main

import domain.*
import service.ApplicationFactory
import service.ApplicationServices
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.UUID

class ConsoleApplication(
    private val services: ApplicationServices = ApplicationFactory.create(),
    input: InputStream = System.`in`,
    private val output: PrintStream = System.out,
) {
    private val reader = BufferedReader(InputStreamReader(input))

    fun run() {
        output.println("Splendor console. Type 'help' for commands.")
        while (true) {
            output.print("> ")
            val line = reader.readLine() ?: break
            val shouldContinue = handle(line)
            if (!shouldContinue) break
        }
    }

    fun handle(rawLine: String): Boolean {
        val line = rawLine.trim()
        if (line.isBlank()) return true

        val args = line.split(Regex("\\s+"))
        try {
            when (args.first().lowercase()) {
                "help" -> printHelp()
                "add-player" -> addPlayer(args.drop(1).joinToString(" "))
                "players" -> listPlayers()
                "new-game" -> createGame(args.drop(1))
                "games" -> listGames()
                "show" -> showGame(args.getOrNull(1))
                "take" -> takeTokens(args)
                "reserve" -> reserveCard(args)
                "buy" -> buyCard(args)
                "return" -> returnTokens(args)
                "finish" -> finishGame(args.getOrNull(1))
                "rating" -> printRating()
                "exit", "quit" -> return false
                else -> output.println("Unknown command: ${args.first()}")
            }
        } catch (ex: IllegalArgumentException) {
            output.println("Error: ${ex.message}")
        } catch (ex: IllegalStateException) {
            output.println("Error: ${ex.message}")
        }

        return true
    }

    private fun printHelp() {
        output.println(
            """
            Commands:
              add-player <name>
              players
              new-game <playerId> <playerId> [playerId] [playerId]
              games
              show <gameId>
              take <gameId> <color=count>...
              reserve <gameId> <cardId> [market|deck]
              buy <gameId> <cardId> [market|reserved]
              return <gameId> <color=count>...
              finish <gameId>
              rating
              exit
            Colors: white, blue, green, red, black, gold.
            """.trimIndent(),
        )
    }

    private fun addPlayer(name: String) {
        val player = services.playerService.addPlayer(name)
        output.println("Player added: ${player.id} ${player.name}")
    }

    private fun listPlayers() {
        val players = services.playerService.listPlayers()
        if (players.isEmpty()) {
            output.println("No players.")
            return
        }
        players.forEach { output.println("${it.id} ${it.name}") }
    }

    private fun createGame(playerIds: List<String>) {
        val session = services.gameService.createSession(playerIds)
        output.println("Game created: ${session.id}")
        printSession(session)
    }

    private fun listGames() {
        val games = services.gameService.listSessions()
        if (games.isEmpty()) {
            output.println("No games.")
            return
        }
        games.forEach {
            output.println("${it.id} status=${it.status} current=${it.state.getCurrentPlayer().player.name}")
        }
    }

    private fun showGame(gameId: String?) {
        val session = requireSession(gameId)
        printSession(session)
    }

    private fun takeTokens(args: List<String>) {
        require(args.size >= 3) { "Usage: take <gameId> <color=count>..." }
        val session = requireSession(args[1])
        val move = TakeTokensMove(UUID.randomUUID().toString(), session.state.getCurrentPlayer().player.id, parseTokens(args.drop(2)))
        printMoveResult(args[1], services.gameService.makeMove(args[1], move))
    }

    private fun reserveCard(args: List<String>) {
        require(args.size >= 3) { "Usage: reserve <gameId> <cardId> [market|deck]" }
        val session = requireSession(args[1])
        val source = parseSource(args.getOrNull(3), default = CardSource.MARKET)
        val move = ReserveCardMove(UUID.randomUUID().toString(), session.state.getCurrentPlayer().player.id, args[2], source)
        printMoveResult(args[1], services.gameService.makeMove(args[1], move))
    }

    private fun buyCard(args: List<String>) {
        require(args.size >= 3) { "Usage: buy <gameId> <cardId> [market|reserved]" }
        val session = requireSession(args[1])
        val source = parseSource(args.getOrNull(3), default = CardSource.MARKET)
        require(source != CardSource.DECK) { "Cards cannot be bought directly from deck" }
        val currentPlayer = session.state.getCurrentPlayer()
        val card = when (source) {
            CardSource.MARKET -> session.state.board.market.getCard(args[2])
            CardSource.RESERVED -> currentPlayer.reservedCards.firstOrNull { it.id == args[2] }
            CardSource.DECK -> null
        } ?: throw IllegalArgumentException("Card is not available")
        val payment = services.paymentCalculator.calculatePayment(currentPlayer, card)
            ?: throw IllegalArgumentException("Current player cannot afford ${card.id}")
        val move = BuyCardMove(UUID.randomUUID().toString(), currentPlayer.player.id, card.id, source, payment)
        printMoveResult(args[1], services.gameService.makeMove(args[1], move))
    }

    private fun returnTokens(args: List<String>) {
        require(args.size >= 3) { "Usage: return <gameId> <color=count>..." }
        val session = requireSession(args[1])
        val move = ReturnTokensMove(UUID.randomUUID().toString(), session.state.getCurrentPlayer().player.id, parseTokens(args.drop(2)))
        printMoveResult(args[1], services.gameService.makeMove(args[1], move))
    }

    private fun finishGame(gameId: String?) {
        val result = services.gameService.finishGame(requireNotNull(gameId) { "Usage: finish <gameId>" })
        output.println("Game finished. Winner: ${result.winner.name}")
        result.results.forEach { output.println("#${it.place} ${it.player.name}: ${it.score} points") }
    }

    private fun printRating() {
        val rating = services.ratingService.getRating()
        if (rating.isEmpty()) {
            output.println("Rating is empty.")
            return
        }
        rating.forEach { output.println("#${it.position} ${it.player.name}: ${"%.2f".format(it.value)}") }
    }

    private fun printMoveResult(gameId: String, result: MoveResult) {
        output.println(result.message)
        if (result.success) {
            services.gameService.getSession(gameId)?.let { printSession(it) }
        }
    }

    private fun printSession(session: GameSession) {
        output.println("Game ${session.id} status=${session.status}")
        output.println("Bank: ${session.state.board.bankTokens}")
        output.println("Current player: ${session.state.getCurrentPlayer().player.name}")
        session.state.players.forEach {
            output.println(
                "${it.player.name}: score=${it.score()} tokens=${it.tokens} bonuses=${it.calculateBonuses()} reserved=${it.reservedCards.size}",
            )
        }
        output.println("Market:")
        listOf(session.state.board.market.levelOne, session.state.board.market.levelTwo, session.state.board.market.levelThree)
            .forEach { row ->
                output.println("  Level ${row.level}:")
                row.openCards.forEach { card ->
                    output.println("    ${card.id} ${card.color} points=${card.points} cost=${card.cost}")
                }
            }
        output.println("Nobles: ${session.state.board.nobles.joinToString { "${it.id}(cost=${it.requirement})" }}")
    }

    private fun requireSession(gameId: String?): GameSession =
        services.gameService.getSession(requireNotNull(gameId) { "Game id is required" })
            ?: throw IllegalArgumentException("Unknown game: $gameId")

    private fun parseTokens(parts: List<String>): TokenSet {
        val pairs = parts.map { part ->
            val split = part.split("=")
            require(split.size == 2) { "Token must look like color=count: $part" }
            parseColor(split[0]) to split[1].toInt()
        }
        return TokenSet.of(*pairs.toTypedArray())
    }

    private fun parseColor(value: String): TokenColor =
        TokenColor.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown token color: $value")

    private fun parseSource(value: String?, default: CardSource): CardSource =
        when (value?.lowercase()) {
            null -> default
            "market" -> CardSource.MARKET
            "reserved" -> CardSource.RESERVED
            "deck" -> CardSource.DECK
            else -> throw IllegalArgumentException("Unknown card source: $value")
        }
}
