# galaxy-vitals-bridge

App Android mínima: lee heart rate, SpO2, pasos, pisos subidos, sueño y el
último ejercicio directo de **Samsung Health** (vía el Samsung Health Data
SDK, no Health Connect) y los publica en
[danzuniga.xyz/status](https://danzuniga.xyz/status.html) — mismo endpoint y
schema base que usa el script de BLE del portfolio (`sync_vitals.py`), así
que ambas fuentes son intercambiables.

## Por qué Samsung Health Data SDK y no Health Connect

La primera versión leía de Health Connect. En la práctica, en un teléfono
no-Samsung (probado en un OPPO) Samsung Health nunca sincroniza datos hacia
Health Connect — el toggle de permiso de escritura aparece activado, pero
Health Connect siempre devuelve cero registros, incluso pidiendo 7 días de
historial. El Samsung Health Data SDK lee directo de la app de Samsung
Health por IPC, sin depender de ese puente roto.

## Cómo está armado

- `MainActivity.kt` — UI en Compose: pegás el token de GitHub, pedís permiso
  de Samsung Health, y podés forzar un sync manual.
- `SamsungHealthRepository.kt` — lee heart rate (últimos 30 min), SpO2
  (últimas 24h), pasos y pisos subidos de hoy, la última sesión de sueño
  (últimas 48h) y el último ejercicio registrado (últimos 7 días).
- `GitHubPublisher.kt` — el mismo PUT a la Contents API que hace
  `sync_vitals.py`, sobre `danielzunigazb/portfolio`, rama `data`.
- `VitalsSyncWorker.kt` — `WorkManager` periódico (cada 15 min, el mínimo que
  permite Android para trabajo periódico).
- `TokenStore.kt` — el token de GitHub se guarda con `EncryptedSharedPreferences`,
  solo en este dispositivo. Nunca se commitea ni se manda a ningún lado más
  que a la API de GitHub.
- `app/libs/samsung-health-data-api-1.1.0.aar` — el SDK de Samsung, no está
  en Maven Central así que va commiteado directo en el repo.

## Setup

1. Abrí la carpeta en Android Studio (Open → esta carpeta).
2. **Activá el modo desarrollador del SDK en Samsung Health, en el celular
   donde vas a correr la app** (ver abajo — sin esto el permiso de lectura
   nunca se concede).
3. Conectá tu celular por USB con depuración USB activada (minSdk 29,
   Android 10+; necesita Samsung Health instalado, funciona aunque el
   teléfono no sea Samsung).
4. Run. La primera vez:
   - Pegá un GitHub fine-grained PAT con permiso **Contents: Read and write**
     sobre `danielzunigazb/portfolio` únicamente, y tocá "Guardar token".
   - Tocá "Pedir permiso de heart rate" y aceptá el diálogo de consentimiento
     de Samsung Health (heart rate, SpO2, pasos, pisos subidos, sueño,
     ejercicio).
   - Tocá "Sync ahora" para probar que publique.
5. Confirmá en `danzuniga.xyz/status` que pase a "● LIVE".

Después de esto, `VitalsSyncWorker` sigue solo cada 15 min en background,
sin necesidad de tener la app abierta.

### Activar el modo desarrollador de Samsung Health

Paso obligatorio en cada celular donde se instale la app, mientras el SDK
no esté registrado como partner ante Samsung (ver Notas):

1. Abrí Samsung Health → ⋮ (arriba a la derecha) → **Configuración → Acerca
   de Samsung Health**.
2. Tocá la línea de la versión **10 veces seguidas**, rápido.
3. Aparece **"Developer mode (Samsung Health Data SDK)"** → tocalo → aceptá
   el aviso → activá **"Developer Mode for Data Read"**.

## JSON publicado

```json
{
  "heart_rate_bpm": 80,
  "oxygen_saturation_pct": 98,
  "steps_today": 4600,
  "floors_climbed_today": 3,
  "sleep_duration_minutes": 452,
  "sleep_score": 64,
  "last_exercise": { "type": "RUNNING", "duration_minutes": 32, "calories": 210.5, "mean_heart_rate_bpm": 142 },
  "battery_pct": null,
  "updated_at": "2026-09-20T19:58:45Z",
  "source": "galaxy-fit3-samsunghealth"
}
```

Cualquier campo sin dato reciente va en `null` (o `last_exercise` completo en
`null` si no hay ejercicio registrado en la última semana). `battery_pct`
siempre es `null` — no es un dato de salud/fitness, ningún SDK de Health lo
expone (la vía BLE directa de `sync_vitals.py` sí puede traerlo).

## Notas

- **El modo desarrollador es solo para pruebas.** Funciona indefinidamente
  para tu propio uso en tu propio teléfono. Si algún día se va a distribuir
  esta app a otras personas (Play Store o APK a terceros), hay que registrar
  el paquete + firma (SHA-256) como partner ante Samsung
  ([developer.samsung.com/health/data](https://developer.samsung.com/health/data/overview.html))
  para que el permiso de lectura funcione sin el modo desarrollador activado
  a mano.
- Si Gradle falla con `Daemon compilation failed` / `IllegalArgumentException: 25.0.3`:
  es el compilador de Kotlin 1.9.24 cayéndose al usar un JDK 24+ (el JBR que
  trae Android Studio). Ya está mitigado con `kotlin.incremental=false` en
  `gradle.properties`.
- Si el proyecto vive dentro de una carpeta sincronizada por OneDrive/Drive/
  Dropbox y Gradle falla con `Unable to delete directory` o
  `AccessDeniedException` dentro de `app/build`: es el sincronizador
  compitiendo por los archivos con Gradle, no un bug del proyecto. Pausá la
  sincronización mientras compilás, o movés el proyecto fuera de esa carpeta.
