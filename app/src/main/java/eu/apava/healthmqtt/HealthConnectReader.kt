package eu.apava.healthmqtt

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectReader(
    private val context: Context
) {
    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
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
