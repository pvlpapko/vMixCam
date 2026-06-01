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


## Автоопределение IP vMix

Приложение при запуске само сканирует локальную Wi-Fi/Ethernet сеть и ищет vMix Web Controller на порту `8088`.

Что нужно включить в vMix:

1. Открой `Settings`.
2. Перейди в `Web Controller`.
3. Включи Web Controller / API.
4. Оставь порт `8088`.
5. Телефон и ПК должны быть в одной Wi-Fi сети.

Если автоопределение не нашло ПК, нажми кнопку `AUTO` внизу экрана или введи IP вручную.


## Wi-Fi/vMix Auto Connect

1. Press **WI-FI** inside the app. Android opens the Wi-Fi chooser/panel.
2. Select the same Wi-Fi network where the vMix PC is connected.
3. Press **AUTO**. The app scans the current subnet and looks for the vMix Web Controller on port `8088`.
4. When found, the vMix PC IP is filled automatically and tally polling starts.

Important: in vMix enable **Settings → Web Controller** and allow port `8088` through Windows Firewall. Android 10+ does not allow silent connection to arbitrary saved Wi-Fi networks without user confirmation, so the app opens the system Wi-Fi panel instead of forcing a hidden connection.

## App icon and Android install warning

The project includes a themed adaptive launcher icon for Android 8+.

Android cannot fully hide the warning shown when installing APK files manually from GitHub/browser. That warning is controlled by Android/Play Protect and disappears only for apps installed from trusted stores such as Google Play or enterprise-managed distribution. For private use, install the APK once and allow installs from the selected source on the phone.


## Версия v6

Добавлено:
- русскоязычный интерфейс;
- большая круглая кнопка начала записи/эфира как в NDI/Nidi-подобных камерах;
- отдельная панель подключения: поиск vMix больше не запускается сразу на главном экране;
- выбор Wi‑Fi через системное меню Android;
- ручное подключение по IP и авто-поиск vMix в текущей сети;
- программная стабилизация предпросмотра: лёгкий цифровой crop/scale, полезно если нет встроенной стабилизации;
- быстрые профили 720p / 1080p / 60fps;
- улучшенный тёмный интерфейс;
- дополнительные функции: Zebra, Focus Peaking, Safe Zone, Low Light Preview, Hide HUD.

Важно: кнопка записи/эфира управляет состоянием приложения и UI. Реальный SRT/RTSP транспорт остаётся точкой подключения для нативного packetizer/libsrt.


## v7
- Панель подключения автоматически закрывается после успешного поиска vMix.
- Добавлена кнопка «Закрыть» в панели подключения.
- Верхние статусы переведены на русский.


## Версия v8 — усиленная стабилизация

Добавлено:
- полностью русский интерфейс;
- режимы стабилизации по кнопке слева: `Стаб выкл` → `Стаб` → `Стаб+` → `Ультра`;
- гироскопическая компенсация мелкой дрожи рук;
- кроп-стабилизация с запасом кадра по краям;
- режим `Ультра` с удержанием горизонта;
- плавное сглаживание рывков при повороте телефона.

Важно: программная стабилизация заметно сглаживает дрожание рук, но физический стабилизатор при ходьбе полностью не заменяет.
