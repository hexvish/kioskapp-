# Node Kiosk Android

Native Android WebView shell for a local Node.js website. It does not modify or bundle the website.

## Build

Set `ANDROID_HOME` to your SDK folder (normally `%LOCALAPPDATA%\\Android\\Sdk`) and run `gradlew.bat assembleDebug`.

## Device Owner test provisioning

Device Owner provisioning only works on a factory-reset / freshly prepared device with no accounts or existing Device Owner. Install, then run:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell dpm set-device-owner com.example.nodekiosk/.KioskDeviceAdminReceiver
adb shell am force-stop com.example.nodekiosk
adb shell monkey -p com.example.nodekiosk 1
```

Change `KioskConfig.DEFAULT_SERVER_URL` for a new build, or use the protected Admin Menu URL editor.

## Administrator password

The source stores only a SHA-256 digest. Before deployment, replace `ADMIN_PASSWORD_SHA256` with the SHA-256 digest of an administrator-chosen password. On PowerShell:

```powershell
$p = 'your-new-password'; -join ([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($p)) | ForEach-Object { $_.ToString('x2') })
```

The pre-build test password is `12345`, supplied here for testing only and not stored in source. Replace the digest before deployment.

## Notes

Lock Task is an official dedicated-device feature and requires Device Owner provisioning. Without it the app remains fullscreen but cannot secure Home/Recents. Reboot is available only where the device supports the Device Owner API. Android has no general public Device Owner shutdown API.

When provisioned as Device Owner, the kiosk applies `LOCK_TASK_FEATURE_NONE`: Android Home, Overview/Recents, notification shade, system bars, global actions, and gesture navigation are unavailable to the kiosk user. The software keyboard remains available when a focused web field requests it.
