package eu.apava.healthmqtt

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectReader(
    private val context: Context
) {
    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    fun isAvailable(): Boolean {
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readStepsToday(): Long {
        val zone = ZoneId.systemDefault()

        val start: Instant = LocalDate.now()
            .atStartOfDay(zone)
            .toInstant()

        val end: Instant = Instant.now()

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
