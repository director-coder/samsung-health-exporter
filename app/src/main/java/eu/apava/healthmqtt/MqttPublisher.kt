package eu.apava.healthmqtt

import android.content.Context
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class MqttPublisher(private val context: Context) {
    private val settings = SettingsStore(context)

    fun publishSteps(steps: Long) {
        val clientId = "health-mqtt-" + UUID.randomUUID().toString().take(8)
        val client = MqttClient(settings.brokerUri, clientId, MemoryPersistence())
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = false
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
            if (settings.username.isNotBlank()) {
                userName = settings.username
                password = settings.password.toCharArray()
            }
        }

        client.connect(options)
        try {
            val base = settings.baseTopic
            publish(client, "$base/availability", "online", retained = true)
            publishDiscovery(client)
            publish(client, "$base/steps_today/state", steps.toString(), retained = true)
            publish(client, "$base/steps_today/last_sync", Instant.now().toString(), retained = true)
        } finally {
            client.disconnect(1000)
            client.close()
        }
    }

    private fun publishDiscovery(client: MqttClient) {
        val base = settings.baseTopic
        val deviceId = "health_mqtt_${base.replace('/', '_')}"
        val configTopic = "homeassistant/sensor/${deviceId}_steps_today/config"
        val payload = """
            {
              "name": "${escapeJson(settings.deviceName)} steps today",
              "unique_id": "${deviceId}_steps_today",
              "state_topic": "$base/steps_today/state",
              "availability_topic": "$base/availability",
              "unit_of_measurement": "steps",
              "state_class": "total_increasing",
              "icon": "mdi:walk",
              "device": {
                "identifiers": ["$deviceId"],
                "name": "${escapeJson(settings.deviceName)}",
                "manufacturer": "APAVA",
                "model": "Health Connect MQTT bridge"
              }
            }
        """.trimIndent()
        publish(client, configTopic, payload, retained = true)
    }

    private fun publish(client: MqttClient, topic: String, payload: String, retained: Boolean) {
        val message = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = retained
        }
        client.publish(topic, message)
    }

    private fun escapeJson(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
