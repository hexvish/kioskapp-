# Tablet deployment

`..\\deploy_kiosk.py` uses only Python's standard library and Android Platform Tools.

Build first:

```powershell
.\gradlew.bat assembleDebug
```

Install and launch one connected tablet:

```powershell
python .\deploy_kiosk.py --serial TABLET_SERIAL
```

Install, make the app Device Owner, and launch it on one clean tablet:

```powershell
python .\deploy_kiosk.py --serial TABLET_SERIAL --provision-device-owner
```

For several connected tablets, use `--all` explicitly:

```powershell
python .\deploy_kiosk.py --all --provision-device-owner
```

Device Owner provisioning is an Android operation, not merely an app setting. Each tablet must be freshly prepared with no Google, work, email, or other accounts. The script prints Android's rejection reason and does not reset or erase tablets.
