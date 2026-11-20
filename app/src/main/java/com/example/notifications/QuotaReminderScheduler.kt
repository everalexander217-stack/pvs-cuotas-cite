package com.example.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.InstallmentEntity
import com.example.ui.theme.AppLanguage
import java.util.concurrent.TimeUnit

interface QuotaReminderCoordinator {
    fun syncSchedule(enabled: Boolean, thresholdDays: Int, triggerImmediateCheck: Boolean = false)
    fun checkNow(force: Boolean = false)
    fun sendTestReminder(
        installments: List<InstallmentEntity>,
        thresholdDays: Int,
        language: AppLanguage
    ): Boolean
}

class WorkManagerReminderCoordinator(
    context: Context
) : QuotaReminderCoordinator {

    private val appContext = context.applicationContext

    override fun syncSchedule(
        enabled: Boolean,
        thresholdDays: Int,
        triggerImmediateCheck: Boolean
    ) {
        val workManager = runCatching { WorkManager.getInstance(appContext) }.getOrNull() ?: return
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            QuotaNotificationHelper.cancelAll(appContext)
            return
        }

        val periodicRequest = PeriodicWorkRequestBuilder<QuotaReminderWorker>(
            12,
            TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        if (triggerImmediateCheck) {
            checkNow(force = false)
        }
    }

    override fun checkNow(force: Boolean) {
        val workManager = runCatching { WorkManager.getInstance(appContext) }.getOrNull() ?: return
        val oneTimeRequest = OneTimeWorkRequestBuilder<QuotaReminderWorker>()
            .setInputData(workDataOf(QuotaReminderWorker.KEY_FORCE_NOTIFY to force))
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    override fun sendTestReminder(
        installments: List<InstallmentEntity>,
        thresholdDays: Int,
        language: AppLanguage
    ): Boolean {
        return QuotaNotificationHelper.postSampleTestReminder(
            context = appContext,
            installments = installments,
            thresholdDays = thresholdDays,
            language = language
        )
    }

    companion object {
        const val PERIODIC_WORK_NAME = "pvs_quota_reminder_periodic"
        const val IMMEDIATE_WORK_NAME = "pvs_quota_reminder_immediate"
    }
}
