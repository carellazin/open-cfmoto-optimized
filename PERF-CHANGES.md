# OpenCfMoto Perf — modifiche rispetto a upstream 2.0.18

Fork personale di [zanderp/open-cfmoto](https://github.com/zanderp/open-cfmoto) (AGPL‑3.0) con
collegamento più rapido e meno latenza video. Tutte le modifiche nel codice sono marcate `[perf]`.

> ⚠️ Non testato su moto reale. Le modifiche sono piccole e prudenti, ma **tieni installata anche
> l'app originale** come riserva: questa build ha un pacchetto diverso (`dev.zanderp.opencfmoto.perf`,
> nome "Open CfMoto Perf") e si installa affiancata. Usane **una sola alla volta** (si contendono le
> stesse porte).

## Collegamento più rapido

| Dove | Prima | Ora | Effetto |
|---|---|---|---|
| `aa/AaReceiver.kt` – chiamata al "server unità principale" di Android Auto (AA 17.4+) | attesa fissa 4 s, poi un tentativo ogni 2 s | primo tentativo a 0,3 s, poi ogni 0,5 s (stessa finestra di ~40 s) | **~3,5–4 s in meno** a ogni Connect e a ogni ripresa |
| `aa/AaReceiver.kt` – soglia "video AA stabile" prima di contattare la moto | ≥ 25 fps | ≥ 15 fps (per 1 s pieno) | niente secondi persi su telefoni che fanno girare AA a 20–24 fps |
| `EasyConnProber.kt` – scoperta della moto | prima NSD (fino a 12 s), poi porta classica :10930 | prima l'**ultimo endpoint che ha funzionato** e la porta :10930 con timeout brevi, poi NSD come prima | salta l'attesa NSD sulle CFMoto che rispondono su :10930 |
| `EasyConnProber.kt` – tentativi di probe | pausa anche dopo l'ultimo tentativo fallito | nessuna pausa dopo l'ultimo | fino a qualche secondo in meno nei percorsi di ripiego |
| `EasyConnProber.kt` – riconnessione dopo un calo | primo tentativo dopo 1 s | dopo 0,3 s, provando prima l'ultimo endpoint buono | ripresa più veloce dopo un blackout del cruscotto |
| `ProbeMemory.kt` (nuovo) | — | ricorda host:porta dell'ultimo probe accettato | — |

## Meno latenza video

| Dove | Prima | Ora | Effetto |
|---|---|---|---|
| `VideoPipeline.kt` – coda dei fotogrammi codificati | 8 (fino a ~330 ms di immagine vecchia) | 3 (~1–2 fotogrammi) | risposta al tocco/zoom più immediata |
| `VideoPipeline.kt` – quando un fotogramma viene scartato | si aspettava il keyframe successivo (fino a 1–2 s di mappa "sporca") | richiesta immediata di un keyframe (max 1 ogni 750 ms) | niente scie/blocchi dopo un calo del Wi‑Fi |
| `aa/VideoDecoder.kt` – decoder del video Android Auto | configurazione standard | modalità **low-latency** (API 30+ se supportata, chiavi vendor Qualcomm/Exynos/Amlogic, priorità real-time), con ripiego automatico alla configurazione originale | tolti alcuni fotogrammi di buffer interno del decoder |
| `VideoPrefs.kt` – modalità energia predefinita | Balanced (24 fps) | Smooth (30 fps) | più fluido; se l'hai già scelta in Setup vale la tua scelta |

## Altro

- `app/build.gradle.kts`: `applicationIdSuffix = ".perf"`, versione `2.0.18-perf`, firma leggibile da
  variabili d'ambiente (per CI), telemetria anonima verso upstream **disattivata** di default.
- `.github/workflows/build-apk.yml`: compila l'APK su GitHub Actions.

## Come ottenere l'APK

### Opzione A — GitHub Actions (non serve installare nulla)

1. Su GitHub fai **Fork** di `zanderp/open-cfmoto`.
2. Dal PC applica la patch al tuo fork e fai push:
   ```bash
   git clone https://github.com/<tuo-utente>/open-cfmoto.git
   cd open-cfmoto
   git apply /percorso/opencfmoto-perf.patch
   git add -A && git commit -m "perf: faster connect + lower video latency"
   git push
   ```
   (La patch è fatta sul commit upstream `0abbe2a`; se upstream è andato avanti e `git apply`
   rifiuta, prova `git apply --3way`.)
3. *(Consigliato)* Nel fork: **Settings → Secrets and variables → Actions → New repository secret**,
   crea i 4 secret con i valori del file `signing-secrets.txt`. Così ogni build futura si installa
   **sopra** la precedente senza perdere le impostazioni.
4. Tab **Actions** → "Build APK (perf)" (parte da sola al push, oppure *Run workflow*) → a fine run
   scarica l'artifact **OpenCfMoto-perf-apk** → dentro c'è `OpenCfMoto-perf-arm64.apk`.

### Opzione B — Android Studio

Apri la cartella, attendi il sync Gradle, poi **Build → Generate App Bundles or APKs → Build APK(s)**
(o `./gradlew :app:assembleRelease`). L'APK è in `app/build/outputs/apk/release/`.

## Dopo l'installazione

- Esporta le impostazioni dall'app originale (**Setup → Share JSON**) e importale nella Perf
  (**Setup → Import…**) per non rifare la configurazione della moto.
- Android Auto: resta necessario **Avvia server unità principale** (menu ⋮ nelle impostazioni
  sviluppatore di AA) — è proprio il percorso che questa build accelera.
- Se qualcosa va peggio: **Setup → Report a problem / Share Logs**; nei log cerca le righe
  `fast path`, `head unit server`, `steady video`, `low-latency`.
