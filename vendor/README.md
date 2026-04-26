# vendor/

Third-party / device-pulled artefacts used during reverse engineering.

## Files included in the public repo

- **`platform_sdk_v4.1.0326.jar`** — Urovo's official SDK jar, mirrored from
  the public Urovo GitHub at
  `https://github.com/urovosamples/SDK_ReleaseforAndroid`. Contains
  `android.device.KeyMapManager`, `KeyMapManager$KeyEntry`, `ScanManager`,
  `DeviceManager`, etc. Use as `compileOnly` dependency if you ever build
  a v3 fork that uses the Urovo SDK directly.

## Files NOT in the public repo (gitignored — pulled from device for analysis only)

- `com.ubx.platform.jar` — Urovo system framework (355 KB, on-device-only).
- `com.ubx.usdk.jar` — Urovo SDK runtime (23 KB, on-device-only).
- `classes.dex`, `META-INF/` — extracted classes for inspection.

These are Urovo IP and stay out of git. You can pull them yourself:

```bash
adb pull /system/framework/com.ubx.platform.jar vendor/
adb pull /system/framework/com.ubx.usdk.jar vendor/
```
