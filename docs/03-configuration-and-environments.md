# 03 — Configuration & Environments

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Build & System Configuration

StreamFolio is an Android application with no remote server components. Configuration is divided into:
1. **Host Environment Variables:** Build machine configuration (JDK, Android SDK, Keystores).
2. **Gradle Build Properties:** JVM heap, AndroidX flags, and resource packaging settings.
3. **Application Runtime Preferences:** Stored on-device via Android `SharedPreferences` (`news_prefs`).

---

## 2. Environment Variables Reference

| Variable Name | Purpose | Required / Optional | Format / Example Value | Where Consumed |
| :--- | :--- | :--- | :--- | :--- |
| `JAVA_HOME` | Specifies path to JDK 11/17/21 runtime | **Required** for CLI builds | `C:\Program Files\Java\jdk-17` | Gradle build daemon |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | Points to Android SDK directory | **Required** (or via `local.properties`) | `C:\Users\username\AppData\Local\Android\Sdk` | Android Gradle Plugin (AGP) |
| `STREAMFOLIO_KEYSTORE_PATH` | Path to production release keystore (`.jks` / `.keystore`) | *Optional* (Release builds only) | `/path/to/release.keystore` | CI/CD Signing (Requires Confirmation) |
| `STREAMFOLIO_KEYSTORE_PASSWORD` | Password for release keystore | *Optional* (Release builds only) | `SecretString` | CI/CD Signing (Requires Confirmation) |
| `STREAMFOLIO_KEY_ALIAS` | Alias for release signing key | *Optional* (Release builds only) | `streamfolio-key` | CI/CD Signing (Requires Confirmation) |
| `STREAMFOLIO_KEY_PASSWORD` | Password for release signing key | *Optional* (Release builds only) | `SecretString` | CI/CD Signing (Requires Confirmation) |

---

## 3. Gradle Build Properties (`gradle.properties`)

Defined in [gradle.properties](file:///d:/News/gradle.properties):

```properties
# JVM Daemon memory allocation and character encoding
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# Enforce AndroidX package structure
android.useAndroidX=true

# Official Kotlin code style rules
kotlin.code.style=official

# Optimize R class bytecode size across libraries
android.nonTransitiveRClass=true
```

---

## 4. Runtime Application Configuration (`PreferencesHelper`)

All user preferences and operational toggles are persisted in private SharedPreferences (`news_prefs`) managed by [PreferencesHelper.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/local/PreferencesHelper.kt).

| Preference Key | Type | Default Value | Valid Values / Constraints | Purpose & Consumer |
| :--- | :--- | :--- | :--- | :--- |
| `completed_onboarding` | `Boolean` | `false` | `true`, `false` | Determines whether to show OnboardingScreen on startup. |
| `language` | `String` | `"en"` | BCP 47 language code (`en`, `fr`, `de`, `ja`, etc.) | Used for curated feed language and ML Kit target language. |
| `region` | `String` | `"US"` | 14 supported 2-letter codes (`US`, `GB`, `SG`, `IN`, `CA`, `AU`, `FR`, `DE`, `HK`, `KR`, `JP`, `BR`, `ZA`, `AE`) | Determines active curated RSS feed provider list. |
| `selected_categories` | `Set<String>` | All 8 categories | `Top Stories`, `World`, `Business`, `Technology`, `Science`, `Sports`, `Health`, `Entertainment` | Active category filters displayed in feed and synced by background worker. |
| `google_news_enabled` | `Boolean` | `true` | `true`, `false` | Master toggle to enable or disable default curated news feeds. |
| `category_order` | `String` (CSV) | Default order | Comma-separated category names | Custom ordering of category pills across the UI. |
| `disabled_feed_urls` | `Set<String>` | `emptySet()` | Set of feed URLs | Specific curated provider URLs disabled by the user in Settings. |
| `enabled_cross_region_feeds` | `Set<String>` | `emptySet()` | Set of feed URLs | Curated provider URLs from other regions explicitly enabled by user. |
| `cache_history_days` | `Int` | `36500` (~100 yrs) | `1` to `36500` | Expiration window for pruning unbookmarked articles from Room DB. |
| `reader_font_family` | `String` | `"sans_serif"` | `"sans_serif"`, `"serif"` | Font family applied in reader mode (`DetailScreen.kt`). |
| `reader_font_size` | `Float` | `16.0f` | `12.0f` to `30.0f` | Font size applied to article body text. |
| `reader_line_spacing` | `Float` | `1.5f` | `1.0f` to `3.0f` | Line height multiplier applied in reader mode. |
| `ai_enabled` | `Boolean` | `false` | `true`, `false` | Master opt-in toggle for on-device AI features. |
| `ai_translation_enabled` | `Boolean` | `true` | `true`, `false` | Enables ML Kit language translation features. |
| `ai_summary_enabled` | `Boolean` | `true` | `true`, `false` | Enables Gemini Nano / AICore summarization. |
| `ai_smart_tags_enabled` | `Boolean` | `true` | `true`, `false` | Enables automated keyword and tag extraction. |
| `swipe_left_action` | `String` | `"bookmark"` | `"bookmark"`, `"share"`, `"read"`, `"none"` | Gesture action triggered by swiping article card left. |
| `swipe_right_action` | `String` | `"share"` | `"bookmark"`, `"share"`, `"read"`, `"none"` | Gesture action triggered by swiping article card right. |
| `tts_speech_rate` | `Float` | `1.0f` | `0.5f` to `3.0f` | Speech rate playback multiplier for Android TTS engine. |
| `use_dynamic_colors` | `Boolean` | `false` | `true`, `false` | Enables Android 12+ Material You dynamic color extraction. |
| `group_similar_stories` | `Boolean` | `true` | `true`, `false` | Enables TF-IDF clustering and grouping of related stories. |
| `bookmark_list_view_mode_is_grid` | `Boolean` | `true` | `true`, `false` | View layout toggle for Bookmark Collections (Grid vs List). |
| `recent_searches` | `String` (Delimited) | `""` | Pipe-delimited string (`|||`) | Last 10 search queries persisted for search screen history. |

---

## 5. Network Security Configuration (`network_security_config.xml`)

Defined in [network_security_config.xml](file:///d:/News/app/src/main/res/xml/network_security_config.xml). By default, Android blocks cleartext (HTTP) traffic. StreamFolio enforces HTTPS by default and whitelists cleartext traffic strictly for legacy publisher domains that do not support HTTPS RSS feeds:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">www.scmp.com</domain>
        <domain includeSubdomains="true">scmp.com</domain>
        <domain includeSubdomains="true">rthk.hk</domain>
        <domain includeSubdomains="true">rthk9.rthk.hk</domain>
        <domain includeSubdomains="true">www.techradar.com</domain>
        <domain includeSubdomains="true">www.nhk.or.jp</domain>
        <domain includeSubdomains="true">www.dailymaverick.co.za</domain>
        <domain includeSubdomains="true">www.iol.co.za</domain>
    </domain-config>
</network-security-config>
```

---

## 6. Secret Management & Git Hygiene

> [!IMPORTANT]
> **Zero Secrets Policy**: StreamFolio is an open-source client application with zero hardcoded API keys, private tokens, or backend credentials.
> - Keystores, signing passwords, and local SDK paths must never be committed to source control.
> - `.gitignore` is configured to exclude `local.properties`, `*.jks`, `*.keystore`, `.gradle/`, and build outputs.
