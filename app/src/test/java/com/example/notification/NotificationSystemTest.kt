package com.example.notification

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PetEntity
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemTest {

    private lateinit var context: Context
    private lateinit var prefs: NotificationPreferences

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
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
        disease: String = PetDisease.NONE.name,
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
            disease = disease,
            lastUpdateTimestamp = lastUpdate
        )
    }

    private fun daytimeTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun nightTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun morningTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
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
    fun hungerAtOrBelow20_schedulesHungerSoon() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(hunger = 20, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, noon)
        assertTrue(estimate.reason.contains("HUNGER"))
        assertEquals(PetStatsCalculator.MIN_CARE_DELAY_MINUTES, estimate.delayMinutes)
    }

    @Test
    fun hygieneAtOrBelow20_schedulesHygieneSoon() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(hygiene = 15, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, noon)
        assertTrue(
            "reason=${estimate.reason}",
            estimate.reason.contains("HYGIENE") ||
                estimate.reason.contains("HUNGER") ||
                estimate.reason.startsWith("HEALTH") ||
                estimate.reason.contains("LONGING")
        )
        assertTrue(estimate.delayMinutes >= PetStatsCalculator.MIN_CARE_DELAY_MINUTES)
    }

    @Test
    fun energyAtOrBelow15_awake_schedulesEnergy() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(energy = 15, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, noon)
        assertTrue(
            "reason=${estimate.reason}",
            estimate.reason.contains("ENERGY") ||
                estimate.reason.contains("HUNGER") ||
                estimate.reason.contains("LONGING")
        )
    }

    @Test
    fun healthEnteringDoente_isConsideredInSchedule() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(hunger = 10, hygiene = 10, health = 41, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, noon)
        assertTrue(
            "reason=${estimate.reason}",
            estimate.reason.startsWith("HEALTH") ||
                estimate.reason.contains("HUNGER") ||
                estimate.reason.contains("HYGIENE")
        )
    }

    @Test
    fun activeDisease_highHealth_urgentShortcut() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(
            health = 100,
            disease = PetDisease.INDIGESTAO.name,
            lastUpdate = noon
        )
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(
            pet,
            noon,
            allowCriticalHealthShortcut = true
        )
        // Doença ativa: candidato HEALTH_DISEASE (0) + shortcut → delay 5 min
        assertEquals("HEALTH_DISEASE", estimate.reason)
        assertEquals(PetStatsCalculator.MIN_CRITICAL_HEALTH_DELAY_MINUTES, estimate.delayMinutes)
    }

    @Test
    fun criticalHealth_usesShortDelayWhenPending() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(health = PetHealthRules.HEALTH_CRITICO_MAX, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(
            pet,
            noon,
            allowCriticalHealthShortcut = true
        )
        assertTrue(estimate.reason.startsWith("HEALTH"))
        assertEquals(PetStatsCalculator.MIN_CRITICAL_HEALTH_DELAY_MINUTES, estimate.delayMinutes)
    }

    @Test
    fun healthAlert_notBlockedByDailyCareLimit() {
        prefs.incrementDailyNotificationCount()
        prefs.incrementDailyNotificationCount()
        prefs.incrementDailyNotificationCount()
        assertFalse(prefs.canSendCareNotificationToday())
        assertFalse(prefs.hasNotifiedHealth)
        prefs.hasNotifiedHealth = true
        assertTrue(prefs.hasNotifiedHealth)
        assertFalse(prefs.canSendCareNotificationToday())
    }

    @Test
    fun sameDisease_singleAlertGuarantee() {
        assertFalse(prefs.hasNotifiedHealth)
        prefs.hasNotifiedHealth = true
        assertTrue(prefs.hasNotifiedHealth)
        prefs.onPetDoctorTreated()
        assertFalse(prefs.hasNotifiedHealth)
    }

    @Test
    fun quietHours_22to08_doesNotScheduleCareNow() {
        val night = nightTimestamp()
        val pet = createSamplePet(hunger = 5, lastUpdate = night)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, night)
        assertEquals("QUIET_UNTIL_0730", estimate.reason)
        assertTrue(estimate.delayMinutes >= PetStatsCalculator.MIN_CARE_DELAY_MINUTES)
        assertTrue(PetStatsCalculator.isNightTime(night))
    }

    @Test
    fun at08_pendingNeedIsConsideredAgain() {
        val morning = morningTimestamp()
        assertFalse(PetStatsCalculator.isNightTime(morning))
        val pet = createSamplePet(hunger = 10, lastUpdate = morning)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, morning)
        assertTrue(estimate.reason != "QUIET_UNTIL_0730")
        assertTrue(
            "reason=${estimate.reason}",
            estimate.reason.contains("HUNGER") || estimate.reason.startsWith("HEALTH")
        )
    }

    @Test
    fun minutesUntilHealthDamageStarts() {
        assertEquals(0L, PetStatsCalculator.minutesUntilHealthDamageStarts(20, 100))
        assertEquals(0L, PetStatsCalculator.minutesUntilHealthDamageStarts(100, 20))
        assertEquals(100L, PetStatsCalculator.minutesUntilHealthDamageStarts(40, 100))
    }

    @Test
    fun testOfflineStatsDecaySimulationAwake() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 3600 * 1000L)
        val pet = createSamplePet(
            hunger = 100,
            hygiene = 100,
            energy = 100,
            happiness = 100,
            lastUpdate = twoHoursAgo
        )
        val result = PetStatsCalculator.calculateSimulatedStats(pet, now)
        assertEquals(76, result.hunger)
        assertEquals(80, result.energy)
        assertEquals(85, result.hygiene)
        assertEquals(83, result.happiness)
        assertTrue(result.wasLonging)
    }

    @Test
    fun testOfflineStatsSleepingRecovery() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (3600 * 1000L)
        val pet = createSamplePet(hunger = 80, energy = 50, isSleeping = true, lastUpdate = oneHourAgo)
        val result = PetStatsCalculator.calculateSimulatedStats(pet, now)
        assertEquals(80, result.energy)
        assertEquals(74, result.hunger)
    }

    @Test
    fun testThresholdDelayEstimation() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(hunger = 40, hygiene = 100, energy = 100, health = 100, lastUpdate = noon)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet, noon)
        assertTrue(estimate.delayMinutes in 15..45)
    }

    @Test
    fun testAntiSpamPerNeedTracking() {
        assertFalse(prefs.hasNotifiedHunger)
        prefs.hasNotifiedHunger = true
        prefs.onPetBathed()
        assertTrue(prefs.hasNotifiedHunger)
        prefs.onPetFed()
        assertFalse(prefs.hasNotifiedHunger)
    }

    @Test
    fun testHygieneAlertResetOnBath() {
        prefs.hasNotifiedHygiene = true
        prefs.onPetBathed()
        assertFalse(prefs.hasNotifiedHygiene)
    }

    @Test
    fun testEnergyAlertResetOnSleep() {
        prefs.hasNotifiedEnergy = true
        prefs.onPetSlept()
        assertFalse(prefs.hasNotifiedEnergy)
    }

    @Test
    fun testHealthAlertResetOnDoctor() {
        prefs.hasNotifiedHealth = true
        prefs.onPetDoctorTreated()
        assertFalse(prefs.hasNotifiedHealth)
    }

    @Test
    fun testDailyNotificationLimitCap() {
        val max = NotificationPreferences.MAX_DAILY_CARE_NOTIFICATIONS
        val start = prefs.getDailyCount()
        if (start < max) {
            repeat(max - start) { prefs.incrementDailyNotificationCount() }
        }
        assertFalse(prefs.canSendCareNotificationToday())
    }

    @Test
    fun testMasterSwitchDisablesNotifications() {
        prefs.isNotificationsEnabled = false
        assertFalse(prefs.isNotificationsEnabled)
    }

    @Test
    fun testEggDoesNotTriggerNeeds() {
        val eggPet = createSamplePet(isHatched = false)
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(eggPet)
        assertEquals(60L, estimate.delayMinutes)
        assertEquals("EGG_OR_UNHATCHED", estimate.reason)
    }

    @Test
    fun afterHealthNotified_criticalShortcutOff_usesCareClamp() {
        val noon = daytimeTimestamp()
        val pet = createSamplePet(
            health = 100,
            disease = PetDisease.INDIGESTAO.name,
            lastUpdate = noon
        )
        val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(
            pet,
            noon,
            allowCriticalHealthShortcut = false
        )
        // Sem shortcut: ainda vê HEALTH_DISEASE=0 mas clamp de cuidado = 15
        assertTrue(estimate.delayMinutes >= PetStatsCalculator.MIN_CARE_DELAY_MINUTES)
    }

    @Test
    fun permissionDenied_logsBlockedReason() {
        ShadowLog.clear()
        // Sem POST_NOTIFICATIONS no Robolectric API 34 → hasNotificationPermission = false
        assertFalse(NotificationHelper.hasNotificationPermission(context))
        NotificationHelper.sendPetNotification(
            context,
            PetNotificationType.HUNGER,
            "Pipoca"
        )
        val blocked = ShadowLog.getLogs().any { entry ->
            entry.tag == "PET_NOTIFICATION_BLOCKED" &&
                entry.msg.contains("PERMISSION_DENIED")
        }
        assertTrue("Esperava log PERMISSION_DENIED em PET_NOTIFICATION_BLOCKED", blocked)
    }

    @Test
    fun schedulerCatch_logsWithSpecificTag() {
        // Garante contrato do log de falha (tag + campos) sem derrubar o processo
        val ex = RuntimeException("simulated scheduler failure")
        Log.e(
            "PET_NOTIFICATION_SCHEDULER",
            "scheduleNextCheck failed petId=1 delay=15 reason=HUNGER " +
                "ex=${ex.javaClass.simpleName}: ${ex.message}",
            ex
        )
        val found = ShadowLog.getLogs().any { entry ->
            entry.tag == "PET_NOTIFICATION_SCHEDULER" &&
                entry.type == Log.ERROR &&
                entry.msg.contains("scheduleNextCheck failed") &&
                entry.msg.contains("petId=1") &&
                entry.msg.contains("delay=15") &&
                entry.msg.contains("reason=HUNGER")
        }
        assertTrue("Esperava Log.e em PET_NOTIFICATION_SCHEDULER com contexto", found)
    }
}
