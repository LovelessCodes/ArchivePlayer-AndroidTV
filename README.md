# ArchivePlayer TV

[![Android](https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/actions/workflows/android.yml/badge.svg)](https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/LovelessCodes/ArchivePlayer-AndroidTV)](https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/releases)

A native Android TV client for [ArchivePlayer](https://github.com/LovelessCodes/ArchivePlayer), the local-first
Archive.org media library. Built with Kotlin and Jetpack Compose for TV, designed
for a D-pad first, ten-foot experience.

The TV app is a companion to the ArchivePlayer server. It discovers a server on
the LAN, pairs with a short code, browses the library, plays Archive.org video
through Media3/ExoPlayer, and keeps watch progress in sync. There is no cloud
account, no telemetry, and no third-party media service in the loop.

> ArchivePlayer TV is derived from the original
> [ArchivePlayer](https://github.com/JeppeKjeldal/ArchivePlayer) by **Jeppe
> Kjeldal**. See [Attribution](#attribution) and [LICENSE](LICENSE).

## Features

- **LAN discovery** — finds servers advertising `_archiveplayer._tcp` with
  Android NSD/mDNS. No IP address typing on first run.
- **Device pairing** — the TV starts an RFC 8628-style device grant. Your phone
  approves it from an already signed-in browser. The TV never asks for an
  account credential.
- **QR or short code** — scan the QR from a phone or enter the dashed code at
  `/#/activate`.
- **Library browsing** — continue watching, category rows, posters, backdrop
  hero, and a focused title detail screen.
- **Episodes** — per-episode progress and direct playback from the detail screen.
- **Playback** — Media3 ExoPlayer, D-pad play/pause, 10-second seek, progress
  reporting, and completion tracking.
- **Remote-aware profiles** — keep a LAN URL and an optional HTTPS remote URL.
  The client tries the LAN first, then the remote address, using the same token.
- **Encrypted token** — the bearer token is stored with an Android Keystore
  AES/GCM key. A `401` clears it and returns to pairing.
- **Remote-first UI** — visible focus rings, large targets, no text fields on the
  normal path, and an on-screen keyboard only where a URL or device name is
  genuinely needed.

## Requirements

| | |
|---|---|
| ArchivePlayer server | v0.5.0 or newer, API version `1` |
| Android | Android TV 5.0 (API 23) or newer |
| Build | JDK 17, Android SDK 35, Android build-tools 35.0.0 |
| Gradle | Wrapper included (Gradle 8.13) |

The server must be reachable from the TV. On the same LAN, discovery works when
multicast DNS is allowed. Guest Wi-Fi, access-point isolation, Docker bridge
networks, and some VLANs block mDNS; use **Enter address manually** there.

## Install

Download the APK from [Releases](https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/releases)
and sideload it:

```bash
adb connect 192.168.1.50:5555
adb install -r ArchivePlayerTV-0.1.0.apk
```

Enable **Network debugging** in the TV's developer options first. On a Google TV
or Android TV device this is usually under **Settings → System → Developer
options → Network debugging**.

The preview APK is signed with the Android debug key. It is suitable for
sideloading and development. A stable release key will be used for store or
long-lived distribution; changing signing keys requires uninstalling the old
build.

## First run

1. The TV scans the LAN for ArchivePlayer servers.
2. Pick the server. The app fetches `/api/health` and checks `apiVersion`.
3. The TV shows a pairing code and QR code.
4. On a phone signed in to that server, open the verification address and
   approve the request. The QR contains the same address with the code filled in.
5. The server returns an opaque token. The TV stores it in the Android Keystore
   and opens the library.

If discovery is unavailable, choose **Enter address manually**. Plain HTTP is
accepted only for private/LAN addresses. Public hosts must use HTTPS.

## Build

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Useful checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

`local.properties` is ignored. CI uses the runner's Android SDK and sets
`ANDROID_HOME`/`ANDROID_SDK_ROOT` explicitly.

### Release signing

`app/build.gradle.kts` reads the following environment variables when they are
present:

```bash
export KEYSTORE_FILE=/absolute/path/to/release.jks
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

If `KEYSTORE_FILE` is absent, the release build falls back to the debug signing
config so contributors can still produce an installable APK. Do not publish a
production build signed with the debug key.

## How it connects

The client uses the same contracts documented in the server repository's
[`docs/tv-clients.md`](https://github.com/LovelessCodes/ArchivePlayer/blob/main/docs/tv-clients.md).
The short version:

| Purpose | Endpoint |
|---|---|
| Health and API version | `GET /api/health` |
| Start pairing | `POST /api/device/start` |
| Poll/redeem pairing | `POST /api/device/token` |
| Library | `GET /api/library` |
| Continue watching | `GET /api/library/continue-watching` |
| Title and episode data | `GET /api/playback/{identifier}` |
| Save progress | `PUT /api/playback/{identifier}/progress` |
| Mark completed | `POST /api/playback/{identifier}/completed` |

All authenticated calls send `Authorization: Bearer <token>`. Tokens are opaque,
stored hashed on the server, and can be revoked from the server's **Settings →
Connected devices** page.

Set `ARCHIVEPLAYER_MDNS=false` only if you have disabled discovery deliberately;
manual entry remains available. If the server runs behind a reverse proxy, give
it a correct public HTTPS URL with `ARCHIVEPLAYER_PUBLIC_URL` so the QR code
points at a reachable activation page.

### Remote access

Add an HTTPS address under **Settings → Remote address** to use the same TV while
travelling. The client tries the LAN address first and falls back to the remote
address. The token is shared between both endpoints, so pairing is not repeated.

Plain HTTP is rejected for public hostnames. For private access, prefer a VPN
such as Tailscale or WireGuard over exposing the server directly. The server
repository documents Caddy, Cloudflare Tunnel, and Docker setups.

## Project layout

```
app/src/main/java/codes/loveless/archiveplayer/tv/
├── data/          API models, OkHttp client, repository, NSD discovery, token store
├── ui/
│   ├── components/  shared TV surfaces, cards, hero, top bar
│   ├── detail/      title and episode detail screen
│   ├── home/        library rows and continue watching
│   ├── pairing/     device grant, QR, countdown, retry
│   ├── player/      Media3 player, D-pad controls, progress reporting
│   ├── settings/    server, account, device, about
│   ├── setup/       discovery and manual address
│   └── theme/       dark palette, typography, shapes
└── util/          URL validation, formatting, QR bitmap, progress bar
```

See [docs/architecture.md](docs/architecture.md) for the data flow and security
boundaries.

## Development notes

- D-pad focus is explicit. Use `focusRequester` when a screen loads and keep
  button targets at least 44 dp.
- All network work runs off the main thread through coroutines.
- Discovery is mDNS only. There is no cloud rendezvous; the TV and server must
  be able to reach each other.
- The browser mock in the server repo is not used here; unit tests use
  MockWebServer and JVM-only code.
- The UI uses hardcoded English strings in Compose for now. `strings.xml` holds
  app labels and is ready for localization.

## Attribution

ArchivePlayer TV is a native Android TV client derived from ArchivePlayer by
**Jeppe Kjeldal**.

- Original project: <https://github.com/JeppeKjeldal/ArchivePlayer>
- Original work Copyright (c) 2026 Jeppe Kjeldal
- Modifications Copyright (c) 2026 ArchivePlayer contributors

The original author is credited in the app under **Settings → About**, in
[NOTICE](NOTICE), and in [LICENSE](LICENSE). The license is the MIT License with
Attribution Requirement. Any distribution, fork, or derivative work must keep
that attribution and link.

## License

[MIT License with Attribution Requirement](LICENSE).
