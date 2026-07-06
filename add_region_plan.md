# Add Hong Kong Region and Default RSS Feeds

This plan details the implementation steps to add Hong Kong (`HK`) as a supported region in the StreamFolio app, along with its default RSS feeds.

## Proposed Changes

### Configuration & Data Layer

#### [MODIFY] [DefaultFeedsConfig.kt](file:///Users/dom/projects/streamfolio/app/src/main/java/uk/sume/streamfolio/data/network/DefaultFeedsConfig.kt)
- Add `"HK"` region mapping in `FEEDS` with the following default categorized feeds:
  - **Top Stories**:
    - Hong Kong Free Press (HKFP): `https://hongkongfp.com/feed/`
    - South China Morning Post (SCMP): `https://www.scmp.com/rss/91/feed`
    - RTHK (Local News): `https://rthk.hk/rthk/news/rss/e_expressnews_elocal.xml`
    - GovHK: `https://www.news.gov.hk/rss/news/topstories_en.xml`
  - **World**:
    - SCMP (World): `https://www.scmp.com/rss/5/feed`
    - RTHK (International): `https://rthk.hk/rthk/news/rss/e_expressnews_einternational.xml`
  - **Business**:
    - SCMP (Business): `https://www.scmp.com/rss/92/feed`
    - RTHK (Finance): `https://rthk.hk/rthk/news/rss/e_expressnews_efinance.xml`
  - **Technology**:
    - SCMP (Tech): `https://www.scmp.com/rss/36/feed`
    - Fintech News HK: `https://fintechnews.hk/feed/`
  - **Sports**:
    - SCMP (Sport): `https://www.scmp.com/rss/95/feed`
    - RTHK (Sport): `https://rthk.hk/rthk/news/rss/e_expressnews_esport.xml`
  - **Health**:
    - SCMP (Health): `https://www.scmp.com/rss/32/feed`
  - **Entertainment**:
    - SCMP (Lifestyle): `https://www.scmp.com/rss/94/feed`
    - Oriental Sunday: `https://www.orientalsunday.hk/feed/`
    - East Week: `https://eastweek.stheadline.com/rss`

- Update `getPublisherName(url: String)` to resolve names for:
  - `hongkongfp.com` -> "Hong Kong Free Press"
  - `scmp.com` -> "South China Morning Post"
  - `rthk.hk` -> "RTHK News"
  - `news.gov.hk` -> "GovHK"
  - `fintechnews.hk` -> "Fintech News HK"
  - `orientalsunday.hk` -> "Oriental Sunday"
  - `stheadline.com` -> "East Week"

---

### UI Layer

#### [MODIFY] [OnboardingScreen.kt](file:///Users/dom/projects/streamfolio/app/src/main/java/uk/sume/streamfolio/ui/screens/OnboardingScreen.kt)
- Add `"HK"` to the list of default recognized regions in `defaultRegion` (line 97).
- Add `"HK" to "🇭🇰 Hong Kong"` to the `regions` map (line 475).

#### [MODIFY] [SettingsScreen.kt](file:///Users/dom/projects/streamfolio/app/src/main/java/uk/sume/streamfolio/ui/screens/SettingsScreen.kt)
- Add `"HK" to "🇭🇰 Hong Kong"` to the `regions` map (line 299).
- Add `"HK" -> "🇭🇰 Hong Kong"` to the regional name rendering mapping in:
  - `regionCode` mapping (line 921)
  - `provider.region` mapping (line 1114)

---

## Verification Plan

### Automated Tests
- Run unit tests to check that `DefaultFeedsConfig` maps correct names and feeds for the `"HK"` region.
  - Gradle command: `./gradlew test`

### Manual Verification
- Launch the application and verify that "Hong Kong" is selectable on the onboarding screen.
- Verify that default feeds load successfully and are tagged with the correct publisher name in Settings and Home Screen.
