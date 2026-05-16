package service

import domain.Player
import domain.PlayerStatistics
import java.nio.file.Files
import java.nio.file.Path

// Простой файловый реестр игроков. Каждая строка файла: id, имя.
class FilePlayerRepository(private val file: Path) : PlayerRepository {
    private val players = linkedMapOf<String, Player>()

    init {
        load()
    }

    override fun save(player: Player): Player {
        players[player.id] = player
        saveAll()
        return player
    }

    override fun findById(id: String): Player? = players[id]

    override fun findAll(): List<Player> = players.values.toList()

    private fun load() {
        if (!Files.exists(file)) return
        Files.readAllLines(file).forEach { line ->
            val parts = line.split("\t")
            if (parts.size >= 2) {
                players[parts[0]] = Player(parts[0], parts[1])
            }
        }
    }

    private fun saveAll() {
        Files.createDirectories(file.parent)
        val lines = players.values.map { "${it.id}\t${it.name}" }
        Files.write(file, lines)
    }
}

// Локальная статистика хранится отдельно от игроков, чтобы ее можно было обновлять после партий.
class FilePlayerStatisticsRepository(private val file: Path) : PlayerStatisticsRepository {
    private val statistics = linkedMapOf<String, PlayerStatistics>()

    init {
        load()
    }

    override fun save(stats: PlayerStatistics): PlayerStatistics {
        statistics[stats.player.id] = stats
        saveAll()
        return stats
    }

    override fun findByPlayerId(id: String): PlayerStatistics? = statistics[id]

    override fun findAll(): List<PlayerStatistics> = statistics.values.toList()

    private fun load() {
        if (!Files.exists(file)) return
        Files.readAllLines(file).forEach { line ->
            val parts = line.split("\t")
            if (parts.size >= 4) {
                val player = Player(parts[0], parts[1])
                statistics[player.id] = PlayerStatistics(
                    player = player,
                    gamesPlayed = parts[2].toIntOrNull() ?: 0,
                    wins = parts[3].toIntOrNull() ?: 0,
                )
            }
        }
    }

    private fun saveAll() {
        Files.createDirectories(file.parent)
        val lines = statistics.values.map {
            "${it.player.id}\t${it.player.name}\t${it.gamesPlayed}\t${it.wins}"
        }
        Files.write(file, lines)
    }
}
