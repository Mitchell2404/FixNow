package com.fixnow.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SolicitudSyncScheduler {
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SolicitudSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag("solicitud_sync")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "solicitud_sync",
                ExistingWorkPolicy.KEEP,
                request
            )
    }
}
