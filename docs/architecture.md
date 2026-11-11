# Architecture

ArchivePlayer TV is a single-module Android app. It owns presentation, discovery,
pairing, local token storage, and playback. The server remains the source of
truth for users, library metadata, watch progress, and API compatibility.

## Layers

```
┌──────────────────────────────────────────────────────────┐
│ Compose for TV                                           │
│ Setup · Pairing · Home · Detail · Player · Settings      │
└───────────────────────┬──────────────────────────────────┘
                        │ ViewModels / repository
┌───────────────────────▼──────────────────────────────────┐
│ ServerRepository                                         │
│ LAN-first endpoint fallback · API wrappers · progress    │
└───────────┬───────────────────────────┬──────────────────┘
            │                           │
┌───────────▼──────────┐    ┌───────────▼──────────────────┐
│ ApiClient (OkHttp)   │    │ ServerStore (SharedPrefs +  │
│ JSON, bearer header  │    │ Android Keystore AES/GCM)   │
└──────────────────────┘    └──────────────────────────────┘
            │
┌───────────▼──────────────────────────────────────────────┐
│ ArchivePlayer server /api/*                              │
└──────────────────────────────────────────────────────────┘
```

`ServerDiscovery` is independent of the repository. It wraps Android NSD and
emits a `List<DiscoveredServer>` from a `callbackFlow`, filtered to
`_archiveplayer._tcp.` TXT records (`name`, `id`, `api`, `version`, `path`,
optional `url`).

## Startup state

`AppViewModel` exposes a small sealed state machine:

```
Loading → Setup       no saved profile
Loading → Pairing     profile exists but has no token
Loading → Ready       profile has a token
Ready   → Pairing     a 401 from any API call
Ready   → Setup       switch server
```

`StartupState.Pairing` carries an attempt counter so a stale pairing ViewModel is
never reused after a cancel, success, or token expiry.

## Pairing

1. `SetupScreen` validates the URL with `ServerUrl` and calls `GET /api/health`.
2. `PairingViewModel` calls `POST /api/device/start` with the TV's device name.
3. The TV renders the user code and a QR bitmap for `verificationUriComplete`.
4. It polls `POST /api/device/token` at the server-provided interval.
5. After approval, the returned opaque token is persisted encrypted and the
   normal app starts.

The TV never collects an account credential. Approval requires a browser session
on the server; a bearer token cannot approve a device. Revoking the session in
the server UI returns `401` to the TV, which clears its token and starts pairing
again.

## Endpoint fallback

`ServerRepository.withProfile` accepts an ordered list:

1. `lanUrl`
2. optional `remoteUrl`

It retries only `IOException`/network failures. An HTTP error is a real server or
authentication answer, not a reason to try another host. The endpoint that
succeeds is stored as the active URL and shown in Settings.

## Playback and progress

`DetailViewModel` picks a media file in this order:

1. the requested episode
2. the current episode remembered by the server
3. the first episode
4. the first video file

`PlayerViewModel` re-fetches playback details for that file, resolves its exact
URL, and restores the episode or title progress. `PlayerScreen` owns an
`ExoPlayer` instance, reports progress on pause, every 15 seconds while playing,
on app stop, and on dispose, and marks the item complete on `STATE_ENDED`.

Progress writes are fire-and-forget on an application scope. A failed write never
interrupts playback.

## Security boundaries

- **Token at rest:** encrypted with AES/GCM using an Android Keystore key. The
  encrypted blob lives in SharedPreferences; the profile metadata does not
  contain the token.
- **Cleartext:** allowed for LAN/self-hosted HTTP, but `ServerUrl` rejects HTTP
  for public hosts. HTTPS remote addresses use the platform trust store.
- **No trust-all TLS:** there is no custom `TrustManager` or hostname verifier.
- **No silent discovery trust:** discovery only prefills a server address. The
  user selects it and the health check runs before pairing.
- **No cloud rendezvous:** discovery is local mDNS; remote access is an explicit
  user-provided HTTPS URL or VPN.

## Testing

- JVM tests cover URL validation, formatting, API paths, bearer/auth headers,
  pairing error mapping, and 204 progress responses with MockWebServer.
- `lintDebug` checks manifest and Android API usage.
- Release builds run R8; keep serialization models and OkHttp rules intact if
  adding new DTOs.
