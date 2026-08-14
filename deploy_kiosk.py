#!/usr/bin/env python3
"""Install and provision Node Kiosk tablets through ADB.

Device Owner provisioning changes device-management ownership. Android permits it
only on a clean tablet with no existing accounts or Device Owner.
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

PACKAGE = "com.example.nodekiosk"
ADMIN = f"{PACKAGE}/.KioskDeviceAdminReceiver"
ROOT = Path(__file__).resolve().parent
DEFAULT_APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"


def find_adb() -> str:
    if command := shutil.which("adb"):
        return command
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    candidates = [Path(sdk) / "platform-tools" / "adb.exe"] if sdk else []
    local = os.environ.get("LOCALAPPDATA")
    if local:
        candidates.append(Path(local) / "Android" / "Sdk" / "platform-tools" / "adb.exe")
    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    raise FileNotFoundError("ADB was not found. Install Android platform-tools or set ANDROID_HOME.")


def run(adb: str, serial: str, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    command = [adb, "-s", serial, *args]
    result = subprocess.run(command, text=True, capture_output=True)
    if check and result.returncode:
        raise RuntimeError((result.stdout + result.stderr).strip())
    return result


def connected_devices(adb: str) -> list[str]:
    output = subprocess.run([adb, "devices"], text=True, capture_output=True, check=True).stdout.splitlines()
    return [line.split()[0] for line in output[1:] if len(line.split()) >= 2 and line.split()[1] == "device"]


def provision(adb: str, serial: str) -> bool:
    owners = run(adb, serial, "shell", "dpm", "list-owners", check=False)
    owner_text = owners.stdout + owners.stderr
    if PACKAGE in owner_text and "DeviceOwner" in owner_text:
        print("  Already Device Owner.")
        return True
    if "owner" in owner_text.lower() and "no owners" not in owner_text.lower():
        print("  SKIPPED: another Device Owner/profile owner is already configured.")
        return False

    result = run(adb, serial, "shell", "dpm", "set-device-owner", ADMIN, check=False)
    text = (result.stdout + result.stderr).strip()
    if result.returncode == 0 and "Success" in text:
        print("  Device Owner provisioned.")
        return True
    print("  PROVISIONING FAILED:", text.replace("\n", " "))
    if "already some accounts" in text:
        print("  This tablet must be factory-reset or have all accounts removed before Device Owner provisioning.")
    return False


def deploy(adb: str, serial: str, apk: Path, make_owner: bool) -> bool:
    print(f"\nTablet {serial}")
    install = run(adb, serial, "install", "-r", str(apk), check=False)
    if install.returncode or "Success" not in install.stdout:
        print("  INSTALL FAILED:", (install.stdout + install.stderr).strip())
        return False
    print("  APK installed.")
    if make_owner and not provision(adb, serial):
        return False
    launch = run(adb, serial, "shell", "monkey", "-p", PACKAGE, "1", check=False)
    if launch.returncode:
        print("  LAUNCH FAILED:", (launch.stdout + launch.stderr).strip())
        return False
    print("  Kiosk launched.")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Deploy Node Kiosk through ADB.")
    parser.add_argument("--apk", type=Path, default=DEFAULT_APK, help="APK to install")
    parser.add_argument("--serial", action="append", help="ADB serial to target; repeat for several tablets")
    parser.add_argument("--all", action="store_true", help="Target every connected, authorized ADB tablet")
    parser.add_argument("--provision-device-owner", action="store_true", help="Set this app as Device Owner on eligible clean tablets")
    args = parser.parse_args()

    if not args.apk.is_file():
        parser.error(f"APK not found: {args.apk}. Run .\\gradlew.bat assembleDebug first.")
    if args.all and args.serial:
        parser.error("Use either --all or --serial, not both.")
    adb = find_adb()
    devices = args.serial or connected_devices(adb)
    if not devices:
        print("No authorized ADB tablets found. Connect a tablet and accept its USB-debugging prompt.", file=sys.stderr)
        return 2
    if not args.all and not args.serial and len(devices) > 1:
        print("More than one tablet is connected. Use --serial SERIAL or --all explicitly.", file=sys.stderr)
        return 2
    outcomes = [deploy(adb, serial, args.apk.resolve(), args.provision_device_owner) for serial in devices]
    return 0 if all(outcomes) else 1


if __name__ == "__main__":
    raise SystemExit(main())
