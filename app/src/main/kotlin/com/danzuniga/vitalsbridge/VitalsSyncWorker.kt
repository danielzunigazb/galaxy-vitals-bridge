package com.danzuniga.vitalsbridge

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import java.util.concurrent.TimeUnit

class VitalsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = TokenStore(applicationContext).githubToken
            ?: return Result.failure() // nothing to publish with

        val samsungHealth = SamsungHealthRepository(applicationContext)
        if (!samsungHealth.hasPermission()) {
            return Result.failure()
        }

        val snapshot = samsungHealth.readVitalsSnapshot()
        val outcome = GitHubPublisher(token).publish(snapshot)
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "vitals_sync"

        /** WorkManager enforces a 15-minute minimum for periodic work. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<VitalsSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
