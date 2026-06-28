# StreamFolio – Premium News Aggregator

## Overview
StreamFolio is a modern, premium Android news‑reading app built with **Jetpack Compose** and **MVVM** architecture. It aggregates curated RSS/Atom feeds from multiple regions, supports custom user‑defined feeds, and offers powerful UI features such as drag‑and‑drop tab reordering, cross‑regional provider toggles, and a polished Material 3 design that matches the app’s blue‑cream branding.

## Key Features

| Feature | Description |
|---------|-------------|
| **Curated Default Feeds** | Reliable regional feeds (US, GB, SG, IN, CA, AU, FR, DE) mapped in `DefaultFeedsConfig.kt`. |
| **Custom RSS Feeds** | Users can add their own feeds; they appear alongside default categories. |
| **Cross‑Regional Provider Toggles** | Enable any provider from any region while keeping a primary region active. |
| **Drag‑and‑Drop Tab Reordering** | Reorder category tabs via long‑press gestures or arrow buttons (Settings → Reorder Category Tabs). |
| **Chronological Sorting** | All dates are normalized to ISO‑8601 UTC strings, guaranteeing correct `ORDER BY pubDate DESC` ordering. |
| **Dynamic “World” Category** | Dedicated world‑news tab with global providers. |
| **Hybrid Search** | Local DB search + concurrent online feed queries, with case‑insensitive matching. |
| **Premium UI** | Custom gradients, blue‑cream palette, high‑contrast typography, skeleton loaders, and smooth micro‑animations. |
| **Predictive Back Navigation** | Edge‑swipe back gestures, custom slide transitions. |
| **Persistence** | Preferences stored via `PreferencesHelper` (category order, disabled feeds, enabled cross‑region feeds). |

## Architecture

- **UI** – Jetpack Compose (Material 3, theming, animations).
- **State Management** – `ViewModel` + `StateFlow`.
- **Networking** – OkHttp for HTTP requests, `RssParser` for XML parsing.
- **Local Storage** – Room database (`ArticleDao`, `Article` entity).
- **Dependency Injection** – Simple constructor injection (no Dagger/Koin to keep the sample light).

## Project Structure

```
app/
 ├─ src/main/java/uk/sume/streamfolio/
 │   ├─ data/
 │   │   ├─ local/          // Room DAO & PreferencesHelper
 │   │   ├─ model/          // Article data class
 │   │   └─ network/        // DefaultFeedsConfig, RssParser, NewsRepository
 │   ├─ ui/
 │   │   ├─ screens/        // HomeScreen, DetailScreen, SettingsScreen, etc.
 │   │   └─ theme/          // Color.kt, Theme.kt
 │   └─ viewmodel/          // NewsViewModel
 └─ res/
     ├─ drawable/           // Icons, splash, etc.
     └─ values/            // Strings, dimens
```

## Build Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Flamingo (2022.2.1) or newer |
| Gradle Wrapper | 8.5 |
| Android Gradle Plugin | 8.2.0 |
| Kotlin | 2.0 (compiled with Kapt fallback to 1.9) |
| Minimum SDK | API 21 |
| Target SDK | API 34 |
| Java | 17 (compatible with Android) |

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/streamfolio.git
   cd streamfolio
   ```

2. **Open in Android Studio**
    - Choose *Open an existing project* and select the `app` directory.
    - Android Studio will sync Gradle automatically.

3. **Run the app**
    - Connect an Android device or start an emulator (API 34 recommended).
    - Click **Run** ► **app**.

4. **Build from the command line** (optional)
   ```bash
   # Compile only
   ./gradlew compileDebugKotlin

   # Assemble a debug APK
   ./gradlew assembleDebug

   # Release build (signing config required)
   ./gradlew assembleRelease
   ```

## Customization

- **Add a new feed** – Open Settings → Manage Content & Sources → *Add Custom Feed*.
- **Change active region** – Settings → Preferences → *Region* dropdown.
- **Reorder categories** – Settings → Reorder Category Tabs (drag handles or arrow buttons).

All changes are persisted via `PreferencesHelper.kt` (shared Preferences) and reflected instantly on the Home screen.

## Testing

The project includes unit tests for the RSS parser and repository logic:

```bash
./gradlew testDebugUnitTest
```

UI tests (Espresso/Compose) can be executed with:

```bash
./gradlew connectedDebugAndroidTest
```

## License

This project is licensed under the **GPL-3.0 license** – feel free to fork, modify, and redistribute.

---

**Happy coding!** 🎉 If you encounter any issues, please open a GitHub issue or contact the maintainer.
