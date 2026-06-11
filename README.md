# Masareef — مصاريف

A private, on-device expense tracker for Egypt. Manual entry plus automatic
expense detection from bank transaction SMS (English and Arabic).

## Features
- Manual expense entry with categories and EGP amounts
- SMS auto-tracking: incoming bank debit SMS are parsed on-device
  (amount, merchant, auto-category) — the SMS text itself is never stored
- One-tap inbox scan to import the last 30 days of transactions
- Monthly stats with per-category breakdown
- 100% offline. No account, no server, no data leaves the phone.

## Building the APK (no local setup needed)
1. Push this repository to GitHub (branch `main`).
2. GitHub Actions builds the APK automatically (see `.github/workflows/build.yml`).
3. Download `masareef.apk` from the **Releases** page of the repo
   (or from the workflow run's Artifacts).
4. Copy it to an Android phone (Android 8.0+) and install.
   You'll need to allow "install from unknown sources".

## Tech
Kotlin · Jetpack Compose (Material 3) · SQLite · no third-party dependencies.
