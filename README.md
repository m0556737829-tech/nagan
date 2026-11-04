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
The bundled catalog currently contains four verified Hebrew public-domain entries ("התקווה", "שיר העבודה והמלאכה", "נדנדה", and "בגנת הירק"), with source and rights notes in the JSON. It does not scrape Shironet or bundle contemporary copyrighted lyrics without permission. Populate app/src/main/assets/lyrics.json only with lyrics that you own, that are public domain, or that are licensed for redistribution. Users can also add lyrics they are authorized to use from inside the app. The 15,000-song Shironet-derived Kaggle dataset is not imported: its CC BY-SA label alone does not establish that the uploader had rights to relicense the underlying song lyrics.

## Build
Open this repository in Android Studio, or run the included GitHub Actions workflow. The debug APK is produced at:
app/build/outputs/apk/debug/app-debug.apk
