# How to build the APK

## Option 1: Android Studio

1. Install Android Studio.
2. Open this folder as a project.
3. Wait for Gradle Sync to finish. Android Studio/Gradle will download dependencies from Google Maven and Maven Central.
4. Connect your Android phone with USB debugging enabled.
5. Press Run, or choose Build > Build Bundle(s) / APK(s) > Build APK(s).
6. The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Option 2: GitHub Actions

1. Create a new GitHub repository.
2. Upload this project to it.
3. Open Actions > Build debug APK > Run workflow.
4. Download the artifact named `HealthConnectHaMqtt-debug-apk`.

This produces a debug APK suitable for sideloading/testing.

## Notes

- The project does not include a prebuilt APK.
- The build needs internet access to download Gradle, Android Gradle Plugin, Health Connect, Compose, WorkManager and MQTT dependencies.
- A release APK should be signed with your own Android signing key. The debug APK is only for testing.
