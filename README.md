# Grok Launcher

Installierbare Android-App (Kotlin), die **https://grok.com/** als sicherer Launcher öffnet.

## Architektur (kurz)

- **Primär: Chrome Custom Tabs** (über `androidx.browser`), damit vorhandene Browser-Sessions, System-Autofill und Passwortmanager genutzt werden.
- **Fallback**: Wenn Custom Tabs nicht verfügbar sind, bietet die App einen **minimalen WebView-Fallback** mit klarer Option **„Im Browser öffnen“**.
- **Kein Klon / keine Umgehung**: Die App baut keine grok.com-Funktionen nach und umgeht keine Login-/OAuth-/Cookie-/Security-Mechanismen.

## Projekt in Android Studio öffnen

1. Repository/Projektordner lokal verfügbar machen (Clone oder Download).
2. Android Studio: **File → Open…** und den Ordner `GrokLauncher` auswählen.
3. Gradle Sync abwarten.

## Lokal eine APK bauen

Voraussetzung: **JDK 17**.

Debug-APK:

```bash
chmod +x ./gradlew
./gradlew :app:assembleDebug
```

APK-Pfad:
`app/build/outputs/apk/debug/`

Release-APK (optional, Signierung nötig):

```bash
chmod +x ./gradlew
./gradlew :app:assembleRelease
```

## GitHub Actions: APK als Artifact herunterladen

Workflow: [.github/workflows/android-build.yml](file:///workspace/.github/workflows/android-build.yml)

Build starten:
- automatisch bei Push auf `main`/`master`, oder
- manuell in GitHub unter **Actions → Android Build → Run workflow**

APK herunterladen:
1. GitHub: **Actions**
2. gewünschten Workflow-Run öffnen
3. unter **Artifacts** das Artifact **apks** herunterladen

Die Debug-APK wird immer erzeugt. Die Release-APK wird zusätzlich versucht und kann ohne Signing-Konfiguration fehlschlagen.

## Release Signing (vorbereitet)

Wenn du eine signierte Release-APK bauen willst, lege im Projektroot eine Datei `keystore.properties` an (nicht committen):

```properties
storeFile=/absoluter/pfad/zu/deinem.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

`app/build.gradle.kts` liest diese Datei automatisch ein, wenn sie existiert.

## App-Icon

Im Projekt ist standardmäßig ein Vector-Foreground enthalten:
- `app/src/main/res/drawable/ic_launcher_foreground.xml`

Wenn du dein PNG verwenden willst:
1. Lösche `app/src/main/res/drawable/ic_launcher_foreground.xml`
2. Lege stattdessen `app/src/main/res/drawable/ic_launcher_foreground.png` ab

Die Adaptive-Icon-Definitionen bleiben gleich:
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
