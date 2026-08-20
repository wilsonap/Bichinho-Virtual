package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PetDao
import com.example.data.local.PetEntity
import com.example.data.model.PetBehaviorState
import com.example.data.repository.PetRepository
import com.example.notification.PetStatsCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SleepSystemTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var petDao: PetDao
    private lateinit var repository: PetRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        petDao = database.petDao()
        repository = PetRepository(petDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun getTimestampForHour(dayOffset: Int = 0, hour: Int, minute: Int = 0): Long {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 16 + dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun createTestPet(
        energy: Int = 80,
        isSleeping: Boolean = false,
        hunger: Int = 90,
        hygiene: Int = 90,
        health: Int = 100,
        happiness: Int = 100,
        lastUpdate: Long = getTimestampForHour(0, 12, 0)
    ): PetEntity {
        return PetEntity(
            id = 1,
            name = "Mochi",
            speciesId = "cat_01",
            stage = "FILHOTE",
            rarity = "COMUM",
            level = 1,
            exp = 0,
            hunger = hunger,
            hygiene = hygiene,
            energy = energy,
            health = health,
            happiness = happiness,
            isSleeping = isSleeping,
            isHatched = true,
            eggWarmProgress = 100,
            roomTheme = "decor_default",
            sleepStartTimestamp = if (isSleeping) lastUpdate else 0L,
            lastUpdateTimestamp = lastUpdate
        )
    }

    // 1. Energia diminuindo normalmente durante o dia
    @Test
    fun testDaytimeEnergyDecayWhenAwake() {
        val t1000 = getTimestampForHour(0, 10, 0)
        val t1200 = getTimestampForHour(0, 12, 0) // 120 minutes awake
        val pet = createTestPet(energy = 100, isSleeping = false, lastUpdate = t1000)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t1200)

        // 120 mins awake -> 120 / 6 = 20 energy loss -> 80 energy
        assertEquals(80, simulated.energy)
        assertFalse(simulated.isSleeping)
    }

    // 2. Sono por energia baixa durante o dia
    @Test
    fun testDaytimeExhaustionAutoSleep() {
        val t1400 = getTimestampForHour(0, 14, 0)
        val t1405 = getTimestampForHour(0, 14, 5)
        // Energy starts at 5% (exhaustion threshold)
        val pet = createTestPet(energy = 5, isSleeping = false, lastUpdate = t1400)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t1405)

        // Should automatically enter sleep mode due to exhaustion
        assertTrue(simulated.isSleeping)
    }

    // 3. Despertar diurno ao atingir 100% de energia
    @Test
    fun testDaytimeAwakeningAt100PercentEnergy() {
        val t1400 = getTimestampForHour(0, 14, 0)
        val t1440 = getTimestampForHour(0, 14, 40) // 40 minutes sleeping
        // 80 energy needs 20 points = 40 minutes
        val pet = createTestPet(energy = 80, isSleeping = true, lastUpdate = t1400)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t1440)

        // Daytime sleep must automatically wake up at 100% energy!
        assertEquals(100, simulated.energy)
        assertFalse(simulated.isSleeping)
    }

    // 4. Pet com 100% de energia às 22:00 entrando em sono noturno mesmo assim
    @Test
    fun testNightSleepStartsAt2200EvenWith100Energy() {
        val t2159 = getTimestampForHour(0, 21, 59)
        val t2205 = getTimestampForHour(0, 22, 5)
        val pet = createTestPet(energy = 100, isSleeping = false, lastUpdate = t2159)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t2205)

        // At 22:00, night sleep starts regardless of high energy!
        assertEquals(100, simulated.energy)
        assertTrue(simulated.isSleeping)
    }

    // 5. Pet chegando a 100% às 02:00 e permanecendo dormindo
    @Test
    fun testNightSleepRemainsSleepingAt0200With100Energy() {
        val t2200 = getTimestampForHour(0, 22, 0)
        val t0200 = getTimestampForHour(1, 2, 0) // 4 hours = 240 mins into night
        // Energy was 80 at 22:00 -> reaches 100 at 22:40 -> at 02:00 must STAY sleeping!
        val pet = createTestPet(energy = 80, isSleeping = true, lastUpdate = t2200)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0200)

        assertEquals(100, simulated.energy)
        assertTrue(simulated.isSleeping) // Must still be sleeping during night!
    }

    // 6. Despertar automático às 07:30
    @Test
    fun testAutoAwakeningAt0730EndsNightSleep() {
        val t2200 = getTimestampForHour(0, 22, 0)
        val t0730 = getTimestampForHour(1, 7, 30)
        val pet = createTestPet(energy = 80, isSleeping = true, lastUpdate = t2200)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0730)

        assertEquals(100, simulated.energy)
        assertFalse(simulated.isSleeping)
    }

    // 7. App fechado antes das 22:00 (ex: 18:00) e calculado à noite
    @Test
    fun testAppClosedBefore2200AndCalculatedDuringNight() {
        val t1800 = getTimestampForHour(0, 18, 0)
        val t2300 = getTimestampForHour(0, 23, 0) // 5 hours later (4h day + 1h night)
        val pet = createTestPet(energy = 80, isSleeping = false, lastUpdate = t1800)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t2300)

        // 18:00 to 22:00 (240 mins awake) -> energy drops from 80 by 40 -> 40
        // 22:00 to 23:00 (60 mins night sleep) -> energy gains 30 -> ~70-72
        assertTrue("Energia recuperada durante a noite", simulated.energy in 70..72)
        assertTrue(simulated.isSleeping)
    }

    // 8. App aberto depois das 07:30 (ex: 10:00) após noite completa
    @Test
    fun testAppOpenedAfter0730FollowingFullNight() {
        val t1800 = getTimestampForHour(0, 18, 0)
        val t1000 = getTimestampForHour(1, 10, 0)
        val pet = createTestPet(energy = 80, isSleeping = false, lastUpdate = t1800)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t1000)

        // 18:00-22:00 (240 min): energy -> 40
        // 22:00-07:30 (570 min night): energy -> 100, wake at 07:30
        // 07:30-10:00 (150 min day): energy -25 -> 75
        assertEquals(75, simulated.energy)
        assertFalse(simulated.isSleeping)
    }

    // 9. App aberto às 03:00 durante a madrugada
    @Test
    fun testAppOpenedAt0300ShowsNightSleep() {
        val t0300 = getTimestampForHour(0, 3, 0)
        assertTrue(PetStatsCalculator.isNightTime(t0300))

        val pet = createTestPet(energy = 100, isSleeping = true, lastUpdate = t0300 - 30 * 60 * 1000L)
        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0300)

        assertTrue(simulated.isSleeping)
        assertEquals(100, simulated.energy)
    }

    // 10. Fome durante a madrugada (reduz em ritmo menor)
    @Test
    fun testNighttimeHungerDecayRate() {
        val t2200 = getTimestampForHour(0, 22, 0)
        val t0730 = getTimestampForHour(1, 7, 30) // 570 minutes of night sleep
        val pet = createTestPet(hunger = 90, energy = 50, isSleeping = true, lastUpdate = t2200)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0730)

        // 570 mins / 10 = 57 hunger loss -> 90 - 57 = 33
        assertTrue("Fome após madrugada", simulated.hunger in 33..34)
    }

    // 11. Ausência de dano de saúde pela madrugada
    @Test
    fun testNoHealthDamageDuringNighttime() {
        val t2200 = getTimestampForHour(0, 22, 0)
        val t0730 = getTimestampForHour(1, 7, 30)
        val pet = createTestPet(hunger = 10, hygiene = 10, health = 100, isSleeping = true, lastUpdate = t2200)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0730)

        assertEquals(100, simulated.health)
    }

    // 12. Ausência de notificações de cuidados durante a madrugada
    @Test
    fun testNoCareNotificationsDuringNighttime() {
        val t0300 = getTimestampForHour(0, 3, 0)
        val pet = createTestPet(hunger = 10, hygiene = 10, energy = 10, health = 10, lastUpdate = t0300)

        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, t0300)
        // 03:00 to 07:30 = 4.5 hours = 270 minutes
        assertEquals("QUIET_UNTIL_0730", estimate.reason)
        assertEquals(270L, estimate.delayMinutes)
    }

    // 13. Retorno das regras normais depois das 07:30
    @Test
    fun testDaytimeRulesResumeAfter0730() {
        val t0730 = getTimestampForHour(0, 7, 30)
        val t0930 = getTimestampForHour(0, 9, 30) // 120 mins daytime awake
        val pet = createTestPet(hunger = 15, hygiene = 90, health = 100, energy = 100, isSleeping = false, lastUpdate = t0730)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0930)

        assertEquals(88, simulated.health)
        assertEquals(80, simulated.energy)
        assertFalse(simulated.isSleeping)
    }

    // Validação dos nomes dos estados de comportamento
    @Test
    fun testBehaviorStateDefinitions() {
        assertEquals("Dormindo", PetBehaviorState.DORMINDO.displayName)
        assertEquals("Acordando", PetBehaviorState.ACORDANDO.displayName)
        assertEquals("Muito Feliz", PetBehaviorState.FELIZ.displayName)
    }

    /**
     * Live: último tick às 07:29 (noite) → tick às 07:30 deve acordar
     * independentemente da energia.
     */
    @Test
    fun testLiveTickAutoAwakeningAt0730RegardlessOfEnergy() = runBlocking {
        val t0729 = getTimestampForHour(0, 7, 29)
        val t0730 = getTimestampForHour(0, 7, 30)
        val pet = createTestPet(energy = 40, isSleeping = true, lastUpdate = t0729)
        petDao.insertOrUpdatePet(pet)

        val updated = repository.tickLiveStats(now = t0730)

        assertNotNull(updated)
        assertFalse(
            "Às 07:30 o pet deve acordar no tick live mesmo com energia < 100",
            updated!!.isSleeping
        )
        assertEquals(39, updated.energy)
    }

    /**
     * Offline: 07:29 → 07:30 também acorda com energia < 100.
     */
    @Test
    fun testOfflineAutoAwakeningAt0730RegardlessOfEnergy() {
        val t0729 = getTimestampForHour(0, 7, 29)
        val t0730 = getTimestampForHour(0, 7, 30)
        val pet = createTestPet(energy = 40, isSleeping = true, lastUpdate = t0729)

        val simulated = PetStatsCalculator.calculateSimulatedStats(pet, t0730)

        assertFalse(simulated.isSleeping)
        assertEquals(40, simulated.energy)
    }

    /**
     * Sono por exaustão diurno permanece: último update já de dia + energia < 100
     * → tick live NÃO força despertar só por ser horário diurno.
     */
    @Test
    fun testLiveTickDoesNotForceWakeDuringDaytimeExhaustionNap() = runBlocking {
        val t1400 = getTimestampForHour(0, 14, 0)
        val t1401 = getTimestampForHour(0, 14, 1)
        val pet = createTestPet(energy = 50, isSleeping = true, lastUpdate = t1400)
        petDao.insertOrUpdatePet(pet)

        val updated = repository.tickLiveStats(now = t1401)

        assertNotNull(updated)
        assertTrue(
            "Sono por exaustão diurno deve continuar até energia 100%",
            updated!!.isSleeping
        )
        assertEquals(52, updated.energy) // +2 no tick de sono diurno
    }
}
