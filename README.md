# BitStream

A minimal, resource-light **Android TV** player. Browse trending movies & TV shows, search, press play. No history, no favorites, no accounts — just a clean D-pad-first UI and an embed-based player.

Works from **Android 6.0 (API 23)** to the latest Android TV.

## How it works

```
TMDB (trending + search)  →  IMDb id  →  embed provider URL  →  fullscreen WebView player
```

- **Data**: [TMDB](https://www.themoviedb.org) (read-only token in `core/Tmdb.kt`)
- **Playback**: [VidFast](https://vidfast.vc) embeds — `vidfast.vc/movie/{imdbId}` and `vidfast.vc/tv/{imdbId}/{s}/{e}`
- **Player**: system WebView, isolated in a dedicated `:player` process

## Features

- Home: **Trending Movies** / **Trending TV** poster rows, endless scroll
- Search with the system keyboard (IME), mixed movie/TV results with badges
- TV shows: season switcher + episode list (with overview on focus)
- Movies: click poster → plays immediately
- Remote-only UI: arrows + OK + BACK, visible focus ring, focus memory
- Popup/new-window blocking and top-frame navigation lock in the player
- Bundled modern CA roots (Let's Encrypt ISRG X1, Google Trust Services R4) so HTTPS keeps working on unpatched Android 6/7 boxes
- Warns if the system WebView is too old for the embed player

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

The app talks to sources only through the `EmbedProvider` interface:

```kotlin
object MyProvider : EmbedProvider {
    override val id = "myprovider"
    override val name = "My Provider"
    override val navHosts = setOf("myprovider.example")
    override fun movieUrl(imdbId: String) = "https://myprovider.example/embed/movie/$imdbId"
    override fun tvUrl(imdbId: String, season: Int, episode: Int) =
        "https://myprovider.example/embed/tv/$imdbId/$season/$episode"
}

Providers.register(MyProvider) // first registered provider is the default
```

That's the whole integration — no UI changes needed.

## Project layout

```
app/src/main/java/com/bitstream/tv/
├── App.kt                  # application, shared Coil image pipeline
├── core/                   # TMDB client, models, OkHttp + old-Android TLS fix
├── provider/               # EmbedProvider interface + registry, VidFast
├── home/                   # trending rows (D-pad focus UX)
├── search/                 # IME search + mixed results grid
├── show/                   # season/episode picker
├── player/                 # WebView player (runs in :player process)
└── ui/Routes.kt            # navigation helpers
```

## Notes for old Android 6 boxes

- The player needs a reasonably modern **Android System WebView** (Chromium 80+).
  On boxes with Google Play this is usually already updated; if not, the app
  shows a hint to update it.
- The app bundles the ISRG Root X1 / GTS Root R4 certificates because unpatched
  Android 6/7 system CA stores predate them (this is the usual cause of
  "connection error" on old boxes).
- Install via sideloading: enable "Unknown sources" for your file manager, then
  open the APK.

## Legal

BitStream is a player shell. It does not host or proxy any video content —
what the configured providers serve is entirely their responsibility. Make
sure your use complies with the law and the rights of content owners in your
region.
