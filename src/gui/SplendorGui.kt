package gui

import domain.BuyCardMove
import domain.Card
import domain.CardSource
import domain.GameSession
import domain.ReserveCardMove
import domain.TakeTokensMove
import domain.TokenColor
import domain.TokenSet
import service.ApplicationFactory
import service.ApplicationServices
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.Timer

// Главное окно игры. Оно только показывает данные и вызывает сервисы, а правила остаются в service.
class SplendorGui(
    private val services: ApplicationServices = ApplicationFactory.createWithFiles(),
) : JFrame("Splendor") {
    private val playerListModel = DefaultListModel<String>()
    private val playerList = JList(playerListModel)
    private val playerNameField = JTextField()
    private val statisticsArea = JTextArea()

    private val gameInfo = JTextArea()
    private val marketPanel = JPanel(GridLayout(3, 1, 6, 6))
    private val messageLabel = JLabel("Добавьте игроков или выберите сохраненных.")
    private val timerLabel = JLabel("Время: 00:00")
    private val reservedCardField = JTextField()

    private val tokenSpinners = linkedMapOf<TokenColor, JSpinner>()

    private var currentSessionId: String? = null
    private var selectedCard: Card? = null
    private var gameStartedAt: Instant? = null

    private val timer = Timer(1000) {
        val started = gameStartedAt
        if (started != null) {
            val seconds = Duration.between(started, Instant.now()).seconds
            timerLabel.text = "Время: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        }
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 650)
        layout = BorderLayout(8, 8)

        add(createLeftPanel(), BorderLayout.WEST)
        add(createCenterPanel(), BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.SOUTH)

        refreshPlayers()
        refreshStatistics()
        refreshGame()
        pack()
        setLocationRelativeTo(null)
    }

    private fun createLeftPanel(): JPanel {
        val panel = JPanel(BorderLayout(6, 6))
        panel.preferredSize = Dimension(270, 600)
        panel.border = BorderFactory.createTitledBorder("Игроки")

        playerList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        panel.add(JScrollPane(playerList), BorderLayout.CENTER)

        val controls = JPanel(GridLayout(0, 1, 4, 4))
        controls.add(playerNameField)
        controls.add(JButton("Добавить игрока").also { it.addActionListener { addPlayer() } })
        controls.add(JButton("Новая партия").also { it.addActionListener { createGame() } })

        val toggle = JToggleButton("Показать статистику", true)
        toggle.addActionListener {
            statisticsArea.isVisible = toggle.isSelected
            toggle.text = if (toggle.isSelected) "Скрыть статистику" else "Показать статистику"
        }
        controls.add(toggle)

        statisticsArea.isEditable = false
        statisticsArea.rows = 8
        controls.add(JScrollPane(statisticsArea))

        panel.add(controls, BorderLayout.SOUTH)
        return panel
    }

    private fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout(6, 6))
        panel.border = BorderFactory.createTitledBorder("Партия")

        val top = JPanel(BorderLayout())
        top.add(timerLabel, BorderLayout.WEST)
        top.add(messageLabel, BorderLayout.CENTER)
        panel.add(top, BorderLayout.NORTH)

        gameInfo.isEditable = false
        gameInfo.rows = 10
        panel.add(JScrollPane(gameInfo), BorderLayout.WEST)
        panel.add(marketPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createActionPanel(): JPanel {
        val panel = JPanel(BorderLayout(6, 6))
        panel.border = BorderFactory.createTitledBorder("Ход")

        val tokens = JPanel(GridLayout(1, 5, 4, 4))
        listOf(TokenColor.WHITE, TokenColor.BLUE, TokenColor.GREEN, TokenColor.RED, TokenColor.BLACK).forEach { color ->
            val spinner = JSpinner(SpinnerNumberModel(0, 0, 3, 1))
            tokenSpinners[color] = spinner
            val block = JPanel(BorderLayout())
            block.add(JLabel(color.name.lowercase()), BorderLayout.NORTH)
            block.add(spinner, BorderLayout.CENTER)
            tokens.add(block)
        }
        panel.add(tokens, BorderLayout.CENTER)

        val buttons = JPanel(GridLayout(2, 4, 4, 4))
        buttons.add(JButton("Взять жетоны").also { it.addActionListener { takeTokens() } })
        buttons.add(JButton("Купить с рынка").also { it.addActionListener { buySelectedCard() } })
        buttons.add(JButton("Резерв с рынка").also { it.addActionListener { reserveSelectedCard() } })
        buttons.add(JButton("Завершить игру").also { it.addActionListener { finishGame() } })

        reservedCardField.toolTipText = "ID зарезервированной карты"
        buttons.add(reservedCardField)
        buttons.add(JButton("Купить резерв").also { it.addActionListener { buyReservedCard() } })
        buttons.add(JButton("Резерв колоды 1").also { it.addActionListener { reserveDeckCard(1) } })
        buttons.add(JButton("Резерв колоды 2/3").also { it.addActionListener { reserveDeckCard(askDeckLevel()) } })
        panel.add(buttons, BorderLayout.EAST)
        return panel
    }

    private fun addPlayer() {
        try {
            services.playerService.addPlayer(playerNameField.text)
            playerNameField.text = ""
            refreshPlayers()
            refreshStatistics()
            messageLabel.text = "Игрок сохранен в реестр."
        } catch (ex: IllegalArgumentException) {
            showError(ex.message ?: "Не удалось добавить игрока")
        }
    }

    private fun createGame() {
        val selectedIds = playerList.selectedValuesList.map { it.substringBefore(" ") }
        val allPlayers = services.playerService.listPlayers()
        val ids = if (selectedIds.isEmpty() && allPlayers.size in 2..4) {
            allPlayers.map { it.id }
        } else {
            selectedIds
        }
        try {
            val session = services.gameService.createSession(ids)
            currentSessionId = session.id
            gameStartedAt = Instant.now()
            timer.start()
            selectedCard = null
            refreshGame()
            messageLabel.text = "Партия создана."
        } catch (ex: IllegalArgumentException) {
            showError(ex.message ?: "Не удалось создать партию")
        }
    }

    private fun takeTokens() {
        val session = currentSession() ?: return
        val selectedTokens = tokenSpinners.mapValues { (_, spinner) -> spinner.value as Int }
            .filterValues { it > 0 }
            .map { it.key to it.value }
            .toTypedArray()

        try {
            val move = TakeTokensMove(
                id = UUID.randomUUID().toString(),
                playerId = session.state.getCurrentPlayer().player.id,
                tokens = TokenSet.of(*selectedTokens),
            )
            val result = services.gameService.makeMove(session.id, move)
            messageLabel.text = result.message
            resetTokenSpinners()
            refreshGame()
        } catch (ex: IllegalArgumentException) {
            showError(ex.message ?: "Неверный ход")
        }
    }

    private fun buySelectedCard() {
        val session = currentSession() ?: return
        val card = selectedCard ?: return showError("Сначала выберите карту на рынке")
        val player = session.state.getCurrentPlayer()
        val payment = services.paymentCalculator.calculatePayment(player, card)
            ?: return showError("У игрока не хватает жетонов")

        val move = BuyCardMove(UUID.randomUUID().toString(), player.player.id, card.id, CardSource.MARKET, payment)
        val result = services.gameService.makeMove(session.id, move)
        messageLabel.text = result.message
        selectedCard = null
        refreshGame()
    }

    private fun buyReservedCard() {
        val session = currentSession() ?: return
        val cardId = reservedCardField.text.trim()
        if (cardId.isBlank()) return showError("Введите ID зарезервированной карты")

        val player = session.state.getCurrentPlayer()
        val card = player.reservedCards.firstOrNull { it.id == cardId }
            ?: return showError("У текущего игрока нет такой карты в резерве")
        val payment = services.paymentCalculator.calculatePayment(player, card)
            ?: return showError("У игрока не хватает жетонов")

        val move = BuyCardMove(UUID.randomUUID().toString(), player.player.id, card.id, CardSource.RESERVED, payment)
        val result = services.gameService.makeMove(session.id, move)
        reservedCardField.text = ""
        messageLabel.text = result.message
        refreshGame()
    }

    private fun reserveSelectedCard() {
        val session = currentSession() ?: return
        val card = selectedCard ?: return showError("Сначала выберите карту на рынке")
        val player = session.state.getCurrentPlayer()
        val move = ReserveCardMove(UUID.randomUUID().toString(), player.player.id, card.id, CardSource.MARKET)
        val result = services.gameService.makeMove(session.id, move)
        messageLabel.text = result.message
        selectedCard = null
        refreshGame()
    }

    private fun reserveDeckCard(level: Int) {
        val session = currentSession() ?: return
        val player = session.state.getCurrentPlayer()
        val move = ReserveCardMove(UUID.randomUUID().toString(), player.player.id, level.toString(), CardSource.DECK)
        val result = services.gameService.makeMove(session.id, move)
        messageLabel.text = result.message
        refreshGame()
    }

    private fun askDeckLevel(): Int {
        val value = JOptionPane.showInputDialog(this, "Уровень колоды: 2 или 3", "2") ?: return 2
        return value.toIntOrNull()?.coerceIn(2, 3) ?: 2
    }

    private fun finishGame() {
        val session = currentSession() ?: return
        val result = services.gameService.finishGame(session.id)
        timer.stop()
        messageLabel.text = "Победитель: ${result.winner.name}"
        refreshGame()
        refreshStatistics()
    }

    private fun currentSession(): GameSession? {
        val id = currentSessionId ?: return showError("Сначала создайте партию").let { null }
        return services.gameService.getSession(id)
    }

    private fun refreshPlayers() {
        playerListModel.clear()
        services.playerService.listPlayers().forEach {
            playerListModel.addElement("${it.id} ${it.name}")
        }
    }

    private fun refreshStatistics() {
        val lines = services.statisticsService.listStatistics().map {
            "${it.player.name}: игр ${it.gamesPlayed}, побед ${it.wins}, ${"%.0f".format(it.winRate * 100)}%"
        }
        statisticsArea.text = if (lines.isEmpty()) "Статистики пока нет." else lines.joinToString("\n")
    }

    private fun refreshGame() {
        val session = currentSessionId?.let { services.gameService.getSession(it) }
        if (session == null) {
            gameInfo.text = "Партия не создана."
            marketPanel.removeAll()
            marketPanel.revalidate()
            marketPanel.repaint()
            return
        }

        val state = session.state
        gameInfo.text = buildString {
            appendLine("Статус: ${session.status}")
            appendLine("Банк: ${state.board.bankTokens}")
            appendLine("Ходит: ${state.getCurrentPlayer().player.name}")
            appendLine()
            state.players.forEach { playerState ->
                appendLine("${playerState.player.name}")
                appendLine("  счет: ${playerState.score()}")
                appendLine("  жетоны: ${playerState.tokens}")
                appendLine("  бонусы: ${playerState.calculateBonuses()}")
                appendLine("  резерв: ${playerState.reservedCards.joinToString { it.id }.ifBlank { "-" }}")
            }
        }

        drawMarket(session)
    }

    private fun drawMarket(session: GameSession) {
        marketPanel.removeAll()
        val rows = listOf(session.state.board.market.levelOne, session.state.board.market.levelTwo, session.state.board.market.levelThree)
        rows.forEach { row ->
            val rowPanel = JPanel(GridLayout(1, 4, 4, 4))
            rowPanel.border = BorderFactory.createTitledBorder("Уровень ${row.level}")
            row.openCards.forEach { card ->
                val button = JButton(cardText(card))
                button.addActionListener {
                    selectedCard = card
                    messageLabel.text = "Выбрана карта ${card.id}"
                }
                rowPanel.add(button)
            }
            marketPanel.add(rowPanel)
        }
        marketPanel.revalidate()
        marketPanel.repaint()
    }

    private fun cardText(card: Card): String =
        "<html>${card.id}<br>${card.color}<br>очки: ${card.points}<br>${card.cost}</html>"

    private fun resetTokenSpinners() {
        tokenSpinners.values.forEach { it.value = 0 }
    }

    private fun showError(text: String) {
        messageLabel.text = text
        JOptionPane.showMessageDialog(this, text, "Ошибка", JOptionPane.ERROR_MESSAGE)
    }
}

fun showGui() {
    SwingUtilities.invokeLater {
        SplendorGui().isVisible = true
    }
}
