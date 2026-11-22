package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DueDateUtils
import com.example.data.InstallmentEntity
import com.example.data.PaymentAlert
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.SpanishStrings
import java.util.Calendar
import java.util.Locale

object QuotaNotificationHelper {
    const val PREFS_NAME = "pvs_cuotas_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_ALERT_THRESHOLD_DAYS = "alert_threshold_days"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_DARK_MODE = "dark_mode_enabled"
    const val KEY_COACH_MARK_COMPLETED = "coach_mark_completed"
    const val KEY_LAST_NOTIFIED_SIGNATURE = "last_notified_signature"

    const val CHANNEL_ID = "pvs_quota_reminders"
    const val NOTIFICATION_ID_PRIMARY = 1001

    const val EXTRA_FOCUS_INSTALLMENT_ID = "extra_focus_installment_id"
    const val EXTRA_OPEN_NOTIFICATIONS_SHEET = "extra_open_notifications_sheet"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canPostSystemNotifications(context: Context): Boolean {
        return hasNotificationPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun ensureChannel(context: Context, language: AppLanguage = AppLanguage.ES) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val strings = if (language == AppLanguage.ES) SpanishStrings else EnglishStrings
            val channel = NotificationChannel(
                CHANNEL_ID,
                strings.notifChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = strings.notifChannelDesc
                enableLights(true)
                lightColor = 0xFF38B261.toInt()
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun postUpcomingReminders(
        context: Context,
        alerts: List<PaymentAlert>,
        language: AppLanguage,
        force: Boolean = false
    ): Boolean {
        if (alerts.isEmpty()) {
            cancelAll(context)
            return false
        }

        ensureChannel(context, language)
        if (!canPostSystemNotifications(context)) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val signature = buildDailySignature(alerts)
        val lastSignature = prefs.getString(KEY_LAST_NOTIFIED_SIGNATURE, null)
        if (!force && signature == lastSignature) {
            return false
        }

        val strings = if (language == AppLanguage.ES) SpanishStrings else EnglishStrings
        val primaryAlert = alerts.first()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (alerts.size == 1) {
                putExtra(EXTRA_FOCUS_INSTALLMENT_ID, primaryAlert.installment.id)
            } else {
                putExtra(EXTRA_OPEN_NOTIFICATIONS_SHEET, true)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PRIMARY,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.pvs_logo_official)
        }.getOrNull()

        val title: String
        val shortBody: String
        val style: NotificationCompat.Style

        if (alerts.size == 1) {
            val amount = formatAmount(primaryAlert.installment.biweeklyQuota)
            val urgency = strings.urgencyLabel(primaryAlert.daysRemaining)
            title = "${strings.notifUpcomingTitle} · ${primaryAlert.installment.code}"
            shortBody = "${primaryAlert.installment.title} · $amount ($urgency)"
            val expandedBody = "${primaryAlert.installment.title}\n$amount · $urgency (${primaryAlert.installment.nextPaymentDate})"
            style = NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(expandedBody)
        } else {
            val amount = formatAmount(primaryAlert.installment.biweeklyQuota)
            val urgency = strings.urgencyLabel(primaryAlert.daysRemaining)
            title = strings.notifSystemMultiTitle(alerts.size)
            shortBody = "${primaryAlert.installment.title}: $amount ($urgency)"
            val inbox = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText(strings.notifCenterTitle)
            alerts.take(5).forEach { alert ->
                val itemAmount = formatAmount(alert.installment.biweeklyQuota)
                val itemUrgency = strings.urgencyLabel(alert.daysRemaining)
                inbox.addLine("${alert.installment.code} · ${alert.installment.title} ($itemAmount · $itemUrgency)")
            }
            style = inbox
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pvs)
            .setColor(0xFF1B5E3A.toInt())
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(style)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                R.drawable.ic_notification_pvs,
                strings.notifActionReview,
                contentPendingIntent
            )

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PRIMARY, builder.build())
            prefs.edit().putString(KEY_LAST_NOTIFIED_SIGNATURE, signature).apply()
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun postSampleTestReminder(
        context: Context,
        installments: List<InstallmentEntity>,
        thresholdDays: Int,
        language: AppLanguage
    ): Boolean {
        val upcoming = DueDateUtils.computeUpcomingAlerts(installments, thresholdDays)
        val alertsToShow = if (upcoming.isNotEmpty()) {
            upcoming
        } else {
            val candidate = installments.firstOrNull { !it.isCompleted } ?: installments.firstOrNull()
            if (candidate != null) {
                listOf(
                    PaymentAlert(
                        installment = candidate.copy(
                            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(1)
                        ),
                        daysRemaining = 1
                    )
                )
            } else {
                listOf(
                    PaymentAlert(
                        installment = InstallmentEntity(
                            id = 999,
                            title = "Smart TV 50\" 4K UHD",
                            code = "TV-50",
                            category = "Tecnología",
                            initialPaid = 97.25,
                            totalAmount = 389.0,
                            biweeklyQuota = 48.63,
                            totalQuotas = 6,
                            paidQuotas = 4,
                            nextPaymentDate = DueDateUtils.formatDateOffsetFromToday(1),
                            sortOrder = 0
                        ),
                        daysRemaining = 1
                    )
                )
            }
        }
        return postUpcomingReminders(
            context = context,
            alerts = alertsToShow,
            language = language,
            force = true
        )
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PRIMARY)
    }

    private fun buildDailySignature(alerts: List<PaymentAlert>): String {
        val cal = Calendar.getInstance()
        val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        val alertKey = alerts.joinToString(",") {
            "${it.installment.id}_${it.daysRemaining}_${it.installment.paidQuotas}"
        }
        return "$dayKey:$alertKey"
    }

    private fun formatAmount(amount: Double): String = String.format(Locale.US, "$%.2f", amount)
}
