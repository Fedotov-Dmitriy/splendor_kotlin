# Splendor — Architecture Diagrams
 
## 1. Игровая сессия и доска
 
`GameSession` — корневая сущность. 
```mermaid
classDiagram
    GameSession *-- "1" GameState
    GameSession o-- "2..4" Player
    GameSession *-- "0..*" Move
    GameState *-- "1" BoardState
    GameState *-- "2..4" PlayerState
    BoardState *-- "1" CardMarket
    BoardState o-- "0..*" Noble
    BoardState *-- "1" TokenSet
    CardMarket *-- "3" CardRow
    CardRow o-- "0..*" Card
 
    class GameSession {
        +String id
        +GameStatus status
    }
    class GameState {
        +Int currentPlayerIndex
        +isFinished() Boolean
        +getCurrentPlayer() PlayerState
    }
    class BoardState {
        +TokenSet bankTokens
    }
    class CardMarket {
        +CardRow levelOne
        +CardRow levelTwo
        +CardRow levelThree
        +getCard(id) Card
        +drawFromDeck(level) Card
    }
    class CardRow {
        +Int level
        +List~Card~ deck
        +List~Card~ openCards
        +refill()
    }
    class Player {
        +String id
        +String name
    }
    class GameStatus {
        <<enumeration>>
        CREATED
        IN_PROGRESS
        FINISHED
    }
```
 
---
 
## 2. Игрок и доменные объекты
 
```mermaid
classDiagram
    PlayerState --> "1" Player
    PlayerState *-- "1" TokenSet
    PlayerState o-- "0..*" Card
    PlayerState o-- "0..*" Noble
    Card *-- "1" TokenSet
    Noble *-- "1" TokenSet
 
    class Player {
        +String id
        +String name
    }
    class PlayerState {
        +TokenSet tokens
        +List~Card~ purchasedCards
        +List~Card~ reservedCards
        +List~Noble~ nobles
        +calculateBonuses() TokenSet
    }
    class Card {
        +String id
        +CardColor color
        +Int level
        +Int points
        +TokenSet cost
    }
    class Noble {
        +String id
        +Int points
        +TokenSet requirement
    }
    class TokenSet {
        +Map~TokenColor~ tokens
        +plus(other) TokenSet
        +minus(other) TokenSet
        +containsAtLeast(other) Boolean
        +total() Int
    }
    class CardColor {
        <<enumeration>>
        WHITE
        BLUE
        GREEN
        RED
        BLACK
    }
    class TokenColor {
        <<enumeration>>
        WHITE
        BLUE
        GREEN
        RED
        BLACK
        GOLD
    }
```
 
---
 
## 3. Ходы
 
```mermaid
classDiagram
    Move <|-- TakeTokensMove
    Move <|-- BuyCardMove
    Move <|-- ReserveCardMove
    Move <|-- ReturnTokensMove
 
    class Move {
        <<abstract>>
        +String id
        +String playerId
        +MoveType type
        +Long createdAt
    }
    class TakeTokensMove {
        +TokenSet tokens
    }
    class BuyCardMove {
        +String cardId
        +CardSource source
        +TokenSet payment
    }
    class ReserveCardMove {
        +String cardId
        +CardSource source
    }
    class ReturnTokensMove {
        +TokenSet tokens
    }
    class MoveType {
        <<enumeration>>
        TAKE_TOKENS
        BUY_CARD
        RESERVE_CARD
        RETURN_TOKENS
    }
    class CardSource {
        <<enumeration>>
        MARKET
        RESERVED
        DECK
    }
```
 
---
 
## 4. Обработка хода
 
```mermaid
classDiagram
    MoveProcessor --> MoveValidator
    MoveProcessor --> MoveApplier
    MoveProcessor --> NobleEvaluator
    MoveProcessor --> ScoreCalculator
    MoveValidator <|.. TakeTokensValidator
    MoveValidator <|.. BuyCardValidator
    MoveValidator <|.. ReserveCardValidator
    MoveValidator <|.. ReturnTokensValidator
    MoveValidator ..> ValidationResult
    MoveValidator --> GameRules
    MoveApplier <|.. TakeTokensApplier
    MoveApplier <|.. BuyCardApplier
    MoveApplier <|.. ReserveCardApplier
    MoveApplier <|.. ReturnTokensApplier
 
    class MoveProcessor {
        +process(move, session) MoveResult
    }
    class MoveValidator {
        <<interface>>
        +validate(move, state) ValidationResult
    }
    class MoveApplier {
        <<interface>>
        +apply(move, state) GameState
    }
    class ValidationResult {
        +Boolean valid
        +String reason
    }
    class MoveResult {
        +Boolean success
        +String message
        +GameState newState
    }
    class NobleEvaluator {
        +evaluate(playerState, nobles) List~Noble~
    }
    class ScoreCalculator {
        +calculatePlayerScore(state) Int
        +calculateGameResult(session) GameResult
    }
    class GameRules {
        +Int maxTokensPerPlayer
        +Int pointsToEndGame
        +isGameOver(state) Boolean
    }
```
 
---
 
## 5. Сервисы, репозитории и результаты
 
```mermaid
classDiagram
    GameService --> GameRepository
    GameService --> GameResultRepository
    GameService --> PlayerRepository
    GameService --> MoveProcessor
    GameService --> GameInitializer
    StatisticsService --> PlayerStatisticsRepository
    StatisticsService --> GameResultRepository
    RatingService --> PlayerStatisticsRepository
    GameResult *-- PlayerFinalResult
    PlayerFinalResult --> Player
    PlayerStatistics --> Player
    Rating --> Player
 
    class GameService {
        +createSession(players) GameSession
        +makeMove(sessionId, move) MoveResult
        +finishGame(sessionId) GameResult
    }
    class StatisticsService {
        +updateStatistics(result)
        +getStatistics(playerId) PlayerStatistics
    }
    class RatingService {
        +recalculate()
        +getRating() List~Rating~
    }
    class GameRepository {
        <<interface>>
        +save(session)
        +findById(id) GameSession
    }
    class GameResultRepository {
        <<interface>>
        +save(result)
        +findByPlayer(id) List~GameResult~
    }
    class PlayerRepository {
        <<interface>>
        +save(player)
        +findById(id) Player
    }
    class PlayerStatisticsRepository {
        <<interface>>
        +save(stats)
        +findByPlayerId(id) PlayerStatistics
    }
    class GameResult {
        +String gameId
        +Player winner
        +Long finishedAt
    }
    class PlayerFinalResult {
        +Int score
        +Int noblesCount
        +Int place
    }
    class PlayerStatistics {
        +Int gamesPlayed
        +Int wins
        +Double winRate
    }
    class Rating {
        +Double value
        +Int position
    }
    class GameInitializer {
        +createInitialState(players, rules) GameState
    }
```
 
