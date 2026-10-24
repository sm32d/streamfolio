# 04 — API & External Integration Documentation

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Architectural Context: Outbound & Integration Model

StreamFolio contains **no proprietary backend REST API or server**. The application functions as an intelligent HTTP/XML client that interfaces with third-party RSS/Atom publishers, search endpoints, and on-device machine learning sub-systems.

---

## 2. Outbound HTTP Integrations

### 1. Curated & Custom RSS/Atom Feed Fetching

- **HTTP Method:** `GET`
- **Target URL:** Any verified HTTPS RSS or Atom endpoint (e.g. `https://feeds.bbci.co.uk/news/rss.xml`, `https://techcrunch.com/feed/`).
- **Headers Sent:**
  ```http
  User-Agent: Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36
  Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
  Accept-Language: en-US,en;q=0.9
  Connection: keep-alive
  Cache-Control: max-age=0
  ```
- **Security Interceptor:** Checked via `SsrfProtectionInterceptor` to block private/internal subnets.
- **Expected Payload:** RSS 2.0 `<rss><channel><item>` or Atom 1.0 `<feed><entry>` XML structure.
- **Error Handling:** Socket timeouts (10s), connect timeouts (10s), SSL handshake failures, and invalid XML structures are caught and logged with redacted host names.

---

### 2. Google News Search & Aggregation

- **HTTP Method:** `GET`
- **Endpoint Pattern:**
  `https://news.google.com/rss/search?q={query}&hl={language}-{region}&gl={region}&ceid={region}:{language}`
- **Parameters:**
  - `q`: URL-encoded search query string (e.g. `technology+ai`).
  - `hl`: Language code (e.g. `en-US`).
  - `gl`: Country/region code (e.g. `US`).
  - `ceid`: Combined identifier (`US:en`).
- **Response Format:** RSS 2.0 XML containing aggregated search results.
- **Downstream Processing:** Articles from Google News contain redirect links (`https://news.google.com/rss/articles/...`) which are resolved to canonical publisher URLs via `resolveGoogleNewsUrl()`.

---

### 3. Article Body Pre-Scraping & Full-Text Extraction

