# StreamFolio — Android app UI kit

A click-through recreation of the shipping Android app (Kotlin + Jetpack Compose, Material 3), built from the source in `sm32d/streamfolio` (`app/src/main/java/uk/sume/streamfolio/ui/`).

Open `index.html`. The frame is 412 × 892 — a modern Android phone at 1x.

## What works

| Flow | Try it |
| --- | --- |
| Browse the feed | Tap category pills; tap a publisher avatar to filter; tap the pager dots |
| Open an article | Tap any card → reader with tabs, typography panel, AI summary |
| Generate a summary | "Generate Summary" in the reader → loading → bullet insights |
| Read aloud | "Listen" in the reader → mini player docks above the nav bar |
| Search | Type in the field (the placeholder types itself); empty and no-result states both appear |
| Bookmarks | Toggle grid/list; open a collection; create a new one via the bottom sheet |
| Settings | Navigate to Preferences; flip switches; tap the version row five times |

## Files

- `index.html` — mounts the app, loads the design-system bundle
- `App.jsx` — tab shell, nav bar, mini player, toast host
- `Chrome.jsx` — status bar, screen header, header icon button, section heading, edge-padded strip
- `HomeScreen.jsx` — wordmark header, category strip, smart tags, publisher filter, trending pager, latest list
- `ReaderScreen.jsx` — Reader/Web tabs, typography panel, AI key insights, article body
- `SearchScreen.jsx` — self-typing placeholder, trending topics, recent searches, result and empty states
- `BookmarksScreen.jsx` — collection grid/list, collection detail, create-collection sheet
- `SettingsScreen.jsx` — grouped settings, Preferences sub-page, StreamFolio FM easter egg
- `data.js` — sample feed content

## Known deviations

- **No article imagery.** The repo ships no editorial images, so every card renders the app's real fallback: a blue→green tile with the publisher's initial. On device, thumbnails are scraped per article.
- **Publisher avatars** use the same Google s2 favicon service the app uses, so they need network access.
- **The full-screen TTS lyrics visualiser** (`TtsMiniPlayer.kt`, ~1,300 lines) is represented by the docked mini player only.
- **Onboarding** (`OnboardingScreen.kt`, 5 steps) and the **home-screen widget** are not recreated.
