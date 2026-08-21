package com.example.game.fishing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.random.Random

class FishingGameEngineTest {

    private fun riseToSurface(engine: FishingGameEngine) {
        var guard = 0
        while (engine.hookPhase != HookPhase.IDLE && !engine.isGameOver && guard < 200) {
            engine.update(0.05f)
            guard++
        }
    }

    private fun forceCatch(
        engine: FishingGameEngine,
        type: FishingCatchType
    ) {
        engine.entities.clear()
        if (engine.hookPhase == HookPhase.IDLE) {
            engine.onCastTap()
        }
        // Desce um pouco
        repeat(15) { engine.update(0.05f) }
        engine.entities.clear()
        if (engine.hookPhase == HookPhase.DESCENDING) {
            engine.onCastTap()
        }
        engine.entities.clear()
        engine.entities.add(
            SwimEntity(
                id = 900L + type.ordinal,
                x = FishingGameEngine.HOOK_X,
                y = engine.hookY,
                vx = 0f,
                type = type
            )
        )
        engine.update(0.02f)
        assertNotNull("deveria prender $type", engine.attached)
        riseToSurface(engine)
    }

    @Test
    fun normalCatch_addsPointsAndCombo1() {
        val engine = FishingGameEngine(Random(1))
        forceCatch(engine, FishingCatchType.SMALL_FISH)
        assertEquals(10, engine.score)
        assertEquals(1, engine.combo)
        assertNull(engine.attached)
    }

    @Test
    fun rareCatch_addsHigherPoints() {
        val engine = FishingGameEngine(Random(2))
        forceCatch(engine, FishingCatchType.STARFISH)
        assertEquals(75, engine.score)
        assertEquals(1, engine.combo)
    }

    @Test
    fun junk_noPoints_breaksCombo_showsReaction() {
        val engine = FishingGameEngine(Random(3))
        forceCatch(engine, FishingCatchType.SMALL_FISH)
        assertEquals(1, engine.combo)
        forceCatch(engine, FishingCatchType.TRASH)
        assertEquals(10, engine.score)
        assertEquals(0, engine.combo)
        assertEquals("Eca! Isso não é peixe! 😖", engine.reactionText)
    }

    @Test
    fun combo_increasesOnConsecutiveFish() {
        val engine = FishingGameEngine(Random(4))
        forceCatch(engine, FishingCatchType.SMALL_FISH) // x1 → +10
        forceCatch(engine, FishingCatchType.SMALL_FISH) // x2 → +20
        forceCatch(engine, FishingCatchType.SMALL_FISH) // x3 → +30
        assertEquals(3, engine.combo)
        assertEquals(60, engine.score)
    }

    @Test
    fun combo_cappedAtX5() {
        val engine = FishingGameEngine(Random(5))
        repeat(7) { forceCatch(engine, FishingCatchType.SMALL_FISH) }
        assertEquals(FishingGameEngine.MAX_COMBO, engine.combo)
        // 10*(1+2+3+4+5+5+5) = 10*25 = 250
        assertEquals(250, engine.score)
    }

    @Test
    fun computeCoins_followsEconomyCurve() {
        assertEquals(5, FishingGameEngine.computeCoinsEarned(0))
        assertEquals(8, FishingGameEngine.computeCoinsEarned(100))
        assertEquals(55, FishingGameEngine.computeCoinsEarned(10_000))
    }

    @Test
    fun timer_60Seconds_triggersGameOver() {
        val engine = FishingGameEngine(Random(6))
        // 61s em passos de 0.05 (margem contra float)
        repeat(1220) { engine.update(0.05f) }
        assertTrue(engine.isGameOver)
        assertEquals(0f, engine.timeLeftSec, 0.01f)
    }

    @Test
    fun gameOver_emitsOnceAndFreezes() {
        val engine = FishingGameEngine(Random(7))
        var gameOvers = 0
        repeat(1220) {
            val events = engine.update(0.05f)
            if (events.contains(FishingGameEvent.GAME_OVER)) gameOvers++
        }
        assertEquals(1, gameOvers)
        val scoreAfter = engine.score
        engine.onCastTap()
        engine.update(0.05f)
        assertEquals(scoreAfter, engine.score)
        assertTrue(engine.isGameOver)
    }

    @Test
    fun restart_resetsState() {
        val engine = FishingGameEngine(Random(8))
        forceCatch(engine, FishingCatchType.COLORFUL_FISH)
        repeat(100) { engine.update(0.05f) }
        engine.reset()
        assertEquals(0, engine.score)
        assertEquals(0, engine.combo)
        assertEquals(FishingGameEngine.MATCH_DURATION_SEC, engine.timeLeftSec, 0.01f)
        assertFalse(engine.isGameOver)
        assertEquals(HookPhase.IDLE, engine.hookPhase)
        assertFalse(engine.hasClaimedRewards())
    }

    @Test
    fun consumeEndRewards_onlyOnce() {
        val engine = FishingGameEngine(Random(9))
        forceCatch(engine, FishingCatchType.PUFFER)
        repeat(1200) { engine.update(0.05f) }
        assertTrue(engine.isGameOver)
        val first = engine.consumeEndRewards()
        assertNotNull(first)
        assertEquals(engine.score, first!!.first)
        assertEquals(FishingGameEngine.computeCoinsEarned(engine.score), first.second)
        assertNull(engine.consumeEndRewards())
        assertTrue(engine.hasClaimedRewards())
    }

    @Test
    fun highscorePersistence_keepsMaximum() {
        assertEquals(420, max(100, 420))
        assertEquals(100, max(100, 50))
        assertEquals(0, max(0, 0))
    }

    @Test
    fun cast_emitsCastEvent() {
        val engine = FishingGameEngine(Random(10))
        assertEquals(FishingGameEvent.CAST, engine.onCastTap())
        assertEquals(HookPhase.DESCENDING, engine.hookPhase)
    }

    @Test
    fun physics_independentOfFrameRate_hookDescent() {
        val a = FishingGameEngine(Random(11))
        val b = FishingGameEngine(Random(11))
        a.onCastTap()
        b.onCastTap()
        repeat(30) { a.update(1f / 60f) }
        repeat(60) { b.update(1f / 120f) }
        assertEquals(a.hookY, b.hookY, 0.02f)
    }
}
