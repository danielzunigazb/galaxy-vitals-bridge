# galaxy-vitals-bridge

App Android mínima: lee el último heart rate de **Health Connect** (que Samsung
Health ya llena desde el Galaxy Fit3) y lo publica en
[danzuniga.xyz/status](https://danzuniga.xyz/status.html) — mismo endpoint y
schema que usa el script de BLE del portfolio (`sync_vitals.py`), así que
ambas fuentes son intercambiables.

## Cómo está armado

- `MainActivity.kt` — UI en Compose: pegás el token de GitHub, pedís permiso
  de Health Connect, y podés forzar un sync manual.
- `HealthConnectRepository.kt` — lee el último `HeartRateRecord` de los
  últimos 30 min.
- `GitHubPublisher.kt` — el mismo PUT a la Contents API que hace
  `sync_vitals.py`, sobre `danielzunigazb/portfolio`, rama `data`.
- `VitalsSyncWorker.kt` — `WorkManager` periódico (cada 15 min, el mínimo que
  permite Android para trabajo periódico).
- `TokenStore.kt` — el token de GitHub se guarda con `EncryptedSharedPreferences`,
  solo en este dispositivo. Nunca se commitea ni se manda a ningún lado más
  que a la API de GitHub.

## Setup

1. Abrí la carpeta en Android Studio (Open → esta carpeta). Si te pide
   generar el Gradle wrapper, decile que sí.
2. Conectá tu celular por USB con depuración USB activada, o usá un emulador
   con Play Store (Health Connect necesita la app de Health Connect instalada
   — en Android 14+ ya viene en el sistema; en versiones anteriores hay que
   instalarla desde Play Store).
3. Run. La primera vez:
   - Pegá un GitHub fine-grained PAT con permiso **Contents: Read and write**
     sobre `danielzunigazb/portfolio` únicamente, y tocá "Guardar token".
   - Tocá "Pedir permiso de heart rate" y aceptá el permiso de Health Connect.
   - Tocá "Sync ahora" para probar que publique.
4. Confirmá en `danzuniga.xyz/status` que pase a "● LIVE".

Después de esto, `VitalsSyncWorker` sigue solo cada 15 min en background,
sin necesidad de tener la app abierta.

## Notas

- No compilé/corrí esto en un dispositivo real — lo escribí contra la API de
  Health Connect (`androidx.health.connect:connect-client`) tal como la
  conozco, pero el primer build en Android Studio puede pedir algún ajuste
  menor de versiones.
- Solo se lee heart rate. La batería del reloj no es un dato de salud/fitness,
  así que Health Connect no la expone — por eso `battery_pct` siempre va en
  `null` desde esta app (la vía BLE directa sí puede traerla).
