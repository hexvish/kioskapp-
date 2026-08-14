# 📱 Node Kiosk Android

> A high-reliability, native Android WebView shell for dedicated kiosk environments. Runs local or remote web applications securely in full-screen Lock Task mode.

---

## 🌟 Overview

**Node Kiosk Android** transforms Android tablets and devices into secure, single-purpose dedicated kiosk hardware. Designed for high availability, it embeds a custom Kotlin WebView client that automatically connects to your web application, retries connection drops, auto-launches on tablet boot, and restricts user escape attempts.

---

## ✨ Key Features

- 🔒 **Device Owner Lock Task Mode**: Restricts system hardware controls including Android Home button, Overview/Recents, Notification Shade, Status Bar, and System Gestures (`LOCK_TASK_FEATURE_NONE`).
- 🔄 **Auto-Reconnect & Offline Resilience**: Displays a clean connection retry screen if the web server drops, retrying automatically every 15 seconds.
- ⚙️ **Protected Admin Menu**: Triggered via a hidden 5-tap gesture in the bottom-right corner, secured with SHA-256 password hashing.
- ⚡ **Boot Persistence**: Automatically launches into kiosk mode immediately upon device startup (`BOOT_COMPLETED`).
- 🤖 **Automated ADB Deployment**: Includes a zero-friction Python CLI deployment script (`deploy_kiosk.py`) to build, install, provision Device Owner rights, and launch tablets over ADB.
- 🌐 **Dynamic Server Configuration**: Change and save target server URLs directly from the protected Admin Menu without rebuilding the app.

---

## 🛠️ Project Structure

```
kioskapp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/nodekiosk/
│   │   │   ├── MainActivity.kt               # Core WebView shell & 5-tap admin trigger
│   │   │   ├── AdminMenuActivity.kt          # Protected admin panel
│   │   │   ├── KioskDeviceManager.kt         # Lock Task & Device Owner policy manager
│   │   │   ├── KioskDeviceAdminReceiver.kt   # Device Admin receiver
│   │   │   ├── KioskConfig.kt                # SharedPreferences URL config & validation
│   │   │   └── BootReceiver.kt               # Boot-time auto-start broadcast receiver
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   └── build.gradle.kts
├── deploy_kiosk.py                           # Automated ADB deployment & provisioning tool
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

- **JDK 17** or higher
- **Android SDK** (API Level 35 compile / API Level 26 min)
- Set `ANDROID_HOME` environment variable (e.g. `%LOCALAPPDATA%\Android\Sdk`)

---

## 🔨 Building the App

Run the Gradle wrapper to build the debug APK:

### Windows (PowerShell)
```powershell
.\gradlew.bat assembleDebug
```

The output APK will be placed at:
`app\build\outputs\apk\debug\app-debug.apk`

---

## 📲 Provisioning & Deployment

Device Owner provisioning requires a freshly prepared / factory-reset Android device with no user accounts added.

### Option 1: Automated Deployment Script (Recommended)

Connect your tablet via USB with USB Debugging enabled, then run:

```powershell
python deploy_kiosk.py --all --provision-device-owner
```

### Option 2: Manual ADB Commands

```powershell
# Install APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Provision Device Owner rights
adb shell dpm set-device-owner com.example.nodekiosk/.KioskDeviceAdminReceiver

# Force stop and launch kiosk app
adb shell am force-stop com.example.nodekiosk
adb shell monkey -p com.example.nodekiosk 1
```

---

## 🔐 Administrator Configuration

### 1. Default Server URL
Configure your default web server URL in `KioskConfig.kt`:
```kotlin
object KioskConfig {
    const val DEFAULT_SERVER_URL = "http://192.168.1.5:8000/"
    ...
}
```
*Note: The server URL can also be changed anytime from the tablet's Admin Menu.*

### 2. Administrator Password (SHA-256)
The app stores only a SHA-256 digest of the admin password. To update the admin password prior to distribution:

1. Compute your SHA-256 hash in PowerShell:
   ```powershell
   $p = 'YourNewPassword'
   -join ([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($p)) | ForEach-Object { $_.ToString('x2') })
   ```
2. Update `ADMIN_PASSWORD_SHA256` in `MainActivity.kt`.

---

## 🛡️ License & Maintenance

Built for high-reliability kiosk deployments. Maintains dedicated device state and automatic server recovery without manual intervention.
