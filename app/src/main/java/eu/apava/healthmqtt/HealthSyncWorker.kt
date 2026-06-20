package eu.apava.healthmqtt

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class HealthSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val reader = HealthConnectReader(applicationContext)
            if (!reader.isAvailable()) return Result.retry()
            if (!reader.hasAllPermissions()) return Result.failure()
            val steps = reader.readStepsToday()
            MqttPublisher(applicationContext).publishSteps(steps)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "health_connect_mqtt_sync"

        fun schedule(context: Context, everyHours: Long) {
            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(everyHours.coerceAtLeast(1L), TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
