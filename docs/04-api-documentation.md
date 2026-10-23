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
- **Rate limit (per device):** at most one request per 30 minutes per feed URL (`LinkOnlyFetchPolicy.MIN_INTERVAL_MS`), applied to automatic refresh, pull-to-refresh, background sync, widget refresh and Settings toggles. Fetches are serialised by a process-wide lock, so simultaneous refreshes cannot both send a request. Longer waits are honored: `Cache-Control: max-age`, the feed's `<ttl>`, `Retry-After`. Responses `429`/`503` back off at least 1 hour; `404`/`410` back off 24 hours; other failures wait 30 minutes. No wait exceeds 24 hours. `304 Not Modified` keeps the cached articles.
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
- **When:** at the start of a feed refresh (`fetchDefaultFeeds`, `fetchCustomFeeds`, `fetchSingleFeed`), at most once per hour (`SourceConfig.CHECK_INTERVAL_MS`).
- **Effect:** a link-only source that is missing or `enabled: false` is not fetched, is hidden from Settings > Providers, and everything cached from it (articles, bookmark-list associations, throttle state) is deleted from the device. A failed download or unparsable file keeps the last known state. Before any successful download, link-only sources are disabled.
- **Availability:** requires the repository to be public with the file on `master`. Public status: `Unknown / Requires Confirmation`.

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
