# BitStream

A minimal, resource-light **Android TV** player. Browse trending movies & TV shows, search, press play. No history, no favorites, no accounts — just a clean D-pad-first UI and an embed-based player.

Works from **Android 6.0 (API 23)** to the latest Android TV.

## How it works

```
TMDB (trending + search)  →  IMDb id  →  embed provider URL  →  fullscreen WebView player
```

- **Data**: [TMDB](https://www.themoviedb.org) (read-only token in `core/Tmdb.kt`)
- **Playback**: embed providers — [VidFast](https://vidfast.vc) and [VidAPI](https://vidapi.ru)
- **Player**: system WebView, isolated in a dedicated `:player` process

## Features

- Home: **Trending Movies** / **Trending TV** poster rows, endless scroll
- Search with the system keyboard (IME), mixed movie/TV results with badges
- TV shows: season switcher + episode list (with overview on focus)
- **Choose source on play**: clicking a movie poster or an episode opens a
  D-pad-friendly overlay listing every registered provider (VidFast, VidAPI, …).
  The last used source is pre-focused; if only one provider is registered the
  overlay is skipped and playback starts immediately.
- Remote-only UI: arrows + OK + BACK, visible focus ring, focus memory
- Popup/new-window blocking and top-frame navigation lock in the player
- Bundled modern CA roots (Let's Encrypt ISRG X1, Google Trust Services R4) so HTTPS keeps working on unpatched Android 6/7 boxes
- Warns if the system WebView is too old for the embed player
- **Old-box rescue**: when the system WebView is outdated, the player offers a
  guided "Fix WebView" screen (Play update or one-time APK sideload of WebView 105,
  the last release for Android 6.0) with per-box ABI instructions and re-check

## Built to stay small

| Choice | Reason |
| --- | --- |
| Kotlin + XML views (no Compose, no appcompat) | smaller APK, lower RAM |
| No database, no DI framework | nothing to store, nothing to start |
| Single shared OkHttp + Coil pipeline | one connection pool, capped caches |
| R8 minify + resource shrinking | ~5 MB APK |
| `:player` process for WebView | exiting playback frees all player memory instantly |

## Building

**Android Studio**: open the project, press Run.

**Command line**:

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Without a signing setup the release APK is signed with the debug key (fine for
sideloading). To sign with your own key, create `keystore.properties` in the
project root (git-ignored):

```properties
storeFile=/absolute/path/to/bitstream.keystore
storePassword=********
keyAlias=bitstream
keyPassword=********
```

Create a keystore once with:

```bash
keytool -genkeypair -v -keystore bitstream.keystore -alias bitstream \
  -keyalg RSA -keysize 2048 -validity 10000
```

**CI**: every push to `main` builds an APK (see `.github/workflows/build.yml`)
and uploads it as an artifact.

## Adding another embed provider

The app talks to sources only through the `EmbedProvider` interface, and the
on-play **Choose source** overlay lists whatever is in the registry — so a new
provider needs **one small file + one registry line**, nothing else:

```kotlin
object MyProvider : EmbedProvider {
    override val id = "myprovider"
    override val name = "My Provider"
    override val navHosts = setOf("myprovider.example")

    // Providers receive BOTH ids — use whichever your service keys off
    // (IMDb `tt…`, TMDB numeric, or a fallback chain).
    override fun movieUrl(ids: TitleIds) =
        "https://myprovider.example/embed/movie/${ids.imdbId ?: ids.tmdbId}"
    override fun tvUrl(ids: TitleIds, season: Int, episode: Int) =
        "https://myprovider.example/embed/tv/${ids.imdbId ?: ids.tmdbId}/$season/$episode"
}

// provider/EmbedProvider.kt — add to the registry list:
private val registry = mutableListOf<EmbedProvider>(VidFastProvider, VidApiProvider, MyProvider)
```

The new source immediately shows up as a button in the chooser — no UI changes.
Ships with:

| Provider | Movie embed | TV embed |
| --- | --- | --- |
| **VidFast** | `vidfast.vc/movie/{imdb}?autoPlay=true` | `vidfast.vc/tv/{imdb}/{s}/{e}?autoPlay=true` |
| **VidAPI** | `vaplayer.ru/embed/movie/{imdb-or-tmdb}?autoplay=1` | `vaplayer.ru/embed/tv/{imdb-or-tmdb}/{s}/{e}?autoplay=1` |

## Project layout

```
app/src/main/java/com/bitstream/tv/
├── App.kt                  # application, shared Coil image pipeline
├── core/                   # TMDB client, models, OkHttp + old-Android TLS fix
├── provider/               # EmbedProvider interface + registry, VidFast, VidAPI
├── home/                   # trending rows (D-pad focus UX)
├── search/                 # IME search + mixed results grid
├── show/                   # season/episode picker
├── player/                 # WebView player (runs in :player process)
└── ui/                     # navigation helpers + "Choose source" overlay
```

## Notes for old Android 6/7 boxes

Cheap Android 6/7 boxes ship a 2015-era **AOSP WebView** (Chromium 44–56). The
obfuscated modern embed players need **Chromium 80+** — old engines can't even
parse their JavaScript, so videos fail. The fix is a one-time system WebView
update, and BitStream guides the user through it:

- When an outdated WebView is detected, the player shows a warning and a
  **“Fix WebView”** button (also on the error screen) that opens an in-app
  **rescue screen**:
  - Box has Google Play → deep link to the *Android System WebView* store page.
  - No Play Store → guided sideload of the last Google-signed release that
    still supports Android 6.0: **Android System WebView 105.0.5195.136**
    (Android 7.0 got up to 114.x). The screen tells the user which file
    variant (arm-v7a / arm64-v8a / x86) fits their box and re-checks when
    they're done.
- Why not bundle our own engine? A browser engine is tens of millions of
  lines — the practical “own WebView” option is embedding GeckoView (~40 MB,
  no Widevine DRM). Keeping the app at ~1 MB and updating the box's WebView
  once is the cleanest solution; rescue restores Chromium 105, which handles
  everything the embed players need.
- Rare AOSP-only firmwares hard-code their WebView and ignore Google-signed
  updates — on those, nothing short of a firmware update changes the engine.
  The rescue screen says so if the version doesn't change after install.
- The app also bundles the ISRG Root X1 / GTS Root R4 certificates because
  unpatched Android 6/7 system CA stores predate them (the usual cause of
  “connection error” on old boxes).
- Install via sideloading: enable "Unknown sources" for your file manager, then
  open the APK.

## Legal

BitStream is a player shell. It does not host or proxy any video content —
what the configured providers serve is entirely their responsibility. Make
sure your use complies with the law and the rights of content owners in your
region.
