# Libre Notes

Libre Notes is a local-first, Markdown-first notes app. The current direction is a polished web design prototype first, followed by a native Android app in Kotlin and Jetpack Compose.

## Repository Layout

- `apps/web` - Vite, React, and TypeScript design prototype for first-run setup, vault status, editor, preview, exports, lock screen, and settings.
- `apps/android` - Native Android app scaffold using Kotlin, Jetpack Compose, Material 3, app-layer/domain-layer/data-layer boundaries, vault crypto helpers, and export helpers.
- `legacy/flutter-prototype` - Preserved Flutter prototype kept for reference while the native rebuild reaches parity.
- `assets/brand/icon.jpg` - Libre Notes brand icon used by the web prototype and Android launcher.

## Local Web Preview

```bash
cd apps/web
npm install
npm run dev
```

Build the web artifact:

```bash
cd apps/web
npm run build
```

## Android Build

The Android app is built from the repository root:

```bash
gradle :apps:android:assembleDebug
```

If Gradle is not installed locally, the GitHub workflow provisions Gradle and builds the debug APK automatically.

## GitHub Build

GitHub Actions builds and uploads:

- `libre-notes-web-preview`
- `libre-notes-android-debug-apk`

## Product Notes

- The production Android app defaults to a selected folder plus an encrypted `libre-notes.vault` file.
- Markdown export keeps the original source exactly.
- HTML, DOC, and PDF exports render Markdown structure instead of wrapping raw Markdown as plain text.
- Biometric unlock is treated as a fast unlock path after a password or PIN has been configured.
