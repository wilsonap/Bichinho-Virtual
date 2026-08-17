package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PetCareWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val pet = db.petDao().getPet() ?: return@withContext Result.success()

            if (!pet.isHatched) {
                return@withContext Result.success()
            }

            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isNotificationsEnabled) {
                return@withContext Result.success()
            }

            // Check if currently within protected night hours (22:00 to 08:00)
            if (PetStatsCalculator.isNightTime()) {
                // Do NOT send care notifications during the night!
                PetCareScheduler.scheduleNextCheck(applicationContext)
                return@withContext Result.success()
            }

            // Single source of truth calculation
            val simulated = PetStatsCalculator.calculateSimulatedStats(pet, System.currentTimeMillis())

            // 1. Health Alert (Highest priority)
            val healthState = com.example.data.model.PetHealthState.fromHealth(simulated.health)
            val petDisease = com.example.data.model.PetDisease.fromString(simulated.disease)
            val effectivePetName = pet.name.ifBlank { "Seu bichinho" }

            if (healthState != com.example.data.model.PetHealthState.SAUDAVEL || petDisease != com.example.data.model.PetDisease.NONE) {
                if (prefs.isHealthEnabled && !prefs.hasNotifiedHealth) {
                    val (healthTitle, healthMsg) = when {
                        healthState == com.example.data.model.PetHealthState.CRITICO -> Pair(
                            "Saúde Crítica! 🚨",
                            "🚨 A saúde de $effectivePetName está crítica! Dê remédios ou leve ao médico imediatamente."
                        )
                        petDisease != com.example.data.model.PetDisease.NONE -> Pair(
                            "Bichinho Doente! 🩹",
                            "🩹 $effectivePetName está com ${petDisease.displayName} e precisa de cuidados."
                        )
                        healthState == com.example.data.model.PetHealthState.DOENTE -> Pair(
                            "Bichinho Doente! 🩹",
                            "🩹 $effectivePetName ficou doente e precisa de cuidados."
                        )
                        else -> Pair(
                            "Bichinho Indisposto 💛",
                            "💛 $effectivePetName não está se sentindo muito bem."
                        )
                    }
                    NotificationHelper.sendPetNotification(
                        applicationContext,
                        PetNotificationType.HEALTH,
                        pet.name,
                        customTitle = healthTitle,
                        customMessage = healthMsg
                    )
                    prefs.hasNotifiedHealth = true
                    prefs.incrementDailyNotificationCount()
                }
            }

            // 2. Hunger Alert (Respects daily limit & anti-spam)
            if (simulated.hunger <= PetStatsCalculator.HUNGER_THRESHOLD) {
                if (prefs.isHungerEnabled && !prefs.hasNotifiedHunger && prefs.canSendCareNotificationToday()) {
                    NotificationHelper.sendPetNotification(
                        applicationContext,
                        PetNotificationType.HUNGER,
                        pet.name
                    )
                    prefs.hasNotifiedHunger = true
                    prefs.incrementDailyNotificationCount()
                }
            }

            // 3. Hygiene Alert (Respects daily limit & anti-spam)
            if (simulated.hygiene <= PetStatsCalculator.HYGIENE_THRESHOLD) {
                if (prefs.isHygieneEnabled && !prefs.hasNotifiedHygiene && prefs.canSendCareNotificationToday()) {
                    NotificationHelper.sendPetNotification(
                        applicationContext,
                        PetNotificationType.HYGIENE,
                        pet.name
                    )
                    prefs.hasNotifiedHygiene = true
                    prefs.incrementDailyNotificationCount()
                }
            }

            // 4. Energy Alert (Only when awake; if already sleeping, no repeated alarm)
            if (simulated.energy <= PetStatsCalculator.ENERGY_THRESHOLD && !simulated.isSleeping) {
                if (prefs.isEnergyEnabled && !prefs.hasNotifiedEnergy && prefs.canSendCareNotificationToday()) {
                    NotificationHelper.sendPetNotification(
                        applicationContext,
                        PetNotificationType.ENERGY,
                        pet.name
                    )
                    prefs.hasNotifiedEnergy = true
                    prefs.incrementDailyNotificationCount()
                }
            }

            // 5. Longing Alert (Triggered when player is away for a long time)
            if (simulated.wasLonging) {
                val now = System.currentTimeMillis()
                val lastLonging = prefs.lastLongingNotificationTimestamp
                val elapsedSinceLastLonging = now - lastLonging
                // Send at most once every 6 hours of continuous absence
                if (prefs.isLongingEnabled && elapsedSinceLastLonging >= 6 * 60 * 60 * 1000L && prefs.canSendCareNotificationToday()) {
                    NotificationHelper.sendPetNotification(
                        applicationContext,
                        PetNotificationType.LONGING,
                        pet.name
                    )
                    prefs.lastLongingNotificationTimestamp = now
                    prefs.incrementDailyNotificationCount()
                }
            }

            // Re-schedule the next intelligent check
            PetCareScheduler.scheduleNextCheck(applicationContext)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
