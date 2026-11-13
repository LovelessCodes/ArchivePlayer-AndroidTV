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

Download the latest APK from
[Releases](https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/releases).
These methods work on Android TV, Google TV, and Fire TV. Samsung Tizen and LG
webOS televisions cannot install Android APKs.

| Method | Needs | Best for |
|---|---|---|
| [ADB over the network](#adb-over-the-network) | Computer, network debugging | Development and repeat installs |
| [Downloader app](#downloader-app-on-the-tv) | TV only | One-off install without a computer |
| [Phone to TV](#phone-to-tv) | Phone and TV | Avoiding remote-control typing |
| [USB drive](#usb-drive) | USB drive and a file manager | Offline installs |
| [Local web server](#local-web-server) | Computer on the same LAN | Testing a local build |
| [Obtainium](#obtainium) | One-time APK install | Automatic GitHub updates |
| [Play internal testing](#google-play-internal-testing) | Play Console account | Family and recurring distribution |
| [Amazon Appstore](#amazon-appstore) | Amazon developer account | Fire TV distribution |

Each release publishes a `SHA256SUMS.txt`. Verify the download on a computer
before transferring it:

```bash
sha256sum ArchivePlayerTV-0.1.0.apk
```

### ADB over the network

This is the fastest method if you already use Android development tools.

```bash
adb connect 192.168.1.50:5555
adb install -r ArchivePlayerTV-0.1.0.apk
```

Enable **Network debugging** in the TV's developer options first. On a Google TV
or Android TV device this is usually under **Settings → System → Developer
options → Network debugging**. Some devices call it **USB debugging** and require
a USB cable for the first connection.

### Downloader app on the TV

No computer required.

1. Install **Downloader by AFTVnews** from the TV's app store, where available.
2. Open Downloader and allow it to install unknown apps when prompted.
3. Enter the direct APK address:

   ```text
   https://github.com/LovelessCodes/ArchivePlayer-AndroidTV/releases/download/v0.1.0/ArchivePlayerTV-0.1.0.apk
   ```

   Replace `0.1.0` with the current release version.

4. Download the file, install it, and open **ArchivePlayer TV**.

Typing that address with a remote is tedious. Use the **Google TV** phone app's
keyboard, a Bluetooth keyboard, or a USB keyboard to paste it.

### Phone to TV

This avoids most typing and works well when the TV has no browser.

1. Install **Send Files to TV** on the phone and the TV from their app stores.
2. Download the APK on the phone from the release page.
3. Send the APK to the TV.
4. If the TV has no file manager, install **X-plore File Manager** or **FX File
   Explorer** from the TV's app store.
5. Open the received APK from the file manager and install it.

Allow installs from the file manager when Android asks.

### USB drive

1. Copy `ArchivePlayerTV-0.1.0.apk` to a FAT32 or exFAT USB drive.
2. Plug the drive into the TV.
3. Open it with a file manager such as **X-plore** or **FX File Explorer**.
4. Select the APK and install it.

The TV's built-in media player normally will not install APKs; a file manager is
required.

### Local web server

If the TV and computer are on the same network:

```bash
cd ~/Downloads
python3 -m http.server 8000
```

Then open this address in Downloader or the TV browser:

```text
http://<computer-lan-ip>:8000/ArchivePlayerTV-0.1.0.apk
```

For example, `http://192.168.1.20:8000/ArchivePlayerTV-0.1.0.apk`. Allow the
connection through the computer firewall if necessary.

This serves files over plain HTTP only on your local network. Do not expose the
directory to the public internet.

### Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) installs and updates APKs
directly from GitHub releases.

1. Install Obtainium on the TV using Downloader, a phone transfer, or USB.
2. Add this repository URL:

   ```text
   https://github.com/LovelessCodes/ArchivePlayer-AndroidTV
   ```

3. Let Obtainium import the releases and install the latest APK.

Its interface is designed for phones, so a mouse or a remote with pointer support
is helpful. Obtainium can check for new GitHub releases and offer updates without
repeating the sideload process.

### Google Play internal testing

This is the cleanest option for family, friends, or long-lived distribution.

You need a Google Play Console developer account, a release-signed APK or AAB,
and the app's store listing and content declarations. The TV banner and Leanback
entry point are already in this repository. Create an **Internal testing** track,
add testers by Google account, and install through the Play Store on the TV.
Internal testers receive updates normally.

The preview APK attached to GitHub is debug-signed and is not suitable for Play
distribution. Configure a release keystore as described in
[Release signing](#release-signing).

### Amazon Appstore

For Fire TV, you can submit a release-signed APK to the Amazon Appstore. Amazon
review is required. For a single device, the Downloader app is much faster.

### Allow installs from unknown sources

Android TV grants this permission per installing app. Open **Settings → Apps →
Security & restrictions → Unknown sources** and enable the app you are using to
install the APK — Downloader, the file manager, or the browser. On some devices
the path is **Settings → Device Preferences → Security & restrictions**. On Fire
TV, use **Settings → My Fire TV → Developer Options → Install unknown apps**.

### Signing and updates

The current GitHub release APK is signed with the Android debug key because no
production keystore is configured. It installs, but a future build signed with a
different key cannot update it in place. Uninstall the old app first:

```bash
adb uninstall codes.loveless.archiveplayer.tv
```

Or use **Settings → Apps → ArchivePlayer TV → Uninstall**. The same rule applies
if a locally built APK uses a different debug key.

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
