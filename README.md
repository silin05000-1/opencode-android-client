# OpenCode Android Client

Мобильный клиент для сервера [OpenCode](https://github.com/opencode-ai/opencode) на Kotlin: Jetpack Compose, Material 3, Gradle Kotlin DSL, Retrofit + Gson, CameraX + ML Kit.

- `compileSdk` / `targetSdk`: **35**
- `minSdk`: **26** (Android 8.0)
- JDK: **17**

## Что умеет

- задать адрес сервера OpenCode вручную (`http://host:4096`) и сохранить его между запусками;
- отсканировать QR-код от `opencode pair` камерой (CameraX + ML Kit Barcode Scanning);
- создать сессию через `POST /session`;
- отправлять сообщения в сессию через `POST /session/{id}/message` и показывать ответ в чате;
- собирать Debug APK через GitHub Actions без установленного Android Studio.

## Структура

```
app/src/main/java/com/example/opencodeandroidclient/
├── MainActivity.kt
├── data/
│   ├── Models.kt
│   └── OpenCodeApi.kt
├── viewmodel/
│   └── ChatViewModel.kt
└── ui/
    ├── ChatScreen.kt
    ├── QrCodeAnalyzer.kt
    ├── QrScannerScreen.kt
    └── theme/Theme.kt
app/src/main/AndroidManifest.xml
.github/workflows/build-apk.yml
```

## Сборка без Android Studio

Нужен JDK 17. Android SDK Gradle подтянет сам через `local.properties` или переменную `ANDROID_HOME`.

```bash
git clone https://github.com/silin05000-1/opencode-android-client.git
cd opencode-android-client

# если Android SDK не в стандартном месте:
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

chmod +x ./gradlew
./gradlew assembleDebug
```

APK появится по пути:

```
app/build/outputs/apk/debug/app-debug.apk
```

Установка на подключённое устройство/эмулятор:

```bash
./gradlew installDebug
```

## GitHub Actions

Workflow `.github/workflows/build-apk.yml` запускается на `push` в `main`/`master`, на pull request и вручную (`workflow_dispatch`). Он использует JDK 17, выполняет `./gradlew assembleDebug` и выгружает `app-debug.apk` как артефакт.

## Подключение к серверу

1. Запустите OpenCode сервер, например: `opencode serve --port 4096`.
2. Введите адрес в поле «Адрес сервера» (для эмулятора Android это обычно `http://10.0.2.2:4096/`) и нажмите «Подключиться», **или**
3. Нажмите кнопку QR в верхней панели и отсканируйте код от `opencode pair`.

Приложение использует `usesCleartextTraffic="true"`, поэтому работает с обычным HTTP без HTTPS.

## Безопасность

В репозитории нет токенов, ключей и адресов серверов: адрес сохраняется только локально в `SharedPreferences` на устройстве. Логирование тел запросов отключено.
