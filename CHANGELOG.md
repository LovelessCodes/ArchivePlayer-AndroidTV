# Changelog

## 0.1.0 — 2026-11-11

First native Android TV client release.

- Android TV app built with Kotlin and Jetpack Compose for TV.
- mDNS/NSD discovery for servers advertising `_archiveplayer._tcp`.
- Device-code pairing with QR display and short-code approval; no account
  credential is entered on the TV.
- Android Keystore AES/GCM token storage; `401` clears the token and restarts
  pairing.
- Library browsing with continue watching, category rows, and title details.
- Episode list with per-episode progress and direct playback.
- Media3/ExoPlayer player with D-pad play/pause, 10-second seek, periodic progress
  reporting, and completion tracking.
- Optional HTTPS remote URL with LAN-first fallback and a shared token.
- Manual server address fallback when mDNS is blocked.
- Unit tests for URL validation, formatting, API paths, pairing errors, and
  progress responses.
