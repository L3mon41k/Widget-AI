# WidgetAI

AI-powered Android home-screen widget creator.

## Stack
- Kotlin, Jetpack Compose, Android App Widgets
- Remote AI generation through an HTTPS backend
- On-device Android data providers (battery, time, network)
- GitHub Actions APK builds

## Backend contract
The Android client sends POST /v1/widgets/generate with JSON containing a prompt and an explicitly selected device context. The server returns a validated widget specification. Keep provider API keys on the server; never embed them in the APK.

## Privacy
Device information is read on-device. Only selected context is sent with a generation request. Notification access is optional; notification content is not uploaded by default.

## Build
GitHub Actions → Android CI → Run workflow. The debug APK is uploaded as an artifact.
