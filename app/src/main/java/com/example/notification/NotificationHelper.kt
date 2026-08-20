package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

enum class PetNotificationType(val extraValue: String) {
    HUNGER("HUNGER"),
    HYGIENE("HYGIENE"),
    ENERGY("ENERGY"),
    HEALTH("HEALTH"),
    LONGING("LONGING"),
    TEST("TEST")
}

object NotificationHelper {

    const val EXTRA_NOTIFICATION_TYPE = "notification_type"

    const val CHANNEL_CARE_ID = "pet_care_channel"
    const val CHANNEL_CARE_NAME = "Cuidados do Bichinho"
    const val CHANNEL_CARE_DESC = "Notificações sobre fome, banho, sono e saudade do seu bichinho."

    const val CHANNEL_HEALTH_ID = "pet_health_channel"
    const val CHANNEL_HEALTH_NAME = "Saúde do Bichinho"
    const val CHANNEL_HEALTH_DESC = "Alertas importantes quando o bichinho estiver doente ou com saúde crítica."

    const val CHANNEL_SILENT_ID = "pet_silent_channel"
    const val CHANNEL_SILENT_NAME = "Notificações Silenciosas"
    const val CHANNEL_SILENT_DESC = "Avisos discretos enviados durante o horário silencioso."

    // Notification IDs
    const val NOTIF_ID_HUNGER = 1001
    const val NOTIF_ID_HYGIENE = 1002
    const val NOTIF_ID_ENERGY = 1003
    const val NOTIF_ID_HEALTH = 1004
    const val NOTIF_ID_LONGING = 1005
    const val NOTIF_ID_TEST = 1006

    /**
     * Creates notification channels if Android O+ (API 26+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Care Channel (Default importance)
            val careChannel = NotificationChannel(
                CHANNEL_CARE_ID,
                CHANNEL_CARE_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_CARE_DESC
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // 2. Health Channel (High importance)
            val healthChannel = NotificationChannel(
                CHANNEL_HEALTH_ID,
                CHANNEL_HEALTH_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_HEALTH_DESC
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // 3. Silent Channel (Low importance for quiet hours)
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT_ID,
                CHANNEL_SILENT_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_SILENT_DESC
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(careChannel, healthChannel, silentChannel))
        }
    }

    /**
     * Quiet Hours alinhados ao sono noturno: 22:00 → 07:30.
     */
    fun isQuietHours(): Boolean {
        return PetStatsCalculator.isNightTime()
    }

    /**
     * Checks if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends a localized pet notification with dynamic name and contextual action intent.
     */
    fun sendPetNotification(
        context: Context,
        type: PetNotificationType,
        petName: String,
        customTitle: String? = null,
        customMessage: String? = null
    ) {
        val effectiveName = petName.ifBlank { "Seu bichinho" }

        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            android.util.Log.i(
                "PET_NOTIFICATION_BLOCKED",
                "blocked reason=PERMISSION_DENIED type=${type.name}"
            )
            return
        }

        val quietHours = isQuietHours()

        val (defTitle, defMessage, notificationId, defaultChannelId, priority) = when (type) {
            PetNotificationType.HUNGER -> {
                Tuple5(
                    "Hora do Lanchinho! 🍎",
                    "🍎 $effectiveName está com fome! Que tal dar alguma coisa para ele comer?",
                    NOTIF_ID_HUNGER,
                    CHANNEL_CARE_ID,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            PetNotificationType.HYGIENE -> {
                Tuple5(
                    "Hora do Banho! 🛁",
                    "🛁 $effectiveName está precisando de um banho.",
                    NOTIF_ID_HYGIENE,
                    CHANNEL_CARE_ID,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            PetNotificationType.ENERGY -> {
                Tuple5(
                    "Que Soninho... 😴",
                    "😴 $effectiveName está ficando com sono.",
                    NOTIF_ID_ENERGY,
                    CHANNEL_CARE_ID,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            PetNotificationType.HEALTH -> {
                Tuple5(
                    "Atenção com a Saúde! 🤒",
                    "🤒 $effectiveName não está se sentindo bem. Abra o jogo para cuidar dele.",
                    NOTIF_ID_HEALTH,
                    CHANNEL_HEALTH_ID,
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            PetNotificationType.LONGING -> {
                Tuple5(
                    "Que Saudade de Você! ❤️",
                    "❤️ $effectiveName está sentindo sua falta.",
                    NOTIF_ID_LONGING,
                    CHANNEL_CARE_ID,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            PetNotificationType.TEST -> {
                Tuple5(
                    "Notificação de Teste 🔔",
                    "As notificações do $effectiveName estão funcionando perfeitamente!",
                    NOTIF_ID_TEST,
                    CHANNEL_CARE_ID,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
        }

        val title = customTitle ?: defTitle
        val message = customMessage ?: defMessage

        // Quiet hours: Override channel to silent, disable sound/vibration
        // (Worker already blocks night for care checks; this covers TEST / edge calls.)
        val channelToUse = if (quietHours) CHANNEL_SILENT_ID else defaultChannelId
        val effectivePriority = if (quietHours) NotificationCompat.PRIORITY_LOW else priority
        if (quietHours) {
            android.util.Log.i(
                "PET_NOTIFICATION_BLOCKED",
                "blocked reason=QUIET_HOURS_SILENT_CHANNEL type=${type.name} (still posted silent)"
            )
        }

        // PendingIntent to launch MainActivity with notification extra
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type.extraValue)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(effectivePriority)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (quietHours) {
            builder.setSilent(true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            android.util.Log.i(
                "PET_NOTIFICATION_SENT",
                "sent type=${type.name} channel=$channelToUse quietHours=$quietHours name=$effectiveName"
            )
        } catch (e: SecurityException) {
            android.util.Log.i(
                "PET_NOTIFICATION_BLOCKED",
                "blocked reason=PERMISSION_DENIED type=${type.name} ex=${e.message}"
            )
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E
    )
}
