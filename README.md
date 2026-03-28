# LnxSense Android

An Android companion app for the [lnxsense](https://codeberg.org/PW999/lnxsense) hardware monitoring server. It connects to a running LnxSense server over the network and displays sensor data in real-time on your phone or tablet.
![Photo of an Android phone running the lnxsense senor panel app](image.jpg)
## Features

- **Live sensor browser** - Browse all sensor groups, devices and individual sensors with real-time value updates. Devices with a configured summary value show a collapsed summary (temperature, average, min/max).
- **Configurable dial gauges** - Select sensors and display them as animated speedometer-style gauges with needle, arc and color-coded status indicators (green/yellow/red). Each gauge shows the device name and sensor name below the dial. Already-assigned sensors are grayed out in the picker to avoid duplicates.
- **Flexible grid layout** - Configure the number of rows and columns for the dial panel (1-6 each). The grid automatically swaps rows and columns in landscape orientation. The dial panel runs full-screen without a top bar for maximum gauge space.
- **Theme support** - System (follows Android setting), Light, Dark, and AMOLED (pure black backgrounds for OLED screens).
- **Keep screen on** - The screen stays awake while the app is in the foreground, useful for desk/wall-mounted monitoring.
- **Auto-reconnect** - When the app returns from the background, it automatically reconnects to the server without navigating back to the connection screen.
- **Persistent configuration** - Connection settings, dial configurations, grid layout and theme choice are persisted across app restarts.

## Requirements

- Android 14 (API 34) or higher
- LnxSense server running with an INET socket accessible over the network

> **Why API 34?** The server sends data using Java's `ObjectOutputStream` with record types. Android 14 is the first version with native Java record support, which guarantees serialization compatibility with the server's Java 25 records. Older Android versions would require D8 desugaring of records into regular classes, producing a different `serialVersionUID` and breaking deserialization.

## Server configuration

The server must be configured to use an INET socket that listens on a network-accessible address. By default it binds to `127.0.0.1` (localhost only), which won't accept connections from other devices.

In `server/src/main/resources/application.yaml`, set:

```yaml
socket:
  socketType: INET
  socketAddress: "0.0.0.0:9999"
```

- `0.0.0.0` makes the server listen on all network interfaces
- `9999` is the default port (can be changed, just match it in the app)
- Make sure your firewall allows TCP traffic on this port
- The server and phone must be on the same network (or the port must be forwarded)

## Building

The Android module is a standalone Gradle project, separate from the Maven build used by the rest of LnxSense.

```bash
cd android
JAVA_HOME=/path/to/jdk17 ./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Build requirements

- JDK 17 (for Gradle and the Android toolchain)
- Android SDK with platform 35 and build-tools 35.0.0
- Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=/path/to/Android/Sdk`

## Usage

1. Start the LnxSense server with the INET socket configuration above
2. Install the APK on your Android device
3. Open the app and enter the server's IP address and port (default 9999)
4. Tap **Connect** - the sensor browser will show all discovered sensors with live values
5. Tap the **gear icon** to configure dial gauges - select sensors, set min/max ranges
6. Tap the **dashboard icon** to view the dial panel
7. Use the **three-dot menu** to switch between light, dark and AMOLED themes

## Architecture

- **Kotlin + Jetpack Compose** with Material 3 and dynamic colors
- **MVVM** with ViewModels, StateFlows and coroutines
- **No dependency injection** - the `SensorRepository` is a manual singleton since it's the only shared dependency
- **Java serialization** over a plain TCP socket - the API record types (`Device`, `Sensor`, `Reading`, `Group`) are copied from the server's API module and compiled at Java 17 to maintain binary serialization compatibility
- **`LenientObjectInputStream`** overrides `readClassDescriptor()` to handle `serialVersionUID` mismatches between the server's Java 25 bytecode and Android's Java 17 bytecode
- **Smooth gauge animations** using `animateFloatAsState` with `FastOutSlowInEasing` for natural needle movement at 200ms update intervals

## Vibe-coded

This entire Android application was vibe-coded in a single session with [Claude Code](https://docs.anthropic.com/en/docs/claude-code) (Claude Opus). From the initial Gradle scaffolding and API type copying, through the socket client, all four screens, the custom Canvas gauge composable, animations, theme support, and every bug fix along the way - all generated through conversational iteration. No Android code was written by hand.
