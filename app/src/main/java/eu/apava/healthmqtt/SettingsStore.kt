package eu.apava.healthmqtt

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var brokerUri: String
        get() = prefs.getString("brokerUri", "tcp://192.168.1.10:1883") ?: "tcp://192.168.1.10:1883"
        set(value) = prefs.edit().putString("brokerUri", value.trim()).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value.trim()).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    var baseTopic: String
        get() = prefs.getString("baseTopic", "home/health/pavel") ?: "home/health/pavel"
        set(value) = prefs.edit().putString("baseTopic", value.trim().trim('/')).apply()

    var deviceName: String
        get() = prefs.getString("deviceName", "Pavel Phone Health") ?: "Pavel Phone Health"
        set(value) = prefs.edit().putString("deviceName", value.trim()).apply()

    var syncEveryHours: Long
        get() = prefs.getLong("syncEveryHours", 1L).coerceAtLeast(1L)
        set(value) = prefs.edit().putLong("syncEveryHours", value.coerceAtLeast(1L)).apply()
}
