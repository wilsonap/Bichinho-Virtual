package com.example.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PetEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationSystemTest {

    private lateinit var context: Context
    private lateinit var prefs: NotificationPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = NotificationPreferences(context)
        prefs.resetAllAlerts()
        prefs.isNotificationsEnabled = true
        prefs.isHungerEnabled = true
        prefs.isHygieneEnabled = true
        prefs.isEnergyEnabled = true
        prefs.isHealthEnabled = true
        prefs.isLongingEnabled = true
    }

    private fun createSamplePet(
        hunger: Int = 100,
        hygiene: Int = 100,
        energy: Int = 100,
        health: Int = 100,
        happiness: Int = 100,
        isSleeping: Boolean = false,
        isHatched: Boolean = true,
        lastUpdate: Long = System.currentTimeMillis()
    ): PetEntity {
        return PetEntity(
            id = 1,
            name = "Pipoca",
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
            isHatched = isHatched,
            eggWarmProgress = 100,
            roomTheme = "decor_default",
            lastUpdateTimestamp = lastUpdate
        )
    }

    @Test
    fun testNotificationChannelsCreation() {
        NotificationHelper.createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_CARE_ID))
        assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_HEALTH_ID))
        assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_SILENT_ID))
    }

    @Test
    fun testOfflineStatsDecaySimulationAwake() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 3600 * 1000L)
        val pet = createSamplePet(hunger = 100, hygiene = 100, energy = 100, happiness = 100, lastUpdate = twoHoursAgo)

        val result = PetStatsCalculator.calculateSimulatedStats(pet, now)

        // 2 hours = 120 minutes
        // Hunger: 100 - (120/5) = 100 - 24 = 76
        // Energy: 100 - (120/6) = 100 - 20 = 80
        // Hygiene: 100 - (120/8) = 100 - 15 = 85
        // Happiness: 100 - (120/7) = 100 - 17 = 83
        assertEquals(76, result.hunger)
        assertEquals(80, result.energy)
        assertEquals(85, result.hygiene)
        assertEquals(83, result.happiness)
        assertTrue(result.wasLonging) // > 45 mins
    }

    @Test
    fun testOfflineStatsSleepingRecovery() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (3600 * 1000L)
        val pet = createSamplePet(hunger = 80, energy = 50, isSleeping = true, lastUpdate = oneHourAgo)

        val result = PetStatsCalculator.calculateSimulatedStats(pet, now)

        // 60 minutes sleeping -> energyGain = 60/2 = 30 -> 50 + 30 = 80
        // hungerLoss = 60/10 = 6 -> 80 - 6 = 74
        assertEquals(80, result.energy)
        assertEquals(74, result.hunger)
    }

    @Test
    fun testThresholdDelayEstimation() {
        // Pet with hunger 40. Needs threshold is 20.
        // Diff = 20 points. Awake hunger loss is 1 pt every 5 min -> 20 * 5 = 100 min.
        val pet = createSamplePet(hunger = 40, hygiene = 100, energy = 100, health = 100)
        val delay = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet)

        // Longing threshold is 45 min from 0 elapsed, so earliest threshold is 45 min
        assertTrue(delay in 15..45)
    }

    @Test
    fun testAntiSpamPerNeedTracking() {
        assertFalse(prefs.hasNotifiedHunger)

        // Mark hunger alert sent
        prefs.hasNotifiedHunger = true
        assertTrue(prefs.hasNotifiedHunger)

        // Bathing shouldn't reset hunger alert
        prefs.onPetBathed()
        assertTrue(prefs.hasNotifiedHunger)

        // Feeding resets hunger alert
        prefs.onPetFed()
        assertFalse(prefs.hasNotifiedHunger)
    }

    @Test
    fun testHygieneAlertResetOnBath() {
        prefs.hasNotifiedHygiene = true
        assertTrue(prefs.hasNotifiedHygiene)

        prefs.onPetBathed()
        assertFalse(prefs.hasNotifiedHygiene)
    }

    @Test
    fun testEnergyAlertResetOnSleep() {
        prefs.hasNotifiedEnergy = true
        assertTrue(prefs.hasNotifiedEnergy)

        prefs.onPetSlept()
        assertFalse(prefs.hasNotifiedEnergy)
    }

    @Test
    fun testHealthAlertResetOnDoctor() {
        prefs.hasNotifiedHealth = true
        assertTrue(prefs.hasNotifiedHealth)

        prefs.onPetDoctorTreated()
        assertFalse(prefs.hasNotifiedHealth)
    }

    @Test
    fun testDailyNotificationLimitCap() {
        assertTrue(prefs.canSendCareNotificationToday())

        prefs.incrementDailyNotificationCount()
        assertEquals(1, prefs.getDailyCount())
        assertTrue(prefs.canSendCareNotificationToday())

        prefs.incrementDailyNotificationCount()
        assertEquals(2, prefs.getDailyCount())
        assertTrue(prefs.canSendCareNotificationToday())

        prefs.incrementDailyNotificationCount()
        assertEquals(3, prefs.getDailyCount())
        assertFalse(prefs.canSendCareNotificationToday()) // Cap reached: max 3 per day
    }

    @Test
    fun testMasterSwitchDisablesNotifications() {
        prefs.isNotificationsEnabled = false
        assertFalse(prefs.isNotificationsEnabled)
    }

    @Test
    fun testEggDoesNotTriggerNeeds() {
        val eggPet = createSamplePet(isHatched = false)
        val delay = PetStatsCalculator.estimateMinutesUntilNextThreshold(eggPet)
        assertEquals(60L, delay)
    }
}
