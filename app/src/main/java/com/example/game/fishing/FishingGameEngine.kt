package com.example.game.fishing

import kotlin.math.abs
import kotlin.random.Random

enum class FishingCatchType(
    val emoji: String,
    val points: Int,
    val isJunk: Boolean,
    /** Peso relativo no spawn (raros = baixo). */
    val spawnWeight: Int
) {
    SMALL_FISH("🐟", 10, false, 32),
    COLORFUL_FISH("🐠", 20, false, 28),
    PUFFER("🐡", 30, false, 14),
    SHRIMP("🦐", 40, false, 8),
    CRAB("🦀", 50, false, 6),
    STARFISH("⭐", 75, false, 3),
    OLD_BOOT("👢", 0, true, 0),
    CAN("🥫", 0, true, 0),
    TRASH("🗑️", 0, true, 0);

    val isRare: Boolean get() = !isJunk && points >= 40
}

enum class HookPhase {
    IDLE,
    DESCENDING,
    ASCENDING
}

enum class FishingGameEvent {
    CAST,
    CATCH_FISH,
    CATCH_RARE,
    CATCH_JUNK,
    GAME_OVER
}

data class SwimEntity(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    val type: FishingCatchType
)

/**
 * Engine puro da Pescaria — física em coordenadas normalizadas (0..1).
 * Sem Compose / alocações no hot path além de eventos ocasionais.
 */
