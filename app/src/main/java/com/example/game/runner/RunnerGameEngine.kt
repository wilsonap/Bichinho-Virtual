package com.example.game.runner

import kotlin.random.Random

data class RunnerObstacle(
    var x: Float,
    val width: Float = 36f,
    val height: Float = 48f
)

data class RunnerCoin(
    var x: Float,
    val y: Float,
    val radius: Float = 16f,
    var isCollected: Boolean = false
)

/** Eventos pontuais para a UI tocar SFX (sem acoplar áudio ao engine). */
enum class RunnerGameEvent {
    COIN_COLLECTED,
    COLLISION
}

/**
 * Engine puro da Corrida do Bichinho.
 * Unidades convertidas do loop legado (~20 ms / 50 ticks/s):
 *   valorPorSegundo = valorPorTick * 50
 *   gravidade = gTick * 50 * 50
 */
class RunnerGameEngine(
    private val random: Random = Random.Default
) {
    var petY: Float = 0f
        private set
    var petVelocityY: Float = 0f
        private set
    var isGrounded: Boolean = true
        private set

    var distance: Float = 0f
        private set
    var coinsCollected: Int = 0
        private set
    var speed: Float = INITIAL_SPEED
        private set
    var isGameOver: Boolean = false
        private set

    val obstacles: MutableList<RunnerObstacle> = mutableListOf()
    val collectibleCoins: MutableList<RunnerCoin> = mutableListOf()

    private var spawnAccumulator = 0f
    private var nextSpeedAtDistance = SPEED_DISTANCE_STEP

    fun reset() {
        petY = 0f
        petVelocityY = 0f
        isGrounded = true
        distance = 0f
        coinsCollected = 0
        speed = INITIAL_SPEED
        isGameOver = false
        obstacles.clear()
        collectibleCoins.clear()
        spawnAccumulator = 0f
        nextSpeedAtDistance = SPEED_DISTANCE_STEP
    }

    /** @return true se o pulo foi aceito */
    fun jump(): Boolean {
        if (!isGrounded || isGameOver) return false
        petVelocityY = JUMP_VELOCITY
        isGrounded = false
        return true
    }

    fun distanceScoreInt(): Int = distance.toInt()

    /** Mesma fórmula de recompensa do loop antigo. */
    fun totalCoinsEarned(): Int = coinsCollected + (distanceScoreInt() / 40)

    /**
     * Avança a simulação com [dtSeconds] (clamped pelo caller).
     * @return eventos ocorridos neste frame (para SFX).
     */
    fun update(dtSeconds: Float): List<RunnerGameEvent> {
        if (isGameOver || dtSeconds <= 0f) return emptyList()

        var events: ArrayList<RunnerGameEvent>? = null
        fun emit(event: RunnerGameEvent) {
            val list = events ?: ArrayList<RunnerGameEvent>(2).also { events = it }
            list.add(event)
        }

        distance += DISTANCE_PER_SECOND * dtSeconds
        while (distance >= nextSpeedAtDistance) {
            speed += SPEED_INCREMENT
            nextSpeedAtDistance += SPEED_DISTANCE_STEP
        }

        if (!isGrounded) {
            petY += petVelocityY * dtSeconds
            petVelocityY += GRAVITY * dtSeconds
            if (petY >= 0f) {
                petY = 0f
                petVelocityY = 0f
                isGrounded = true
            }
        }

        spawnAccumulator += dtSeconds
        while (spawnAccumulator >= SPAWN_INTERVAL_SEC) {
            spawnAccumulator -= SPAWN_INTERVAL_SEC
            spawnObstacleAndMaybeCoin()
        }

        val move = speed * dtSeconds
        var i = 0
        while (i < obstacles.size) {
            val obs = obstacles[i]
            obs.x -= move
            if (obs.x < -100f) {
                obstacles.removeAt(i)
                continue
            }
            if (collidesWithPet(obs)) {
                isGameOver = true
                emit(RunnerGameEvent.COLLISION)
                return events ?: emptyList()
            }
            i++
        }

        i = 0
        while (i < collectibleCoins.size) {
            val coin = collectibleCoins[i]
            coin.x -= move
            if (coin.x < -100f) {
                collectibleCoins.removeAt(i)
                continue
            }
            if (!coin.isCollected && collidesWithCoin(coin)) {
                coin.isCollected = true
                coinsCollected++
                emit(RunnerGameEvent.COIN_COLLECTED)
            }
            i++
        }

        return events ?: emptyList()
    }

    private fun spawnObstacleAndMaybeCoin() {
        obstacles.add(
            RunnerObstacle(
                x = LOGICAL_WIDTH,
                width = 36f,
                height = random.nextInt(35, 55).toFloat()
            )
        )
        if (random.nextBoolean()) {
            collectibleCoins.add(
                RunnerCoin(
                    x = LOGICAL_WIDTH + random.nextInt(60, 160),
                    y = -random.nextInt(50, 130).toFloat()
                )
            )
        }
    }

    private fun collidesWithPet(obs: RunnerObstacle): Boolean {
        val petLeft = PET_LEFT
        val petRight = PET_RIGHT
        val petBottom = 0f + petY
        val obsLeft = obs.x
        val obsRight = obs.x + obs.width
        val obsTop = -obs.height
        return petRight > obsLeft && petLeft < obsRight && petBottom > obsTop
    }

    private fun collidesWithCoin(coin: RunnerCoin): Boolean {
        val petCenterX = PET_CENTER_X
        val petCenterY = -30f + petY
        val dx = petCenterX - coin.x
        val dy = petCenterY - coin.y
        val hit = 42f * 42f
        return dx * dx + dy * dy < hit
    }

    companion object {
        /** Equivalente ao delay(20) legado → 50 ticks/s. */
        const val LEGACY_TICKS_PER_SECOND = 50f

        val INITIAL_SPEED = 7.5f * LEGACY_TICKS_PER_SECOND
        val SPEED_INCREMENT = 0.4f * LEGACY_TICKS_PER_SECOND
        val DISTANCE_PER_SECOND = 1f * LEGACY_TICKS_PER_SECOND
        const val SPEED_DISTANCE_STEP = 150f

        /** g_tick * ticks² → unidades/s² */
        val GRAVITY = 1.1f * LEGACY_TICKS_PER_SECOND * LEGACY_TICKS_PER_SECOND
        val JUMP_VELOCITY = -18f * LEGACY_TICKS_PER_SECOND

        val SPAWN_INTERVAL_SEC = 80f / LEGACY_TICKS_PER_SECOND

        const val LOGICAL_WIDTH = 1000f
        const val PET_LEFT = 110f
        const val PET_RIGHT = 170f
        const val PET_CENTER_X = 140f

        /** Evita salto de física após hitch. */
        const val MAX_DT_SECONDS = 0.05f
    }
}
