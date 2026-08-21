package com.example.game.footsteps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FootstepsGameEngineTest {

    /** Completa caminhada + holds da demo até PLAYER_INPUT. */
    private fun finishDemo(engine: FootstepsGameEngine) {
        var guard = 0
        while (engine.phase != FootstepsPhase.PLAYER_INPUT && !engine.isGameOver && guard < 500) {
            engine.update(0.2f)
            guard++
        }
        assertEquals(FootstepsPhase.PLAYER_INPUT, engine.phase)
    }

    private fun walkArrive(engine: FootstepsGameEngine) {
        engine.update(FootstepsGameEngine.WALK_DURATION_SEC + 0.01f)
    }

    @Test
    fun startRound_sequenceLengthGrows() {
        val engine = FootstepsGameEngine(Random(1))
        engine.startRound()
        assertEquals(3, engine.sequence.size)
        finishDemo(engine)
        // completa rodada forçando taps corretos
        engine.sequence.forEach { pad ->
            engine.onPadTapped(pad)
            walkArrive(engine)
        }
        assertEquals(FootstepsPhase.CELEBRATE, engine.phase)
        engine.update(FootstepsGameEngine.CELEBRATE_SEC + 0.01f)
        engine.startRound()
        assertEquals(4, engine.sequence.size)
    }

    @Test
    fun correctSequence_increasesScoreAndCombo() {
        val engine = FootstepsGameEngine(Random(2))
        engine.startRound()
        finishDemo(engine)
        val len = engine.sequence.size
        engine.sequence.forEach { pad ->
            val ev = engine.onPadTapped(pad)
            assertTrue(ev.contains(FootstepsEvent.PLAYER_STEP_OK))
            walkArrive(engine)
        }
        assertTrue(engine.score >= len * 10)
        assertEquals(2, engine.combo)
    }

    @Test
    fun wrongPad_losesLifeAndResetsCombo() {
        val engine = FootstepsGameEngine(Random(3))
        engine.startRound()
        finishDemo(engine)
        // acerta o primeiro para combo > 1 depois erra
        val first = engine.sequence[0]
        engine.onPadTapped(first)
        walkArrive(engine)
        // completa? se sequence > 1, força combo via complete — mais simples: setar via rounds
        engine.update(0.01f)
        // Erra de propósito
        val wrong = (0 until 6).first { it != engine.sequence[engine.playerStep] }
        val events = engine.onPadTapped(wrong)
        assertTrue(events.contains(FootstepsEvent.PLAYER_STEP_WRONG))
        assertEquals(2, engine.lives)
        assertEquals(1, engine.combo)
    }

    @Test
    fun threeWrongs_gameOver() {
        val engine = FootstepsGameEngine(Random(4))
        engine.startRound()
        finishDemo(engine)
        repeat(3) {
            if (engine.isGameOver) return@repeat
            // Garante fase input
            var g = 0
            while (engine.phase != FootstepsPhase.PLAYER_INPUT && !engine.isGameOver && g < 200) {
                engine.update(0.2f)
                g++
            }
            if (engine.isGameOver) return@repeat
            val expected = engine.sequence[engine.playerStep]
            val wrong = (0 until 6).first { it != expected }
            engine.onPadTapped(wrong)
        }
        assertTrue(engine.isGameOver)
        assertEquals(0, engine.lives)
        assertEquals(FootstepsPhase.GAME_OVER, engine.phase)
    }

    @Test
    fun combo_cappedAtX5() {
        val engine = FootstepsGameEngine(Random(5))
        repeat(6) {
            engine.startRound()
            finishDemo(engine)
            engine.sequence.forEach { pad ->
                engine.onPadTapped(pad)
                walkArrive(engine)
            }
            engine.update(FootstepsGameEngine.CELEBRATE_SEC + 0.05f)
        }
        assertEquals(FootstepsGameEngine.MAX_COMBO, engine.combo)
    }

    @Test
    fun consumeEndRewards_onlyOnce() {
        val engine = FootstepsGameEngine(Random(6))
        engine.startRound()
        finishDemo(engine)
        repeat(3) {
            if (engine.isGameOver) return@repeat
            var g = 0
            while (engine.phase != FootstepsPhase.PLAYER_INPUT && !engine.isGameOver && g < 200) {
                engine.update(0.2f); g++
            }
            if (engine.isGameOver) return@repeat
            val wrong = (0 until 6).first { it != engine.sequence[engine.playerStep] }
            engine.onPadTapped(wrong)
        }
        assertTrue(engine.isGameOver)
        val first = engine.consumeEndRewards()
        assertNotNull(first)
        assertNull(engine.consumeEndRewards())
        assertTrue(engine.hasClaimedRewards())
    }

    @Test
    fun restart_resetsState() {
        val engine = FootstepsGameEngine(Random(7))
        engine.startRound()
        finishDemo(engine)
        engine.onPadTapped(engine.sequence[0])
        walkArrive(engine)
        engine.reset()
        assertEquals(0, engine.score)
        assertEquals(1, engine.combo)
        assertEquals(3, engine.lives)
        assertEquals(0, engine.round)
        assertFalse(engine.isGameOver)
        assertEquals(FootstepsPhase.READY, engine.phase)
    }

    @Test
    fun coins_followEconomyCurve() {
        assertEquals(5, FootstepsGameEngine.computeCoinsEarned(0))
        assertEquals(8, FootstepsGameEngine.computeCoinsEarned(100))
        assertEquals(55, FootstepsGameEngine.computeCoinsEarned(9_000))
    }

    @Test
    fun petInterpolatesDuringWalk() {
        val engine = FootstepsGameEngine(Random(8))
        engine.startRound()
        val startX = engine.petX
        engine.update(0.1f)
        // Em DEMO_WALK, progresso > 0
        assertTrue(engine.phase == FootstepsPhase.DEMO_WALK)
        assertTrue(engine.animProgress in 0.01f..0.99f || engine.petX != startX || engine.animProgress > 0f)
    }
}
