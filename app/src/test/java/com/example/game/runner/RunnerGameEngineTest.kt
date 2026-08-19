package com.example.game.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class RunnerGameEngineTest {

    @Test
    fun jumpHeight_matchesLegacyDiscreteAt20ms() {
        // Simulação legada (tick 20 ms)
        var legacyY = 0f
        var legacyV = -18f
        var legacyMin = 0f
        while (true) {
            legacyY += legacyV
            legacyV += 1.1f
            if (legacyY < legacyMin) legacyMin = legacyY
            if (legacyY >= 0f) break
        }

        val engine = RunnerGameEngine(Random(0))
        assertTrue(engine.jump())
        val dt = 1f / RunnerGameEngine.LEGACY_TICKS_PER_SECOND
        var engineMin = 0f
        repeat(200) {
            engine.update(dt)
            if (engine.petY < engineMin) engineMin = engine.petY
            if (engine.isGrounded && engine.petY == 0f && engineMin < 0f) return@repeat
        }

        assertEquals(legacyMin, engineMin, 0.5f)
        assertTrue(engine.isGrounded)
    }

    @Test
    fun distanceAndSpeed_independentOfFrameRate() {
        val lowFps = RunnerGameEngine(Random(1))
        val highFps = RunnerGameEngine(Random(1))

        // 2.5s — abaixo do marco de 150m para evitar limiar float
        repeat(150) { lowFps.update(1f / 60f) }
        repeat(300) { highFps.update(1f / 120f) }

        assertEquals(lowFps.distance, highFps.distance, 0.75f)
        assertEquals(lowFps.speed, highFps.speed, 0.01f)
        assertEquals(RunnerGameEngine.INITIAL_SPEED, lowFps.speed, 0.01f)
    }

    @Test
    fun speedIncreasesEvery150Distance() {
        val engine = RunnerGameEngine(Random(2))
        val initial = engine.speed

        // > 3.1s a 50 unidades/s → passa de 150 com folga
        repeat(160) { engine.update(1f / 50f) }

        assertTrue(engine.distance >= 150f)
        assertEquals(initial + RunnerGameEngine.SPEED_INCREMENT, engine.speed, 0.01f)
    }

    @Test
    fun spawnInterval_independentOfFps() {
        val engine60 = RunnerGameEngine(Random(42))
        val engine120 = RunnerGameEngine(Random(42))

        // > 1.6s com folga para evitar undershoot float
        repeat(100) { engine60.update(1f / 60f) }
        repeat(200) { engine120.update(1f / 120f) }

        assertEquals(engine60.obstacles.size, engine120.obstacles.size)
        assertTrue(engine60.obstacles.isNotEmpty())
    }

    @Test
    fun collision_triggersGameOver() {
        val engine = RunnerGameEngine(Random(0))
        engine.obstacles.add(RunnerObstacle(x = 140f, width = 36f, height = 48f))
        val events = engine.update(0.001f)
        assertTrue(engine.isGameOver)
        assertTrue(events.contains(RunnerGameEvent.COLLISION))
    }

    @Test
    fun coinPickup_incrementsCounter() {
        val engine = RunnerGameEngine(Random(0))
        engine.collectibleCoins.add(RunnerCoin(x = 140f, y = -30f))
        val events = engine.update(0.001f)
        assertEquals(1, engine.coinsCollected)
        assertTrue(events.contains(RunnerGameEvent.COIN_COLLECTED))
        assertFalse(engine.isGameOver)
    }

    @Test
    fun totalCoinsEarned_matchesLegacyFormula() {
        val engine = RunnerGameEngine(Random(0))
        repeat(80) { engine.update(1f / 50f) }
        engine.collectibleCoins.add(RunnerCoin(x = 140f, y = -30f))
        engine.update(0.001f)
        engine.collectibleCoins.add(RunnerCoin(x = 140f, y = -30f))
        engine.update(0.001f)

        val distance = engine.distanceScoreInt()
        assertEquals(engine.coinsCollected + (distance / 40), engine.totalCoinsEarned())
    }

    @Test
    fun reset_clearsStateAndAllowsJumpAgain() {
        val engine = RunnerGameEngine(Random(0))
        engine.obstacles.add(RunnerObstacle(x = 140f, height = 48f))
        engine.update(0.001f)
        assertTrue(engine.isGameOver)

        engine.reset()
        assertFalse(engine.isGameOver)
        assertEquals(0, engine.obstacles.size)
        assertEquals(0f, engine.distance, 0f)
        assertTrue(engine.jump())
    }

    @Test
    fun jump_rejectedWhenAirborneOrGameOver() {
        val engine = RunnerGameEngine(Random(0))
        assertTrue(engine.jump())
        assertFalse(engine.jump())

        engine.reset()
        engine.obstacles.add(RunnerObstacle(x = 140f, height = 48f))
        engine.update(0.001f)
        assertFalse(engine.jump())
    }

    @Test
    fun physicsRate_doesNotAccelerateWithHigherFps() {
        val a = RunnerGameEngine(Random(7))
        val b = RunnerGameEngine(Random(7))
        a.jump()
        b.jump()

        // Mesmo tempo de voo em 60 vs 120 Hz (Euler tem erro O(dt); tolera diferença pequena)
        repeat(30) { a.update(1f / 60f) }
        repeat(60) { b.update(1f / 120f) }

        assertEquals(a.distance, b.distance, 0.5f)
        // Posição no ar: mesma ordem de magnitude, sem “acelerar” o dobro no 120 Hz
        assertTrue(abs(a.petY - b.petY) < 12f)
        assertTrue(a.petY < -1f && b.petY < -1f)
    }
}
