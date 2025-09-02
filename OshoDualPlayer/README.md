# OshoDualPlayer (Minimal)

Dual audio player (discourse + background music) in Kotlin using ExoPlayer.

## Build in GitHub Actions
- Push this repo to GitHub (branch `main`)
- Actions will install Android SDK + Gradle 8.7 and build
- Download the artifact `app-debug.apk` from the run results

## Replace audio
Replace the placeholders in `app/src/main/res/raw/`:
- `discourse.mp3`
- `music.mp3`

## Notes
- This project intentionally does **not** include the Gradle Wrapper (to keep the repo light). The workflow installs Gradle 8.7 and uses `gradle assembleDebug`.
