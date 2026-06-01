# vMixCam

Android camera app prototype for using a phone as a live camera source for vMix over the same Wi-Fi network.

## Current build

This project is made to compile in Android Studio and GitHub Actions. It includes:

- Kotlin + Jetpack Compose UI
- CameraX live preview
- autofocus / tap-to-focus
- zoom slider
- exposure slider
- microphone toggle UI
- grid overlay
- safe-zone overlay
- crosshair overlay
- Wi-Fi/battery indicators
- vMix API tally polling
- stream profile screen for SRT/RTSP connection data
- clean module structure for replacing the transport with a native SRT/RTSP sender

## Build in GitHub

1. Create a new GitHub repository.
2. Upload all files from this ZIP.
3. Open **Actions**.
4. Run **Android APK** workflow.
5. Download the generated `vMixCam-debug-apk` artifact.

## Build locally

Open the folder in Android Studio and run:

```bash
gradle assembleDebug
```

APK path:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## vMix settings

The app screen shows recommended URLs:

```text
srt://PHONE_IP:9999?mode=caller
rtsp://PHONE_IP:8554/live
```

For vMix tally, enable vMix web controller/API and set the PC IP inside the app, usually:

```text
http://VMIX_PC_IP:8088/api
```

## Important note

The repository contains a working camera/UI base and a transport abstraction. Full real-time SRT/RTSP packet streaming requires adding the native transport layer, usually libsrt or an RTSP/RTP H.264 packetizer. This avoids NDI licensing and keeps the app open-source friendly.
