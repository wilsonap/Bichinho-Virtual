package com.example.game.catchgame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class CatchGameEngineTest {

    @Test
    fun fallSpeed_independentOfFrameRate() {
        val a = CatchGameEngine(Random(1))
        val b = CatchGameEngine(Random(1))
        // força um item idêntico
        a.fallingItems.add(
            FallingItem(1, 0.5f, 0f, CatchItemType.APPLE, 0.5f)
        )
        b.fallingItems.add(
            FallingItem(1, 0.5f, 0f, CatchItemType.APPLE, 0.5f)
        )

        repeat(30) { a.update(1f / 60f) }
        repeat(60) { b.update(1f / 120f) }

        assertEquals(a.fallingItems[0].y, b.fallingItems[0].y, 0.01f)
    }

    @Test
    fun spawnInterval_approx0_7Seconds() {
        val engine = CatchGameEngine(Random(42))
        repeat(41) { engine.update(1f / 60f) } // ~0.683s — ainda 0
        assertEquals(0, engine.fallingItems.size)
        repeat(5) { engine.update(1f / 60f) } // passa de 0.7s
        assertTrue(engine.fallingItems.isNotEmpty())
    }

    @Test
    fun spawnRate_sameAt60And120Hz() {
        val e60 = CatchGameEngine(Random(7))
        val e120 = CatchGameEngine(Random(7))
        repeat(120) { e60.update(1f / 60f) } // 2s
        repeat(240) { e120.update(1f / 120f) }
        assertEquals(e60.fallingItems.size, e120.fallingItems.size)
    }

    @Test
    fun goodCatch_increasesScoreAndCoins() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.77f, CatchItemType.APPLE, 1f)
        )
        val events = engine.update(0.02f) // y → ~0.79 na zona
        assertTrue(events.contains(CatchGameEvent.GOOD_CATCH))
        assertEquals(10, engine.score)
        assertEquals(1, engine.coinsEarned)
        assertEquals(1, engine.combo)
        assertEquals(0, engine.fallingItems.size)
    }

    @Test
    fun coinCatch_givesTwoCoins() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.77f, CatchItemType.COIN, 1f)
        )
        val events = engine.update(0.02f)
        assertTrue(events.contains(CatchGameEvent.COIN_CATCH))
        assertEquals(2, engine.coinsEarned)
        assertEquals(20, engine.score)
    }

    @Test
    fun bomb_reducesLifeAndResetsCombo() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        // força combo > 0
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.77f, CatchItemType.STAR, 1f)
        )
        engine.update(0.02f)
        assertEquals(1, engine.combo)

        engine.fallingItems.add(
            FallingItem(2, 0.5f, 0.77f, CatchItemType.BOMB, 1f)
        )
        val events = engine.update(0.02f)
        assertTrue(events.contains(CatchGameEvent.BOMB_HIT))
        assertEquals(2, engine.lives)
        assertEquals(0, engine.combo)
        assertFalse(engine.isGameOver)
    }

    @Test
    fun threeBombs_triggerGameOverOnce() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        repeat(3) { idx ->
            engine.fallingItems.add(
                FallingItem(idx.toLong(), 0.5f, 0.77f, CatchItemType.BOMB, 1f)
            )
            engine.update(0.02f)
        }
        assertTrue(engine.isGameOver)
        assertEquals(0, engine.lives)
    }

    @Test
    fun tunnelThrough_catchZoneStillDetected() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        // Salto grande que atravessa 0.78..0.88
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.70f, CatchItemType.COOKIE, 10f)
        )
        val events = engine.update(0.05f) // Δy = 0.5 → y = 1.2
        assertTrue(events.contains(CatchGameEvent.GOOD_CATCH))
        assertEquals(15, engine.score)
    }

    @Test
    fun missGoodItem_resetsCombo() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.5f)
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.77f, CatchItemType.APPLE, 1f)
        )
        engine.update(0.02f)
        assertEquals(1, engine.combo)

        engine.fallingItems.add(
            FallingItem(2, 0.9f, 1.0f, CatchItemType.APPLE, 1f) // longe do pet
        )
        engine.update(0.1f)
        assertEquals(0, engine.combo)
    }

    @Test
    fun reset_clearsState() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.2f)
        engine.fallingItems.add(
            FallingItem(1, 0.5f, 0.77f, CatchItemType.BOMB, 1f)
        )
        engine.update(0.02f)
        engine.reset()
        assertEquals(0.5f, engine.petNormalizedX, 0f)
        assertEquals(0, engine.score)
        assertEquals(CatchGameEngine.INITIAL_LIVES, engine.lives)
        assertEquals(0, engine.fallingItems.size)
        assertFalse(engine.isGameOver)
    }

    @Test
    fun petNudge_clamped() {
        val engine = CatchGameEngine(Random(0))
        engine.setPetNormalizedX(0.05f)
        assertEquals(CatchGameEngine.PET_MIN_X, engine.petNormalizedX, 0f)
        engine.nudgePet(1f)
        assertEquals(CatchGameEngine.PET_MAX_X, engine.petNormalizedX, 0f)
    }

    @Test
    fun physicsDoesNotDoubleAt120Hz() {
        val a = CatchGameEngine(Random(3))
        val b = CatchGameEngine(Random(3))
        a.fallingItems.add(FallingItem(1, 0.5f, 0f, CatchItemType.STAR, 0.4f))
        b.fallingItems.add(FallingItem(1, 0.5f, 0f, CatchItemType.STAR, 0.4f))
        repeat(45) { a.update(1f / 60f) }
        repeat(90) { b.update(1f / 120f) }
        assertTrue(abs(a.fallingItems[0].y - b.fallingItems[0].y) < 0.02f)
    }
}