class FishingGameEngine(
    private val random: Random = Random.Default
) {
    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var timeLeftSec: Float = MATCH_DURATION_SEC
        private set
    var isGameOver: Boolean = false
        private set
    var hookY: Float = SURFACE_Y
        private set
    var hookPhase: HookPhase = HookPhase.IDLE
        private set
    var attached: SwimEntity? = null
        private set
    var reactionText: String? = null
        private set
    var reactionTimer: Float = 0f
        private set

    val entities: MutableList<SwimEntity> = mutableListOf()

    private var nextId = 0L
    private var spawnAccum = 0f
    private var rewardsClaimed = false
    private var elapsedSec = 0f

    val coinsEarned: Int
        get() = computeCoinsEarned(score)

    fun reset() {
        score = 0
        combo = 0
        timeLeftSec = MATCH_DURATION_SEC
        isGameOver = false
        hookY = SURFACE_Y
        hookPhase = HookPhase.IDLE
        attached = null
        reactionText = null
        reactionTimer = 0f
        entities.clear()
        nextId = 0L
        spawnAccum = 0f
        rewardsClaimed = false
        elapsedSec = 0f
    }

    /** Toque em Lançar / Puxar. */
    fun onCastTap(): FishingGameEvent? {
        if (isGameOver) return null
        return when (hookPhase) {
            HookPhase.IDLE -> {
                hookPhase = HookPhase.DESCENDING
                attached = null
                FishingGameEvent.CAST
            }
            HookPhase.DESCENDING -> {
                hookPhase = HookPhase.ASCENDING
                null
            }
            HookPhase.ASCENDING -> null
        }
    }

    /**
     * Avança a simulação.
     * @return eventos de SFX (lista vazia reutilizável quando possível).
     */
    fun update(dtSeconds: Float): List<FishingGameEvent> {
        if (isGameOver || dtSeconds <= 0f) return emptyList()

        var events: ArrayList<FishingGameEvent>? = null
        fun emit(e: FishingGameEvent) {
            val list = events ?: ArrayList<FishingGameEvent>(2).also { events = it }
            list.add(e)
        }

        elapsedSec += dtSeconds
        timeLeftSec = (MATCH_DURATION_SEC - elapsedSec).coerceAtLeast(0f)
        if (elapsedSec >= MATCH_DURATION_SEC) {
            timeLeftSec = 0f
            finishMatch { emit(it) }
            return events ?: emptyList()
        }

        if (reactionTimer > 0f) {
            reactionTimer -= dtSeconds
            if (reactionTimer <= 0f) {
                reactionTimer = 0f
                reactionText = null
            }
        }

        val progress = (elapsedSec / MATCH_DURATION_SEC).coerceIn(0f, 1f)
        updateSwimmers(dtSeconds, progress)
        spawnAccum += dtSeconds
        val spawnEvery = spawnInterval(progress)
        while (spawnAccum >= spawnEvery) {
            spawnAccum -= spawnEvery
            spawnEntity(progress)
        }

        updateHook(dtSeconds) { emit(it) }

        return events ?: emptyList()
    }

    /**
     * Retorna score/moedas uma única vez no fim (evita recompensa duplicada).
     */
    fun consumeEndRewards(): Pair<Int, Int>? {
        if (!isGameOver || rewardsClaimed) return null
        rewardsClaimed = true
        return score to coinsEarned
    }

    fun hasClaimedRewards(): Boolean = rewardsClaimed

    private fun finishMatch(emit: (FishingGameEvent) -> Unit) {
        if (isGameOver) return
        isGameOver = true
        hookPhase = HookPhase.IDLE
        emit(FishingGameEvent.GAME_OVER)
    }

    private fun updateHook(dt: Float, emit: (FishingGameEvent) -> Unit) {
        when (hookPhase) {
            HookPhase.IDLE -> {
                hookY = SURFACE_Y
            }
            HookPhase.DESCENDING -> {
                hookY = (hookY + HOOK_SPEED * dt).coerceAtMost(MAX_HOOK_Y)
                if (hookY >= MAX_HOOK_Y) {
                    hookPhase = HookPhase.ASCENDING
                }
            }
            HookPhase.ASCENDING -> {
                // Captura só na subida e se ainda não há peixe preso
                if (attached == null) {
                    tryCatch(emit)
                } else {
                    attached!!.x = HOOK_X
                    attached!!.y = hookY
                }
                hookY = (hookY - HOOK_SPEED * dt).coerceAtLeast(SURFACE_Y)
                if (hookY <= SURFACE_Y) {
                    onReachedSurface(emit)
                }
            }
        }
    }

    private fun tryCatch(emit: (FishingGameEvent) -> Unit) {
        var i = 0
        while (i < entities.size) {
            val e = entities[i]
            if (abs(e.x - HOOK_X) <= CATCH_X_TOL && abs(e.y - hookY) <= CATCH_Y_TOL) {
                entities.removeAt(i)
                attached = e
                e.x = HOOK_X
                e.y = hookY
                if (e.type.isJunk) {
                    emit(FishingGameEvent.CATCH_JUNK)
                } else if (e.type.isRare) {
                    emit(FishingGameEvent.CATCH_RARE)
                } else {
                    emit(FishingGameEvent.CATCH_FISH)
                }
                return
            }
            i++
        }
    }

    private fun onReachedSurface(emit: (FishingGameEvent) -> Unit) {
        val catch = attached
        attached = null
        hookPhase = HookPhase.IDLE
        hookY = SURFACE_Y
        if (catch == null) return

        if (catch.type.isJunk) {
            combo = 0
            reactionText = "Eca! Isso não é peixe! 😖"
            reactionTimer = REACTION_DURATION_SEC
            return
        }

        combo = (combo + 1).coerceAtMost(MAX_COMBO)
        score += catch.type.points * combo
    }

    private fun updateSwimmers(dt: Float, progress: Float) {
        val speedMul = fishSpeedMul(progress)
        var i = 0
        while (i < entities.size) {
            val e = entities[i]
            if (attached?.id == e.id) {
                i++
                continue
            }
            e.x += e.vx * speedMul * dt
            if (e.x < -0.05f || e.x > 1.05f) {
                entities.removeAt(i)
                continue
            }
            i++
        }
    }

    private fun spawnEntity(progress: Float) {
        if (entities.size >= MAX_ENTITIES) return
        val type = pickType(progress)
        val depth = DEPTH_LANES[random.nextInt(DEPTH_LANES.size)]
        val fromLeft = random.nextBoolean()
        val speed = (0.10f + random.nextFloat() * 0.12f) * (if (fromLeft) 1f else -1f)
        entities.add(
            SwimEntity(
                id = nextId++,
                x = if (fromLeft) -0.04f else 1.04f,
                y = depth,
                vx = speed,
                type = type
            )
        )
    }

    private fun pickType(progress: Float): FishingCatchType {
        val junkChance = junkChance(progress)
        if (random.nextFloat() < junkChance) {
            return JUNK_TYPES[random.nextInt(JUNK_TYPES.size)]
        }
        var total = 0
        for (t in FISH_TYPES) total += t.spawnWeight
        var roll = random.nextInt(total)
        for (t in FISH_TYPES) {
            roll -= t.spawnWeight
            if (roll < 0) return t
        }
        return FishingCatchType.SMALL_FISH
    }

    companion object {
        const val MATCH_DURATION_SEC = 60f
        const val MAX_COMBO = 5
        const val MAX_DT_SECONDS = 0.05f
        const val SURFACE_Y = 0.30f
        const val MAX_HOOK_Y = 0.92f
        const val HOOK_X = 0.50f
        const val HOOK_SPEED = 0.55f
        const val CATCH_X_TOL = 0.08f
        const val CATCH_Y_TOL = 0.055f
        const val MAX_ENTITIES = 14
        const val REACTION_DURATION_SEC = 1.6f

        val DEPTH_LANES: FloatArray = floatArrayOf(0.42f, 0.55f, 0.68f, 0.80f)

        val FISH_TYPES: Array<FishingCatchType> = arrayOf(
            FishingCatchType.SMALL_FISH,
            FishingCatchType.COLORFUL_FISH,
            FishingCatchType.PUFFER,
            FishingCatchType.SHRIMP,
            FishingCatchType.CRAB,
            FishingCatchType.STARFISH
        )

        val JUNK_TYPES: Array<FishingCatchType> = arrayOf(
            FishingCatchType.OLD_BOOT,
            FishingCatchType.CAN,
            FishingCatchType.TRASH
        )

        fun spawnInterval(progress: Float): Float =
            lerp(1.65f, 0.85f, progress)

        fun fishSpeedMul(progress: Float): Float =
            lerp(0.75f, 1.55f, progress)

        fun junkChance(progress: Float): Float =
            lerp(0.06f, 0.26f, progress)

        fun computeCoinsEarned(score: Int): Int {
            if (score <= 0) return 5
            return (score / 25).coerceIn(8, 55)
        }

        private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    }
}
