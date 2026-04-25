# Libre Notes

Libre Notes is a local-first, Markdown-first notes app prototype. The current build target is Flutter, with GitHub Actions configured to build Web, Android debug APK, and Windows release artifacts.

## Current App

- Desktop-style three-pane notes workspace
- Expandable folder dropdown tree
- Colored first-letter folder icons, with settings toggle
- Compact or rich note list modes
- Raw Markdown editor
- Rendered preview pane
- Vertical filename attachment rows or large preview cards
- Settings dialog inspired by VS Code structure
- Light/dark quick toggle
- AMOLED dark option
- Accent color and accent-tinted background controls

## Local Build

Install Flutter in WSL or Windows first, then run:

```bash
flutter create --project-name libre_notes --platforms=web,android,windows .
flutter pub get
flutter analyze
flutter build web --release
```

For Android:

```bash
flutter build apk --debug
```

For Windows:

```bash
flutter config --enable-windows-desktop
flutter build windows --release
```

## GitHub Build

Push this repo to GitHub and open the **Actions** tab. The `Flutter build` workflow installs Flutter, generates missing platform folders, analyzes the app, and uploads artifacts:

- `libre-notes-web`
- `libre-notes-android-debug-apk`
- `libre-notes-windows`

## Development Note

The source is intentionally dependency-light right now. It uses Flutter Material 3 only, so it can build cleanly in GitHub Actions before the app grows into file-system persistence, sync, PDF export, and markdown/LaTeX rendering packages.
