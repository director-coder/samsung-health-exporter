package eu.apava.healthmqtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScreen() }
    }
}

@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { SettingsStore(context) }
    val reader = remember { HealthConnectReader(context) }
    val scope = rememberCoroutineScope()

    var brokerUri by remember { mutableStateOf(settings.brokerUri) }
    var username by remember { mutableStateOf(settings.username) }
    var password by remember { mutableStateOf(settings.password) }
    var baseTopic by remember { mutableStateOf(settings.baseTopic) }
    var deviceName by remember { mutableStateOf(settings.deviceName) }
    var syncEveryHours by remember { mutableStateOf(settings.syncEveryHours.toString()) }
    var status by remember { mutableStateOf("Ready") }
    var lastSteps by remember { mutableStateOf<Long?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        status = if (granted.containsAll(reader.permissions)) {
            "Health Connect permission granted"
        } else {
            "Health Connect permission was not granted"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Health Connect → MQTT", style = MaterialTheme.typography.headlineSmall)
                Text("Publishes today's step count from Health Connect to Home Assistant via MQTT discovery.")

                OutlinedTextField(
                    value = brokerUri,
                    onValueChange = { brokerUri = it },
                    label = { Text("MQTT broker URI") },
                    placeholder = { Text("tcp://192.168.1.10:1883 or ssl://host:8883") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("MQTT username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("MQTT password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseTopic,
                    onValueChange = { baseTopic = it },
                    label = { Text("Base topic") },
                    placeholder = { Text("home/health/pavel") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device name in Home Assistant") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = syncEveryHours,
                    onValueChange = { syncEveryHours = it.filter { c -> c.isDigit() } },
                    label = { Text("Sync interval, hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        save(settings, brokerUri, username, password, baseTopic, deviceName, syncEveryHours)
                        status = "Settings saved"
                    }) { Text("Save") }

                    Button(onClick = {
                        if (!reader.isAvailable()) {
                            status = "Health Connect is not available on this phone"
                        } else {
                            permissionsLauncher.launch(reader.permissions)
                        }
                    }) { Text("Grant permission") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        save(settings, brokerUri, username, password, baseTopic, deviceName, syncEveryHours)
                        scope.launch {
                            status = "Syncing..."
                            try {
                                val steps = reader.readStepsToday()
                                withContext(Dispatchers.IO) { MqttPublisher(context).publishSteps(steps) }
                                lastSteps = steps
                                status = "Published $steps steps to MQTT"
                            } catch (e: Exception) {
                                status = "Sync failed: ${e.message ?: e::class.java.simpleName}"
                            }
                        }
                    }) { Text("Sync now") }

                    Button(onClick = {
                        save(settings, brokerUri, username, password, baseTopic, deviceName, syncEveryHours)
                        HealthSyncWorker.schedule(context, settings.syncEveryHours)
                        status = "Periodic sync scheduled every ${settings.syncEveryHours} hour(s)"
                    }) { Text("Schedule") }
                }

                Spacer(Modifier.height(8.dp))
                Text("Status: $status")
                Text("Last steps: ${lastSteps?.toString() ?: "-"}")
                Text("MQTT state topic: ${baseTopic.trim().trim('/')}/steps_today/state")
                Text("MQTT discovery topic: homeassistant/sensor/health_mqtt_${baseTopic.trim().trim('/').replace('/', '_')}_steps_today/config")
            }
        }
    }
}

private fun save(
    settings: SettingsStore,
    brokerUri: String,
    username: String,
    password: String,
    baseTopic: String,
    deviceName: String,
    syncEveryHours: String
) {
    settings.brokerUri = brokerUri
    settings.username = username
    settings.password = password
    settings.baseTopic = baseTopic
    settings.deviceName = deviceName
    settings.syncEveryHours = syncEveryHours.toLongOrNull() ?: 1L
}