- **HTTP Method:** `GET`
- **Target URL:** Canonical publisher article link.
- **Engine:** Jsoup HTML parser via `fetchArticleBody(url)` in [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt#L743).
- **Extraction Strategy:**
  1. Inspects `<article>`, `[role=main]`, `[itemprop=articleBody]`, `.article-body`, `.story-body`, `.entry-content`.
  2. Strips advertisement scripts, tracker iframes, navigation chrome, and boilerplate.
  3. Sanitizes output with `Jsoup.clean(..., Safelist.none())`.
- **Not performed for link-only sources** (see section 4): `fetchArticleBody()` returns early for them and the pre-scrape skips them.

---

### 4. Link-Only Feed Fetching (Joho no Todai)

Feeds on hosts registered in [LinkOnlySources.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlySources.kt) (currently `joho-todai.com`, feed `https://joho-todai.com/en/rss/`) use a different fetch path, `fetchLinkOnlyFeedXml()` in [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt). Rationale: [ADR-006](file:///d:/News/docs/14-decisions-and-known-limitations.md).

- **Only fetched while enabled** by the remote source list (section 5). Otherwise no request is made.
- **Headers Sent:**
  ```http
  User-Agent: StreamFolio/<versionName> (Android feed reader; headline-and-link only)
  Accept: application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8
  If-None-Match: <stored ETag, when known>
  If-Modified-Since: <stored Last-Modified, when known>
  ```
- **Rate limit (per device):** at most one request per 30 minutes per feed URL (`LinkOnlyFetchPolicy.MIN_INTERVAL_MS`), applied to automatic refresh, pull-to-refresh, background sync, widget refresh and Settings toggles. Fetches are serialised by a process-wide lock, so simultaneous refreshes cannot both send a request. Longer waits are honored in full, with no 24-hour ceiling: `Cache-Control: max-age`, the feed's `<ttl>`, `Retry-After`. The `<ttl>` from the last `200` response is stored and still applies after a `304`. If the server gives no instruction, `429`/`503` back off 1 hour, `404`/`410` 24 hours, other failures 30 minutes. Only a one-year sanity bound (`MAX_WAIT_MS`) guards against overflow and corrupt values. `304 Not Modified` keeps the cached articles.
- **Fields used:** `title`, `link`, `pubDate`, and the plain-text `description` (HTML stripped, max 500 characters). Ignored: `content`, `content:encoded`, `enclosure`, `media:content`, `media:thumbnail`, `itunes:image`, and images inside `description`.
- **Never requested for these articles:** article pages, thumbnails/`og:image`, favicons (a letter avatar is shown).
- **Search:** online search never fetches link-only feeds; cached headlines are found by the local search.
- **Per-device state** (throttle time, ETag, Last-Modified) is stored in the `link_only_state` SharedPreferences file (see [03](file:///d:/News/docs/03-configuration-and-environments.md)).

---

### 5. Remote Source List (static JSON in this repository)

- **HTTP Method:** `GET`
- **URL:** `https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json` (`SourceConfig.URL` in [LinkOnlyState.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlyState.kt)).
- **Schema:**
  ```json
  { "version": 1, "sources": { "joho-todai-en": { "enabled": true } } }
  ```
- **When:** at the start of every feed refresh (`fetchDefaultFeeds`, `fetchCustomFeeds`, `fetchSingleFeed`) — foreground, background sync and widget alike — before any link-only feed is requested. A list validated within the last 60 seconds (`SourceConfig.FRESHNESS_MS`) is reused, only to coalesce a burst of refreshes into one download.
- **Effect:** a link-only source that is missing or `enabled: false` is not fetched, is hidden from Settings > Providers, and everything cached from it (articles, bookmark-list associations, throttle state) is deleted from the device. If the list cannot be downloaded or parsed, it is not validated and fetching of link-only feeds is suspended until a fresh list is validated (`LinkOnlyStateStore.mayFetch`). Cached articles stay until a validated list says to delete them. Before any successful download, link-only sources are disabled.
- **Availability:** requires the repository to be public with the file on `master` (public as of 2026-10-23).

---

### iOS differences (added 2026-10-24)

- **User-Agent:** ordinary feeds send an iPhone Safari User-Agent (`FeedDownloader.browserUserAgent`) with the same `Accept` / `Accept-Language` headers; link-only feeds and the source list send `StreamFolio/<version> (iOS feed reader; headline-and-link only)`. Everything else in sections 1–5 above (fetch limits, headers for conditional requests, `Retry-After`/`ttl`/429/503/404/410 handling, the 60-second source-list freshness, purge on withdrawal) is implemented identically and covered by `RepositoryTests` and `LinkOnlyPolicyTests`. Publisher terms are platform-independent.
- **Timeouts and limits:** 10 s request timeout, 30 s total, 20 MB maximum body.
- **Concurrency:** a link-only slot is reserved *before* the request is sent, so concurrent refreshes cannot both pass the 30-minute check; on a transport error (no HTTP response) the previous schedule is restored, like Android.
- **Article pages** (body scraping and lead-image resolution) are fetched with the same safe client, the browser User-Agent and `Accept-Language: en-US`, decoded with the charset the server declares. Link-only article pages and images are never requested. Extraction rules are in `docs/01` §6.
- **Online search** (`SearchService`) does not call a search API: it fetches every active curated feed (8 in parallel, same headers and safe client), filters locally and caches matches, exactly like Android's `searchNewsOnline`. Link-only feeds are skipped.
- **Custom feeds** (`NewsRepository.refreshCustomFeeds`, `FeedReader`): each custom feed URL is re-validated (HTTPS, public host) before every fetch; a document that is OPML instead of RSS is expanded into the feeds it lists, each validated again; articles are tagged with the feed's id and title and filed under its tab label. Online search also covers custom feeds, even with curated feeds off, and never fetches link-only URLs.
- **Not yet ported:** Google News redirect resolution (`resolveGoogleNewsUrl`).

---

## 3. Internal Android IPC & Intent Contracts

### 1. `MainActivity` Deep Linking Intent

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:windowSoftInputMode="adjustResize">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- **Intent Extra:** `"article_url"` (`String`).
- **Behavior:** When passed, `MainActivity` sanitizes and normalizes the URL to HTTPS via `UrlSecurityValidator.normalizeToHttps(url)` and immediately routes `AppNavigation` to `DetailScreen`.

---

### 2. `TtsPlaybackService` Foreground Service Actions

`TtsPlaybackService` is registered with `android:exported="false"` and `foregroundServiceType="mediaPlayback"`. It responds to internal explicit intents:

| Action String | Constant | Effect |
| :--- | :--- | :--- |
| `uk.sume.streamfolio.action.TOGGLE_TTS_PLAYBACK` | `ACTION_TOGGLE_PLAYBACK` | Toggles play/pause state in `TtsPlaybackManager`. |
| `uk.sume.streamfolio.action.NEXT_TTS_ARTICLE` | `ACTION_NEXT_ARTICLE` | Skips to the next queued article in the playlist. |
| `uk.sume.streamfolio.action.PREVIOUS_TTS_ARTICLE` | `ACTION_PREVIOUS_ARTICLE` | Rewinds or skips to the previous article in the playlist. |
| `uk.sume.streamfolio.action.STOP_TTS_PLAYBACK` | `ACTION_STOP` | Stops playback, releases MediaSession, and halts the foreground service. |

---

### 3. AppWidget Broadcast & Service Intents

- **`StreamFolioWidgetProvider`:** Receives `android.appwidget.action.APPWIDGET_UPDATE` broadcasts. Dispatches widget layout invalidation and forces `AppWidgetManager.notifyAppWidgetViewDataChanged()`.
- **`StreamFolioWidgetService`:** Requires permission `android.permission.BIND_REMOTEVIEWS`. Acts as the `RemoteViewsFactory` providing cursor-backed article rows for the widget list view.
- **`WidgetConfigureActivity`:** Responds to `android.appwidget.action.APPWIDGET_CONFIGURE` allowing users to configure widget theme, category, and display limits.

---

## 4. Backup & Restore Data Interchange Format (JSON Schema v2)

StreamFolio exports and imports its entire local application state via a structured JSON interchange format validated by [BackupHelper.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/BackupHelper.kt).

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "StreamFolioBackupV2",
  "type": "object",
  "required": ["version", "preferences", "custom_feeds", "articles", "bookmark_lists", "article_bookmark_cross_refs"],
  "properties": {
    "version": { "type": "integer", "enum": [2] },
    "preferences": {
      "type": "object",
      "properties": {
        "language": { "type": "string" },
        "region": { "type": "string" },
        "selected_categories": { "type": "array", "items": { "type": "string" } },
        "is_default_feeds_enabled": { "type": "boolean" },
        "category_order": { "type": "array", "items": { "type": "string" } },
        "disabled_feed_urls": { "type": "array", "items": { "type": "string" } },
        "enabled_cross_region_feeds": { "type": "array", "items": { "type": "string" } },
        "cache_history_days": { "type": "integer" },
        "reader_font_family": { "type": "string" },
        "reader_font_size": { "type": "number" },
        "reader_line_spacing": { "type": "number" },
        "is_ai_enabled": { "type": "boolean" },
        "is_translation_enabled": { "type": "boolean" },
        "is_summary_enabled": { "type": "boolean" },
        "is_smart_tags_enabled": { "type": "boolean" },
        "swipe_left_action": { "type": "string" },
        "swipe_right_action": { "type": "string" },
        "tts_speech_rate": { "type": "number" },
        "use_dynamic_colors": { "type": "boolean" },
        "group_similar_stories": { "type": "boolean" },
        "bookmark_list_view_mode_is_grid": { "type": "boolean" },
        "recent_searches": { "type": "array", "items": { "type": "string" } }
      }
    },
    "custom_feeds": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["title", "url", "category"],
        "properties": {
          "title": { "type": "string", "maxLength": 200 },
          "url": { "type": "string", "format": "uri" },
          "category": { "type": "string", "maxLength": 100 }
        }
      }
    },
    "articles": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["link", "title", "description", "pubDate", "sourceName", "sourceUrl", "category"],
        "properties": {
          "link": { "type": "string", "format": "uri" },
          "title": { "type": "string", "maxLength": 500 },
          "description": { "type": "string", "maxLength": 2000 },
          "pubDate": { "type": "string", "maxLength": 100 },
          "sourceName": { "type": "string", "maxLength": 200 },
          "sourceUrl": { "type": "string", "maxLength": 500 },
          "category": { "type": "string", "maxLength": 100 },
          "thumbnailUrl": { "type": ["string", "null"] },
          "isBookmarked": { "type": "boolean" },
          "fullText": { "type": ["string", "null"], "maxLength": 50000 },
          "aiSummary": { "type": ["string", "null"], "maxLength": 10000 },
          "isRead": { "type": "boolean" }
        }
      }
    },
    "bookmark_lists": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["id", "name", "isDefault", "createdAt"],
        "properties": {
          "id": { "type": "integer" },
          "name": { "type": "string", "maxLength": 100 },
          "isDefault": { "type": "boolean" },
          "createdAt": { "type": "integer" }
        }
      }
    },
    "article_bookmark_cross_refs": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["articleLink", "listId"],
        "properties": {
          "articleLink": { "type": "string" },
          "listId": { "type": "integer" }
        }
      }
    }
  }
}
```

### iOS backup and OPML (added 2026-10-24)

[BackupService](../ios/Packages/StreamFolioCore/Sources/StreamFolioCore/Backup/BackupService.swift) reads and writes this exact schema, so a backup made on Android restores on iOS and vice versa (tested with an Android-shaped file). Differences:

- **Export** lists every key above (`BackupPreferences`), keys sorted, pretty-printed; link-only articles and their list links are never exported. It does not include `completed_onboarding`; the translation target language is the `language` preference, as on Android.
- **Restore** parses the whole file *before* changing anything (Android deletes custom feeds first, so an invalid file there loses them); a bad file changes nothing. Then, in one transaction: custom feeds are replaced; articles are inserted/replaced; if the file has lists, non-default lists are replaced; if it has links, all links are replaced by those whose list exists (links to missing lists are dropped). Preferences are applied last: a scalar outside its allowed range (font size 99, swipe action "explode", a number where a boolean is expected) is ignored, not clamped; collections are bounded by the preference's own rules. The default "Saved" list is re-created if the file omitted it.
- **Limits:** 32 MB per backup file, 5 MB per OPML file (iOS addition; Android has none). Text fields keep Android's bounds (title 500, description 2000, text 50 000, summary 10 000 …).
- **OPML** (`Opml`): same folder rule as Android (an `<outline>` with `xmlUrl` is a feed filed under the nearest enclosing folder, else "Imported"); `xmlurl` in lower case is read too; DOCTYPE entities are not resolved. Import lists only safe, not-yet-subscribed feeds, lets the user untick folders and rename their tabs, and skips duplicates (URL, case-insensitive). Export groups feeds by tab label in first-seen order and escapes `& < > " '`.
- Files are chosen and saved through the system Files picker (`fileImporter` / `fileExporter`); nothing is shared with another app unless the user picks a location there.
