# NAGAN — Offline Music & Lyrics Player

NAGAN is a native Android music player designed to work offline.

## Features
- Scans audio files already stored on the device using Android MediaStore.
- Plays local audio without an internet connection.
- Searches the bundled lyrics catalog by title and artist.
- Supports timed LRC lyrics and plain-text lyrics.
- Lets the user add/edit their own lyrics locally; saved lyrics remain on-device.
- No analytics, ads, accounts, network calls, or online lyrics lookup.

## Lyrics and rights
The project includes an empty catalog structure. It does not scrape Shironet or bundle copyrighted lyrics without permission. Populate app/src/main/assets/lyrics.json only with lyrics that you own or are licensed to distribute. Users can also add lyrics they are authorized to use from inside the app.

## Build
Open this repository in Android Studio, or run the included GitHub Actions workflow. The debug APK is produced at:
app/build/outputs/apk/debug/app-debug.apk
