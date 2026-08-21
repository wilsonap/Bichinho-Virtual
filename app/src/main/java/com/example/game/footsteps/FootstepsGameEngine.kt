package com.example.game.footsteps

import kotlin.random.Random

enum class FootstepsPhase {
    /** Aguardando início / entre rodadas. */
    READY,
    /** Pet andando na demonstração. */
    DEMO_WALK,
    /** Breve pausa com pegada destacada. */
    DEMO_HOLD,
    /** Jogador deve tocar. */
    PLAYER_INPUT,
    /** Pet andando até o pad escolhido. */
    PLAYER_WALK,
    /** Acertou a sequência — celebração curta. */
    CELEBRATE,
    GAME_OVER
}

enum class FootstepsEvent {
    DEMO_STEP,
    DEMO_DONE,
    PLAYER_STEP_OK,
    PLAYER_STEP_WRONG,
    ROUND_COMPLETE,
    GAME_OVER,
    WALK_START,
    WALK_ARRIVED
}

/**
 * Engine puro — Siga as Pegadas.
 * 6 pads (0..5). Sequência cresce por rodada: 3, 4, 5, 6...
 * Moedas só no fim via [computeCoinsEarned].
 */
class FootstepsGameEngine(
    private val random: Random = Random.Default
) {
    var phase: FootstepsPhase = FootstepsPhase.READY
        private set
    var score: Int = 0
        private set
    var combo: Int = 1
        private set
    var lives: Int = INITIAL_LIVES
        private set
    var round: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    /** Sequência da rodada atual (índices 0..5). */
    var sequence: List<Int> = emptyList()
        private set

    /** Próximo índice esperado na sequência (modo jogador). */
    var playerStep: Int = 0
        private set

    /** Passo atual na demo (índice em [sequence]). */
    var demoStep: Int = 0
        private set

    /** Pad onde o pet está parado (ou destino ao fim da caminhada). */
    var petPad: Int = HOME_PAD
        private set

    var animFromX: Float = HOME_X
        private set
    var animFromY: Float = HOME_Y
        private set
    var animToX: Float = HOME_X
        private set
    var animToY: Float = HOME_Y
        private set
    var animProgress: Float = 1f
        private set

    /** Pad destacado (demo/hold/erro). -1 = nenhum. */
    var highlightPad: Int = -1
        private set

    /** Pegadas visíveis (demo ou acertos do jogador). */
    val visibleFootprints: MutableSet<Int> = linkedSetOf()

    var errorPad: Int = -1
        private set
    var errorTimer: Float = 0f
        private set
    var celebrateTimer: Float = 0f
        private set
    var holdTimer: Float = 0f
        private set

    private var rewardsClaimed = false

    val petX: Float
        get() = lerp(animFromX, animToX, smooth(animProgress))
    val petY: Float
        get() = lerp(animFromY, animToY, smooth(animProgress))

    val coinsEarned: Int
        get() = computeCoinsEarned(score)

    val needsAnimationLoop: Boolean
        get() = phase == FootstepsPhase.DEMO_WALK ||
            phase == FootstepsPhase.DEMO_HOLD ||
            phase == FootstepsPhase.PLAYER_WALK ||
            phase == FootstepsPhase.CELEBRATE ||
            errorTimer > 0f

    fun reset() {
        phase = FootstepsPhase.READY
        score = 0
        combo = 1
        lives = INITIAL_LIVES
        round = 0
        isGameOver = false
        sequence = emptyList()
        playerStep = 0
        demoStep = 0
        petPad = HOME_PAD
        setPose(HOME_X, HOME_Y)
        highlightPad = -1
        visibleFootprints.clear()
        errorPad = -1
        errorTimer = 0f
        celebrateTimer = 0f
        holdTimer = 0f
        rewardsClaimed = false
    }

    /** Inicia a próxima rodada (ou a primeira). */
    fun startRound(): List<FootstepsEvent> {
        if (isGameOver) return emptyList()
        round += 1
        val length = (BASE_SEQUENCE_LEN + round - 1).coerceAtMost(MAX_SEQUENCE_LEN)
        sequence = buildSequence(length)
        playerStep = 0
        demoStep = 0
        visibleFootprints.clear()
        highlightPad = -1
        errorPad = -1
        // Volta ao centro antes da demo
        petPad = HOME_PAD
        setPose(HOME_X, HOME_Y)
        return beginDemoWalkTo(sequence[0])
    }

    fun onPadTapped(pad: Int): List<FootstepsEvent> {
        if (isGameOver || phase != FootstepsPhase.PLAYER_INPUT) return emptyList()
        if (pad !in 0 until PAD_COUNT) return emptyList()

        val expected = sequence[playerStep]
        if (pad != expected) {
            return handleWrong(pad)
        }

        val events = ArrayList<FootstepsEvent>(3)
        events.add(FootstepsEvent.PLAYER_STEP_OK)
        events.addAll(beginPlayerWalkTo(pad))
        return events
    }

    /**
     * Avança animações/timers. Só precisa ser chamado enquanto [needsAnimationLoop].
     */
    fun update(dtSeconds: Float): List<FootstepsEvent> {
        if (dtSeconds <= 0f || isGameOver) return emptyList()

        if (errorTimer > 0f) {
            errorTimer = (errorTimer - dtSeconds).coerceAtLeast(0f)
            if (errorTimer <= 0f) errorPad = -1
        }

        when (phase) {
            FootstepsPhase.DEMO_WALK, FootstepsPhase.PLAYER_WALK -> {
                animProgress = (animProgress + dtSeconds / WALK_DURATION_SEC).coerceAtMost(1f)
                if (animProgress >= 1f) {
                    val list = ArrayList<FootstepsEvent>(4)
                    list.add(FootstepsEvent.WALK_ARRIVED)
                    list.addAll(onArrived())
                    return list
                }
            }
            FootstepsPhase.DEMO_HOLD -> {
                holdTimer -= dtSeconds
                if (holdTimer <= 0f) {
                    return advanceDemoAfterHold()
                }
            }
            FootstepsPhase.CELEBRATE -> {
                celebrateTimer -= dtSeconds
                if (celebrateTimer <= 0f) {
                    phase = FootstepsPhase.READY
                }
            }
            else -> Unit
        }

        return emptyList()
    }

    fun consumeEndRewards(): Pair<Int, Int>? {
        if (!isGameOver || rewardsClaimed) return null
        rewardsClaimed = true
        return score to coinsEarned
    }

    fun hasClaimedRewards(): Boolean = rewardsClaimed

    // --- internos ---

    private fun beginDemoWalkTo(pad: Int): List<FootstepsEvent> {
        phase = FootstepsPhase.DEMO_WALK
        startWalk(pad)
        return listOf(FootstepsEvent.WALK_START)
    }

    private fun beginPlayerWalkTo(pad: Int): List<FootstepsEvent> {
        phase = FootstepsPhase.PLAYER_WALK
        startWalk(pad)
        return listOf(FootstepsEvent.WALK_START)
    }

    private fun startWalk(pad: Int) {
        animFromX = petX
        animFromY = petY
        val (tx, ty) = padPos(pad)
        animToX = tx
        animToY = ty
        animProgress = 0f
        petPad = pad
        highlightPad = pad
    }

    private fun onArrived(): List<FootstepsEvent> {
        animProgress = 1f
        setPose(animToX, animToY)
        return when (phase) {
            FootstepsPhase.DEMO_WALK -> {
                visibleFootprints.add(petPad)
                highlightPad = petPad
                phase = FootstepsPhase.DEMO_HOLD
                holdTimer = DEMO_HOLD_SEC
                listOf(FootstepsEvent.DEMO_STEP)
            }
            FootstepsPhase.PLAYER_WALK -> {
                visibleFootprints.add(petPad)
                highlightPad = petPad
                playerStep += 1
                if (playerStep >= sequence.size) {
                    completeRound()
                } else {
                    phase = FootstepsPhase.PLAYER_INPUT
                    highlightPad = -1
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun advanceDemoAfterHold(): List<FootstepsEvent> {
        demoStep += 1
        if (demoStep >= sequence.size) {
            // Limpa pegadas e passa a vez
            visibleFootprints.clear()
            highlightPad = -1
            phase = FootstepsPhase.PLAYER_INPUT
            playerStep = 0
            // Pet volta ao início da sequência (primeiro pad) — fica onde está
            return listOf(FootstepsEvent.DEMO_DONE)
        }
        return beginDemoWalkTo(sequence[demoStep])
    }

    private fun completeRound(): List<FootstepsEvent> {
        val gained = sequence.size * POINTS_PER_STEP * combo
        score += gained
        combo = (combo + 1).coerceAtMost(MAX_COMBO)
        phase = FootstepsPhase.CELEBRATE
        celebrateTimer = CELEBRATE_SEC
        highlightPad = -1
        return listOf(FootstepsEvent.ROUND_COMPLETE)
    }

    private fun handleWrong(pad: Int): List<FootstepsEvent> {
        lives -= 1
        combo = 1
        errorPad = pad
        errorTimer = ERROR_FLASH_SEC
        highlightPad = -1
        visibleFootprints.clear()
        playerStep = 0
        val events = ArrayList<FootstepsEvent>(4)
        events.add(FootstepsEvent.PLAYER_STEP_WRONG)
        if (lives <= 0) {
            isGameOver = true
            phase = FootstepsPhase.GAME_OVER
            events.add(FootstepsEvent.GAME_OVER)
        } else {
            // Repete a demo da mesma sequência
            events.addAll(restartCurrentRoundDemo())
        }
        return events
    }

    /** Após erro com vidas restantes: repete a demo da sequência atual. */
    fun restartCurrentRoundDemo(): List<FootstepsEvent> {
        if (isGameOver || sequence.isEmpty()) return emptyList()
        playerStep = 0
        demoStep = 0
        visibleFootprints.clear()
        highlightPad = -1
        errorPad = -1
        petPad = HOME_PAD
        setPose(HOME_X, HOME_Y)
        return beginDemoWalkTo(sequence[0])
    }

    private fun buildSequence(length: Int): List<Int> {
        val result = ArrayList<Int>(length)
        var last = -1
        repeat(length) {
            var next: Int
            do {
                next = random.nextInt(PAD_COUNT)
            } while (next == last && PAD_COUNT > 1)
            result.add(next)
            last = next
        }
        return result
    }

    private fun setPose(x: Float, y: Float) {
        animFromX = x
        animFromY = y
        animToX = x
        animToY = y
        animProgress = 1f
    }

    companion object {
        const val PAD_COUNT = 6
        const val HOME_PAD = -1
        const val INITIAL_LIVES = 3
        const val MAX_COMBO = 5
        const val BASE_SEQUENCE_LEN = 3
        const val MAX_SEQUENCE_LEN = 8
        const val POINTS_PER_STEP = 10
        const val WALK_DURATION_SEC = 0.45f
        const val DEMO_HOLD_SEC = 0.35f
        const val CELEBRATE_SEC = 0.9f
        const val ERROR_FLASH_SEC = 0.55f
        const val MAX_DT_SECONDS = 0.05f

        const val HOME_X = 0.50f
        const val HOME_Y = 0.52f

        /** Posições normalizadas dos pads 0..5 (layout 2-2-2). */
        val PAD_X: FloatArray = floatArrayOf(0.35f, 0.65f, 0.22f, 0.78f, 0.35f, 0.65f)
        val PAD_Y: FloatArray = floatArrayOf(0.30f, 0.30f, 0.50f, 0.50f, 0.72f, 0.72f)

        fun padPos(index: Int): Pair<Float, Float> = PAD_X[index] to PAD_Y[index]

        fun computeCoinsEarned(score: Int): Int {
            if (score <= 0) return 5
            return (score / 25).coerceIn(8, 55)
        }

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        private fun smooth(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            return x * x * (3f - 2f * x)
        }
    }
}
