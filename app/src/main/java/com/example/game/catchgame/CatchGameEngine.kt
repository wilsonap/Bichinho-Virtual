package com.example.game.catchgame

import kotlin.math.abs
import kotlin.random.Random

data class FallingItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: CatchItemType,
    /** Velocidade normalizada (0..1) por segundo. */
    val speedPerSecond: Float
)

enum class CatchItemType(val emoji: String, val points: Int, val isBomb: Boolean) {
    APPLE("🍎", 10, false),
    COOKIE("🍪", 15, false),
    STAR("⭐", 25, false),
    COIN("🪙", 20, false),
    BOMB("💣", -1, true)
}

enum class CatchGameEvent {
    GOOD_CATCH,
    COIN_CATCH,
    BOMB_HIT,
    GAME_OVER
}

/**
 * Engine puro da Captura de Objetos.
 * Calibração a partir do loop legado (~20 ms → 50 ticks/s):
 *   speedPerSecond = speedPorTick * 50
 *   spawnInterval = 35 / 50 = 0.7 s
 */
class CatchGameEngine(
    private val random: Random = Random.Default
) {
    var petNormalizedX: Float = 0.5f
        private set

    var score: Int = 0
        private set
    var coinsEarned: Int = 0
        private set
    var lives: Int = INITIAL_LIVES
        private set
    var combo: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    val fallingItems: MutableList<FallingItem> = mutableListOf()

    private var nextItemId = 0L
    private var spawnAccumulator = 0f

    fun reset() {
        petNormalizedX = 0.5f
        score = 0
        coinsEarned = 0
        lives = INITIAL_LIVES
        combo = 0
        isGameOver = false
        fallingItems.clear()
        nextItemId = 0L
        spawnAccumulator = 0f
    }

    fun setPetNormalizedX(x: Float) {
        if (isGameOver) return
        petNormalizedX = x.coerceIn(PET_MIN_X, PET_MAX_X)
    }

    fun nudgePet(delta: Float) {
        setPetNormalizedX(petNormalizedX + delta)
    }

    /**
     * Avança a simulação.
     * @return eventos para SFX na UI (sem alocar se vazio).
     */
    fun update(dtSeconds: Float): List<CatchGameEvent> {
        if (isGameOver || dtSeconds <= 0f) return emptyList()

        var events: ArrayList<CatchGameEvent>? = null
        fun emit(event: CatchGameEvent) {
            val list = events ?: ArrayList<CatchGameEvent>(2).also { events = it }
            list.add(event)
        }

        spawnAccumulator += dtSeconds
        while (spawnAccumulator >= SPAWN_INTERVAL_SEC) {
            spawnAccumulator -= SPAWN_INTERVAL_SEC
            spawnItem()
        }

        var i = 0
        while (i < fallingItems.size) {
            val item = fallingItems[i]
            val previousY = item.y
            item.y += item.speedPerSecond * dtSeconds

            if (crossedCatchZone(previousY, item.y) &&
                abs(item.x - petNormalizedX) < CATCH_X_TOLERANCE
            ) {
                when {
                    item.type.isBomb -> {
                        lives -= 1
                        combo = 0
                        emit(CatchGameEvent.BOMB_HIT)
                        fallingItems.removeAt(i)
                        if (lives <= 0) {
                            isGameOver = true
                            emit(CatchGameEvent.GAME_OVER)
                            return events ?: emptyList()
                        }
                        continue
                    }
                    item.type == CatchItemType.COIN -> {
                        coinsEarned += 2
                        score += item.type.points
                        combo += 1
                        emit(CatchGameEvent.COIN_CATCH)
                        fallingItems.removeAt(i)
                        continue
                    }
                    else -> {
                        coinsEarned += 1
                        score += item.type.points
                        combo += 1
                        emit(CatchGameEvent.GOOD_CATCH)
                        fallingItems.removeAt(i)
                        continue
                    }
                }
            }

            if (item.y > MISS_Y) {
                if (!item.type.isBomb) {
                    combo = 0
                }
                fallingItems.removeAt(i)
                continue
            }
            i++
        }

        return events ?: emptyList()
    }

    private fun spawnItem() {
        val isBomb = random.nextInt(100) < BOMB_CHANCE_PERCENT
        val type = if (isBomb) {
            CatchItemType.BOMB
        } else {
            GOOD_ITEM_TYPES[random.nextInt(GOOD_ITEM_TYPES.size)]
        }
        val speedTick = random.nextFloat() * SPEED_TICK_RANGE + SPEED_TICK_MIN
        fallingItems.add(
            FallingItem(
                id = nextItemId++,
                x = random.nextFloat().coerceIn(0.1f, 0.9f),
                y = SPAWN_Y,
                type = type,
                speedPerSecond = speedTick * LEGACY_TICKS_PER_SECOND
            )
        )
    }

    /**
     * Detecta passagem pela faixa [CATCH_ZONE_TOP, CATCH_ZONE_BOTTOM]
     * mesmo quando o passo de dt “pula” a zona.
     */
    private fun crossedCatchZone(previousY: Float, currentY: Float): Boolean {
        if (currentY in CATCH_ZONE_TOP..CATCH_ZONE_BOTTOM) return true
        if (previousY < CATCH_ZONE_TOP && currentY > CATCH_ZONE_BOTTOM) return true
        if (previousY < CATCH_ZONE_TOP && currentY >= CATCH_ZONE_TOP) return true
        return false
    }

    companion object {
        const val LEGACY_TICKS_PER_SECOND = 50f
        const val INITIAL_LIVES = 3
        const val BOMB_CHANCE_PERCENT = 25
        const val SPAWN_INTERVAL_SEC = 35f / LEGACY_TICKS_PER_SECOND // 0.7s
        const val SPEED_TICK_MIN = 0.008f
        const val SPEED_TICK_RANGE = 0.004f
        const val SPAWN_Y = -0.05f
        const val CATCH_ZONE_TOP = 0.78f
        const val CATCH_ZONE_BOTTOM = 0.88f
        const val CATCH_X_TOLERANCE = 0.12f
        const val MISS_Y = 1.05f
        const val PET_MIN_X = 0.1f
        const val PET_MAX_X = 0.9f
        const val PET_NUDGE = 0.12f
        const val MAX_DT_SECONDS = 0.05f

        val GOOD_ITEM_TYPES: Array<CatchItemType> = arrayOf(
            CatchItemType.APPLE,
            CatchItemType.COOKIE,
            CatchItemType.STAR,
            CatchItemType.COIN
        )
    }
}
