package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PetCareScheduler {

    const val UNIQUE_WORK_NAME = "pet_care_offline_worker"
    private const val TAG = "PET_NOTIFICATION_SCHEDULER"
    private const val TAG_SCHEDULE = "PET_NOTIFICATION_SCHEDULE"

    /**
     * Schedules an intelligent OneTimeWorkRequest with an initial delay calculated
     * from when the next critical pet attribute is estimated to reach its threshold.
     */
    fun scheduleNextCheck(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            var petId: Int? = null
            var delayMinutes: Long? = null
            var reason: String? = null
            try {
                val prefs = NotificationPreferences(appContext)
                if (!prefs.isNotificationsEnabled) {
                    cancelAllChecks(appContext)
                    Log.i(TAG_SCHEDULE, "blocked reason=MASTER_OFF")
                    return@launch
                }

                val db = AppDatabase.getDatabase(appContext)
                val pet = db.petDao().getPet()

                if (pet == null || !pet.isHatched) {
                    cancelAllChecks(appContext)
                    Log.i(TAG_SCHEDULE, "blocked reason=NO_HATCHED_PET")
                    return@launch
                }
                petId = pet.id

                val simulated = PetStatsCalculator.calculateSimulatedStats(pet)
                val healthState = PetHealthState.fromHealth(simulated.health)
                val disease = PetDisease.fromString(simulated.disease)
                val healthUrgentPending =
                    prefs.isHealthEnabled &&
                        !prefs.hasNotifiedHealth &&
                        (healthState == PetHealthState.DOENTE ||
                            healthState == PetHealthState.CRITICO ||
                            disease != PetDisease.NONE)

                val estimate = PetStatsCalculator.estimateMinutesUntilNextThreshold(
                    pet = pet,
                    allowCriticalHealthShortcut = healthUrgentPending
                )
                delayMinutes = estimate.delayMinutes
                reason = estimate.reason

                val workRequest = OneTimeWorkRequestBuilder<PetCareWorker>()
                    .setInitialDelay(estimate.delayMinutes, TimeUnit.MINUTES)
                    .addTag(UNIQUE_WORK_NAME)
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                Log.i(
                    TAG_SCHEDULE,
                    "scheduled type=${estimate.reason} delay=${estimate.delayMinutes}min petId=${pet.id} " +
                        "health=${simulated.health} disease=${simulated.disease} " +
                        "criticalShortcut=$healthUrgentPending"
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "scheduleNextCheck failed petId=$petId delay=$delayMinutes reason=$reason " +
                        "ex=${e.javaClass.simpleName}: ${e.message}",
                    e
                )
            }
        }
    }

    fun cancelAllChecks(context: Context) {
        try {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.i(TAG_SCHEDULE, "cancelled uniqueWork=$UNIQUE_WORK_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "cancelAllChecks failed: ${e.message}", e)
        }
    }

    fun triggerImmediateCheck(context: Context) {
        val appContext = context.applicationContext
        try {
            val workRequest = OneTimeWorkRequestBuilder<PetCareWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "${UNIQUE_WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.i(TAG_SCHEDULE, "scheduled type=IMMEDIATE delay=0min")
        } catch (e: Exception) {
            Log.e(TAG, "triggerImmediateCheck failed: ${e.message}", e)
        }
    }
}
