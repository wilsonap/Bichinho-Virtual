package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "bichinho_notification_prefs"

        // Settings keys
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_HUNGER_ENABLED = "key_hunger_enabled"
        private const val KEY_HYGIENE_ENABLED = "key_hygiene_enabled"
        private const val KEY_ENERGY_ENABLED = "key_energy_enabled"
        private const val KEY_HEALTH_ENABLED = "key_health_enabled"
        private const val KEY_LONGING_ENABLED = "key_longing_enabled"

        // Anti-spam state keys
        private const val KEY_NOTIFIED_HUNGER = "key_notified_hunger"
        private const val KEY_NOTIFIED_HYGIENE = "key_notified_hygiene"
        private const val KEY_NOTIFIED_ENERGY = "key_notified_energy"
        private const val KEY_NOTIFIED_HEALTH = "key_notified_health"
        private const val KEY_LAST_LONGING_TIMESTAMP = "key_last_longing_timestamp"

        // Daily rate limiting keys (Max 3 care notifications per day)
        private const val KEY_DAILY_NOTIFICATION_COUNT = "key_daily_notification_count"
        private const val KEY_LAST_NOTIFICATION_DATE = "key_last_notification_date"

        const val MAX_DAILY_CARE_NOTIFICATIONS = 3
    }

    // User settings
    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var isHungerEnabled: Boolean
        get() = prefs.getBoolean(KEY_HUNGER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HUNGER_ENABLED, value).apply()

    var isHygieneEnabled: Boolean
        get() = prefs.getBoolean(KEY_HYGIENE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HYGIENE_ENABLED, value).apply()

    var isEnergyEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENERGY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENERGY_ENABLED, value).apply()

    var isHealthEnabled: Boolean
        get() = prefs.getBoolean(KEY_HEALTH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HEALTH_ENABLED, value).apply()

    var isLongingEnabled: Boolean
        get() = prefs.getBoolean(KEY_LONGING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LONGING_ENABLED, value).apply()

    // Anti-Spam state tracking
    var hasNotifiedHunger: Boolean
        get() = prefs.getBoolean(KEY_NOTIFIED_HUNGER, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFIED_HUNGER, value).apply()

    var hasNotifiedHygiene: Boolean
        get() = prefs.getBoolean(KEY_NOTIFIED_HYGIENE, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFIED_HYGIENE, value).apply()

    var hasNotifiedEnergy: Boolean
        get() = prefs.getBoolean(KEY_NOTIFIED_ENERGY, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFIED_ENERGY, value).apply()

    var hasNotifiedHealth: Boolean
        get() = prefs.getBoolean(KEY_NOTIFIED_HEALTH, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFIED_HEALTH, value).apply()

    var lastLongingNotificationTimestamp: Long
        get() = prefs.getLong(KEY_LAST_LONGING_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LONGING_TIMESTAMP, value).apply()

    /**
     * Daily notification counter management
     */
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun canSendCareNotificationToday(): Boolean {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "") ?: ""
        if (today != lastDate) {
            // New day: Reset counter
            prefs.edit()
                .putString(KEY_LAST_NOTIFICATION_DATE, today)
                .putInt(KEY_DAILY_NOTIFICATION_COUNT, 0)
                .apply()
            return true
        }
        val currentCount = prefs.getInt(KEY_DAILY_NOTIFICATION_COUNT, 0)
        return currentCount < MAX_DAILY_CARE_NOTIFICATIONS
    }

    fun incrementDailyNotificationCount() {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "") ?: ""
        val currentCount = if (today == lastDate) {
            prefs.getInt(KEY_DAILY_NOTIFICATION_COUNT, 0)
        } else {
            0
        }
        prefs.edit()
            .putString(KEY_LAST_NOTIFICATION_DATE, today)
            .putInt(KEY_DAILY_NOTIFICATION_COUNT, currentCount + 1)
            .apply()
    }

    fun getDailyCount(): Int {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "") ?: ""
        return if (today == lastDate) prefs.getInt(KEY_DAILY_NOTIFICATION_COUNT, 0) else 0
    }

    // Resetting alert flags when player performs care actions
    fun onPetFed() {
        hasNotifiedHunger = false
    }

    fun onPetBathed() {
        hasNotifiedHygiene = false
    }

    fun onPetSlept() {
        hasNotifiedEnergy = false
    }

    fun onPetDoctorTreated() {
        hasNotifiedHealth = false
    }

    fun onAppOpened() {
        lastLongingNotificationTimestamp = System.currentTimeMillis()
    }

    fun resetAllAlerts() {
        hasNotifiedHunger = false
        hasNotifiedHygiene = false
        hasNotifiedEnergy = false
        hasNotifiedHealth = false
        lastLongingNotificationTimestamp = 0L
    }
}
