# HealthConnectHaMqtt

Minimal Android/Kotlin app for publishing Health Connect data to Home Assistant over MQTT.

Current MVP:

- Reads **today's steps** from Health Connect using `StepsRecord.COUNT_TOTAL` aggregation.
- Publishes to MQTT with retained messages.
- Publishes Home Assistant MQTT Discovery config automatically.
- Provides manual "Sync now" and scheduled WorkManager sync.

## Architecture

```text
Samsung Health / Galaxy Watch / other app
        -> Health Connect on Android phone
        -> this Android app
        -> MQTT broker, for example Mosquitto add-on in HAOS
        -> Home Assistant MQTT sensor
```

## Build

Open this directory in Android Studio and run the `app` configuration on your phone.

Requirements:

- Android Studio with Android Gradle Plugin 8.x support.
- Android device with Health Connect available.
- Home Assistant MQTT broker reachable from the phone.

The project intentionally does not include a Gradle wrapper binary. Android Studio can use its installed Gradle, or you can generate a wrapper locally.

## Phone setup

1. Make sure Samsung Health writes steps to Health Connect.
2. On Android 14+: open Settings and search for Health Connect.
3. On Android 13 or lower: install Health Connect from Google Play if needed.
4. Install this app.
5. Enter MQTT settings.
6. Tap **Save**.
7. Tap **Grant permission** and allow reading steps.
8. Tap **Sync now**.
9. Tap **Schedule** for periodic sync.

## MQTT settings

Example for HAOS Mosquitto add-on:

- Broker URI: `tcp://192.168.1.10:1883`
- Username: your MQTT user
- Password: your MQTT password
- Base topic: `home/health/pavel`
- Device name: `Pavel Phone Health`

Published topics:

```text
home/health/pavel/availability = online
home/health/pavel/steps_today/state = 8432
home/health/pavel/steps_today/last_sync = 2026-06-20T12:00:00Z
homeassistant/sensor/health_mqtt_home_health_pavel_steps_today/config = { ... discovery json ... }
```

The app publishes Home Assistant MQTT Discovery config automatically. If MQTT Discovery is enabled in Home Assistant, a sensor should appear automatically.

## Manual Home Assistant YAML alternative

If you do not use MQTT Discovery, add this to `configuration.yaml`:

```yaml
mqtt:
  sensor:
    - name: "Pavel Phone steps today"
      unique_id: pavel_phone_steps_today
      state_topic: "home/health/pavel/steps_today/state"
      availability_topic: "home/health/pavel/availability"
      unit_of_measurement: "steps"
      state_class: total_increasing
      icon: mdi:walk
```

Then restart Home Assistant.

## Important limitations

- The MVP only reads steps. Add more Health Connect record types later: sleep, heart rate, weight, etc.
- Background Health Connect reads may require an additional user permission on newer Health Connect versions. If scheduled sync does not work, open the app and use **Sync now**, then extend the app to request background-read permission for your Android/Health Connect version.
- Do not expose your MQTT broker publicly without TLS and strong authentication.
- For WAN access, prefer VPN/Tailscale/WireGuard or HA Cloud/Nabu Casa routing rather than opening MQTT directly to the internet.

## Next metrics to add

Good next additions:

- Weight: `WeightRecord`
- Heart rate: `HeartRateRecord`
- Sleep: `SleepSessionRecord`
- Distance: `DistanceRecord`
- Active calories: `ActiveCaloriesBurnedRecord`

Each metric should get a stable MQTT topic and its own Home Assistant MQTT Discovery config.
