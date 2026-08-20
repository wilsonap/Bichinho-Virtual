package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PetDao
import com.example.data.local.PetEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.PetSchoolRules
import com.example.data.model.PetStage
import com.example.data.repository.PetRepository
import com.example.notification.PetStatsCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchoolSystemTest {

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
        runBlocking {
            petDao.insertOrUpdatePlayer(PlayerEntity(id = 1, coins = 100))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun ts(dayOffset: Int, hour: Int, minute: Int = 0): Long {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 20 + dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun pet(
        stage: String = PetStage.FILHOTE.name,
        lastUpdate: Long = ts(0, 9, 0),
        isAtSchool: Boolean = false,
        schoolEnd: Long = 0L,
        lastReward: Long = 0L,
        hunger: Int = 80,
        hygiene: Int = 80,
        energy: Int = 80,
        happiness: Int = 70,
        health: Int = 100,
        totalExp: Int = 0
    ) = PetEntity(
        id = 1,
        name = "Pipoca",
        speciesId = "gato",
        stage = stage,
        rarity = "COMUM",
        level = 1,
        exp = 0,
        totalExp = totalExp,
        hunger = hunger,
        hygiene = hygiene,
        energy = energy,
        health = health,
        happiness = happiness,
        isSleeping = false,
        isHatched = true,
        eggWarmProgress = 100,
        isAtSchool = isAtSchool,
        schoolEndTimestamp = schoolEnd,
        lastSchoolRewardEndTimestamp = lastReward,
        lastUpdateTimestamp = lastUpdate
    )

    @Test
    fun filhote_canGoMorning() {
        val now = ts(0, 8, 15)
        val eligibility = PetSchoolRules.canAttendSchool(pet(stage = PetStage.FILHOTE.name), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Allowed)
        val allowed = eligibility as PetSchoolRules.Eligibility.Allowed
        assertEquals(PetSchoolRules.Shift.MORNING, allowed.shift)
        assertEquals(ts(0, 12, 0), allowed.endTimestamp)
    }

    @Test
    fun jovem_canGoAfternoon() {
        val now = ts(0, 13, 30)
        val eligibility = PetSchoolRules.canAttendSchool(pet(stage = PetStage.JOVEM.name), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Allowed)
        assertEquals(PetSchoolRules.Shift.AFTERNOON, (eligibility as PetSchoolRules.Eligibility.Allowed).shift)
    }

    @Test
    fun adulto_cannotGo() {
        val now = ts(0, 9, 0)
        val eligibility = PetSchoolRules.canAttendSchool(pet(stage = PetStage.ADULTO.name), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Denied)
    }

    @Test
    fun idoso_cannotGo() {
        val now = ts(0, 14, 0)
        val eligibility = PetSchoolRules.canAttendSchool(pet(stage = PetStage.IDOSO.name), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Denied)
    }

    @Test
    fun morningShift_freezesAttributes() {
        val start = ts(0, 8, 0)
        val mid = ts(0, 10, 0)
        val schoolEnd = ts(0, 12, 0)
        val p = pet(
            lastUpdate = start,
            isAtSchool = true,
            schoolEnd = schoolEnd,
            hunger = 80,
            hygiene = 80,
            energy = 80,
            happiness = 70
        )
        val sim = PetStatsCalculator.calculateSimulatedStats(p, mid)
        assertEquals(80, sim.hunger)
        assertEquals(80, sim.hygiene)
        assertEquals(80, sim.energy)
        assertEquals(70, sim.happiness)
        assertTrue(sim.isAtSchool)
    }

    @Test
    fun afternoonShift_freezesAttributes() {
        val start = ts(0, 13, 0)
        val mid = ts(0, 15, 0)
        val schoolEnd = ts(0, 17, 0)
        val p = pet(
            lastUpdate = start,
            isAtSchool = true,
            schoolEnd = schoolEnd,
            hunger = 75,
            hygiene = 75,
            energy = 75,
            happiness = 60
        )
        val sim = PetStatsCalculator.calculateSimulatedStats(p, mid)
        assertEquals(75, sim.hunger)
        assertEquals(75, sim.hygiene)
        assertEquals(75, sim.energy)
        assertEquals(60, sim.happiness)
        assertTrue(sim.isAtSchool)
    }

    @Test
    fun appClosedDuringSchool_preservesState() = runBlocking {
        val start = ts(0, 9, 0)
        val schoolEnd = ts(0, 12, 0)
        val mid = ts(0, 11, 0)
        petDao.insertOrUpdatePet(
            pet(lastUpdate = start, isAtSchool = true, schoolEnd = schoolEnd, hunger = 90)
        )
        // Simula abertura no meio da aula (offline catch-up)
        val stored = petDao.getPet()!!
        val sim = PetStatsCalculator.calculateSimulatedStats(stored, mid)
        assertTrue(sim.isAtSchool)
        assertEquals(90, sim.hunger)
    }

    @Test
    fun afterSchoolEnds_attributesDecayAgain() {
        val start = ts(0, 8, 0)
        val schoolEnd = ts(0, 12, 0)
        val after = ts(0, 13, 0) // 60 min após escola
        val p = pet(
            lastUpdate = start,
            isAtSchool = true,
            schoolEnd = schoolEnd,
            hunger = 80,
            energy = 80
        )
        val sim = PetStatsCalculator.calculateSimulatedStats(p, after)
        assertFalse(sim.isAtSchool)
        // 60 min acordado: hunger -12 (a cada 5), energy -10 (a cada 6)
        assertTrue("Fome deve ter caído após a escola", sim.hunger < 80)
        assertTrue("Energia deve ter caído após a escola", sim.energy < 80)
        assertEquals(schoolEnd, sim.completedSchoolEndTimestamp)
    }

    @Test
    fun rewardGrantedOnlyOnce() = runBlocking {
        val start = ts(0, 8, 0)
        val schoolEnd = ts(0, 12, 0)
        val after = ts(0, 12, 5)
        petDao.insertOrUpdatePet(
            pet(
                lastUpdate = start,
                isAtSchool = true,
                schoolEnd = schoolEnd,
                happiness = 50,
                totalExp = 0
            )
        )
        val first = repository.completeSchoolSession(petDao.getPet()!!, after)
        assertFalse(first.isAtSchool)
        assertEquals(schoolEnd, first.lastSchoolRewardEndTimestamp)
        val happinessAfter = first.happiness
        val expAfter = first.totalExp

        // Segunda chamada não deve recompensar de novo
        val again = repository.completeSchoolSession(
            first.copy(isAtSchool = true, schoolEndTimestamp = schoolEnd),
            after
        )
        assertEquals(happinessAfter, again.happiness)
        assertEquals(expAfter, again.totalExp)
    }

    @Test
    fun notificationsSuspendedDuringSchool() {
        val now = ts(0, 10, 0)
        val schoolEnd = ts(0, 12, 0)
        val p = pet(
            lastUpdate = now,
            isAtSchool = true,
            schoolEnd = schoolEnd,
            hunger = 10 // abaixo do threshold — mas escola suspende
        )
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(p, now)
        assertEquals("SCHOOL_UNTIL_END", estimate.reason)
    }

    @Test
    fun nightStartsAt2200() {
        assertTrue(PetStatsCalculator.isNightTime(ts(0, 22, 0)))
        assertFalse(PetStatsCalculator.isNightTime(ts(0, 21, 59)))
    }

    @Test
    fun nightEndsAt0730() {
        assertTrue(PetStatsCalculator.isNightTime(ts(0, 7, 29)))
        assertFalse(PetStatsCalculator.isNightTime(ts(0, 7, 30)))
    }

    @Test
    fun wakeAt0730EvenWithLowEnergy() = runBlocking {
        val night = ts(0, 7, 29)
        val day = ts(0, 7, 30)
        petDao.insertOrUpdatePet(
            pet(
                lastUpdate = night,
                energy = 40,
                happiness = 80
            ).copy(isSleeping = true)
        )
        // Força lastUpdate noturno no entity
        val sleeping = petDao.getPet()!!.copy(isSleeping = true, lastUpdateTimestamp = night, energy = 40)
        petDao.insertOrUpdatePet(sleeping)
        val updated = repository.tickLiveStats(day)!!
        assertFalse(updated.isSleeping)
        assertEquals(39, updated.energy) // acorda e aplica decay diurno no mesmo tick
    }

    @Test
    fun at0800_canStartMorningSchool() {
        val now = ts(0, 8, 0)
        assertFalse(PetStatsCalculator.isNightTime(now))
        val eligibility = PetSchoolRules.canAttendSchool(pet(stage = PetStage.FILHOTE.name), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Allowed)
    }

    @Test
    fun lateMorning_blockedWhenLessThanMinUseful() {
        val now = ts(0, 11, 50)
        val eligibility = PetSchoolRules.canAttendSchool(pet(), now)
        assertTrue(eligibility is PetSchoolRules.Eligibility.Denied)
    }
}
