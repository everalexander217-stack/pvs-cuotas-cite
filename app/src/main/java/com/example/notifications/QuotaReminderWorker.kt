package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.DueDateUtils
import com.example.ui.theme.AppLanguage

class QuotaReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            QuotaNotificationHelper.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean(QuotaNotificationHelper.KEY_NOTIFICATIONS_ENABLED, true)
        if (!enabled) {
            QuotaNotificationHelper.cancelAll(applicationContext)
            return Result.success()
        }

        val thresholdDays = prefs.getInt(QuotaNotificationHelper.KEY_ALERT_THRESHOLD_DAYS, 3)
        val langCode = prefs.getString(QuotaNotificationHelper.KEY_APP_LANGUAGE, AppLanguage.ES.shortCode)
        val language = if (langCode == AppLanguage.EN.shortCode) AppLanguage.EN else AppLanguage.ES
        val force = inputData.getBoolean(KEY_FORCE_NOTIFY, false)

        val installments = AppDatabase.getInstance(applicationContext)
            .installmentDao()
            .getAll()

        val upcoming = DueDateUtils.computeUpcomingAlerts(
            installments = installments,
            thresholdDays = thresholdDays
        )

        if (upcoming.isNotEmpty()) {
            QuotaNotificationHelper.postUpcomingReminders(
                context = applicationContext,
                alerts = upcoming,
                language = language,
                force = force
            )
        } else {
            QuotaNotificationHelper.cancelAll(applicationContext)
        }

        return Result.success()
    }

    companion object {
        const val KEY_FORCE_NOTIFY = "force_notify"
    }
}
