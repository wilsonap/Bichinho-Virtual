package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PetCareWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "PET_NOTIFICATION_WORKER"
        try {
            Log.i(tag, "worker started")

            val db = AppDatabase.getDatabase(applicationContext)
            val pet = db.petDao().getPet() ?: run {
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=NO_PET")
                return@withContext Result.success()
            }

            if (!pet.isHatched) {
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=NOT_HATCHED petId=${pet.id}")
                return@withContext Result.success()
            }

            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isNotificationsEnabled) {
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=MASTER_OFF petId=${pet.id}")
                return@withContext Result.success()
            }

            if (PetStatsCalculator.isNightTime()) {
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=QUIET_HOURS petId=${pet.id}")
                PetCareScheduler.scheduleNextCheck(applicationContext)
                return@withContext Result.success()
            }

            val now = System.currentTimeMillis()
            if (pet.isAtSchool && pet.schoolEndTimestamp > now) {
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=AT_SCHOOL petId=${pet.id}")
                PetCareScheduler.scheduleNextCheck(applicationContext)
                return@withContext Result.success()
            }

            val simulated = PetStatsCalculator.calculateSimulatedStats(pet, System.currentTimeMillis())
            val healthState = PetHealthState.fromHealth(simulated.health)
            val petDisease = PetDisease.fromString(simulated.disease)
            val effectivePetName = pet.name.ifBlank { "Seu bichinho" }

            Log.i(
                tag,
                "eval petId=${pet.id} hunger=${simulated.hunger} hygiene=${simulated.hygiene} " +
                    "energy=${simulated.energy} health=${simulated.health} disease=${simulated.disease} " +
                    "sleeping=${simulated.isSleeping}"
            )

            // 1. Health — fora do cap diário de cuidados comuns; single-alert via hasNotifiedHealth
            if (healthState != PetHealthState.SAUDAVEL || petDisease != PetDisease.NONE) {
                when {
                    !prefs.isHealthEnabled ->
                        Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=HEALTH_DISABLED type=HEALTH")
                    prefs.hasNotifiedHealth ->
                        Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=ALREADY_NOTIFIED type=HEALTH")
                    else -> {
                        val (healthTitle, healthMsg) = when {
                            healthState == PetHealthState.CRITICO -> Pair(
                                "Saúde Crítica! 🚨",
                                "🚨 A saúde de $effectivePetName está crítica! Dê remédios ou leve ao médico imediatamente."
                            )
                            petDisease != PetDisease.NONE -> Pair(
                                "🤒 $effectivePetName não está bem",
                                "🤒 $effectivePetName não está se sentindo bem. Abra o jogo para cuidar dele."
                            )
                            healthState == PetHealthState.DOENTE -> Pair(
                                "🤒 $effectivePetName não está bem",
                                "🤒 $effectivePetName não está se sentindo bem. Abra o jogo para cuidar dele."
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
                        // Não incrementa MAX_DAILY_CARE — saúde crítica é canal separado
                    }
                }
            }

            // 2–5. Cuidados comuns (fome/higiene/energia/saudade) — respeitam cap diário = 3
            if (simulated.hunger <= PetStatsCalculator.HUNGER_THRESHOLD) {
                trySendCare(
                    prefs = prefs,
                    enabled = prefs.isHungerEnabled,
                    alreadyNotified = prefs.hasNotifiedHunger,
                    type = PetNotificationType.HUNGER,
                    petName = pet.name
                ) { prefs.hasNotifiedHunger = true }
            }

            if (simulated.hygiene <= PetStatsCalculator.HYGIENE_THRESHOLD) {
                trySendCare(
                    prefs = prefs,
                    enabled = prefs.isHygieneEnabled,
                    alreadyNotified = prefs.hasNotifiedHygiene,
                    type = PetNotificationType.HYGIENE,
                    petName = pet.name
                ) { prefs.hasNotifiedHygiene = true }
            }

            if (simulated.energy <= PetStatsCalculator.ENERGY_THRESHOLD && !simulated.isSleeping) {
                trySendCare(
                    prefs = prefs,
                    enabled = prefs.isEnergyEnabled,
                    alreadyNotified = prefs.hasNotifiedEnergy,
                    type = PetNotificationType.ENERGY,
                    petName = pet.name
                ) { prefs.hasNotifiedEnergy = true }
            }

            if (simulated.wasLonging) {
                val longingNow = System.currentTimeMillis()
                val elapsedSinceLastLonging = longingNow - prefs.lastLongingNotificationTimestamp
                if (elapsedSinceLastLonging < 6 * 60 * 60 * 1000L) {
                    Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=LONGING_COOLDOWN type=LONGING")
                } else {
                    trySendCare(
                        prefs = prefs,
                        enabled = prefs.isLongingEnabled,
                        alreadyNotified = false,
                        type = PetNotificationType.LONGING,
                        petName = pet.name
                    ) {
                        prefs.lastLongingNotificationTimestamp = longingNow
                    }
                }
            }

            PetCareScheduler.scheduleNextCheck(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    private fun trySendCare(
        prefs: NotificationPreferences,
        enabled: Boolean,
        alreadyNotified: Boolean,
        type: PetNotificationType,
        petName: String,
        onSent: () -> Unit
    ) {
        when {
            !enabled ->
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=TYPE_DISABLED type=${type.name}")
            alreadyNotified ->
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=ALREADY_NOTIFIED type=${type.name}")
            !prefs.canSendCareNotificationToday() ->
                Log.i("PET_NOTIFICATION_BLOCKED", "blocked reason=DAILY_LIMIT type=${type.name}")
            else -> {
                NotificationHelper.sendPetNotification(applicationContext, type, petName)
                onSent()
                prefs.incrementDailyNotificationCount()
            }
        }
    }
}
