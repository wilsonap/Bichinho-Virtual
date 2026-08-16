package com.example.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object PetCareScheduler {

    const val UNIQUE_WORK_NAME = "pet_care_offline_worker"

    /**
     * Schedules an intelligent OneTimeWorkRequest with an initial delay calculated
     * from when the next critical pet attribute (Hunger, Hygiene, Energy, Longing)
     * is estimated to reach its threshold.
     */
    fun scheduleNextCheck(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = NotificationPreferences(appContext)
                if (!prefs.isNotificationsEnabled) {
                    cancelAllChecks(appContext)
                    return@launch
                }

                val db = AppDatabase.getDatabase(appContext)
                val pet = db.petDao().getPet()

                if (pet == null || !pet.isHatched) {
                    cancelAllChecks(appContext)
                    return@launch
                }

                val delayMinutes = PetStatsCalculator.estimateMinutesUntilNextThreshold(pet)

                val workRequest = OneTimeWorkRequestBuilder<PetCareWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .addTag(UNIQUE_WORK_NAME)
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } catch (_: Exception) {
                // Ignore background scheduling exceptions
            }
        }
    }

    /**
     * Cancels any scheduled background pet care work.
     */
    fun cancelAllChecks(context: Context) {
        try {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        } catch (_: Exception) {
        }
    }

    /**
     * Immediately evaluates and sends any pending notifications (useful for debug and manual test actions).
     */
    fun triggerImmediateCheck(context: Context) {
        val appContext = context.applicationContext
        val workRequest = OneTimeWorkRequestBuilder<PetCareWorker>()
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "${UNIQUE_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
