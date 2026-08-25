repo: sm32d/streamfolio
branch: master
path: app/src/main

## Last sync
date: 2026-08-25T07:35:30Z

### Updated in this project
- Built the design system from scratch: tokens, 26 components, 18 specimen cards.
- Imported the app icon (3 variants) and the four otter empty-state illustrations.
- Recreated the Android app as an interactive 412x892 UI kit (feed, reader, search, bookmarks, settings).
- Documented content, visual and iconography foundations in readme.md.

## Screen map
| Project screen / file | Repo source |
| --- | --- |
| tokens/colors.css, tokens/typography.css | app/src/main/java/uk/sume/streamfolio/ui/theme/Color.kt, Theme.kt, Type.kt |
| components/navigation/CategoryPill.jsx | ui/components/CategoryPill.kt |
| components/navigation/GlassmorphicNavBar.jsx | ui/components/GlassmorphicNavBar.kt |
| components/navigation/SubPageTopBar.jsx, components/settings/* | ui/components/SettingsComponents.kt |
| components/article/ArticleCard.jsx, TrendingCard.jsx | ui/screens/HomeScreen.kt |
| components/article/PublisherAvatar.jsx | ui/components/PublisherAvatar.kt |
| components/feedback/EmptyState.jsx | ui/components/EmptyState.kt |
| components/feedback/OfflineIndicator.jsx | ui/components/OfflineIndicator.kt |
| components/feedback/SkeletonLoader.jsx | ui/components/SkeletonLoader.kt |
| components/feedback/ToastBanner.jsx | ui/components/BookmarkToastBanner.kt |
| components/feedback/SwipeHintBanner.jsx | ui/screens/HomeScreen.kt |
| components/sheets/ConfirmationSheet.jsx, SheetActionRow.jsx | ui/components/ConfirmationModalSheet.kt, ui/screens/HomeScreen.kt |
| components/collections/* | ui/screens/BookmarkScreen.kt |
| components/promo/AiSpotlightBanner.jsx | ui/screens/HomeScreen.kt |
| components/promo/RadioCard.jsx | ui/components/StreamFolioRadioCard.kt |
| components/player/TtsMiniPlayer.jsx | ui/components/TtsMiniPlayer.kt |
| ui_kits/android-app/HomeScreen.jsx | ui/screens/HomeScreen.kt |
| ui_kits/android-app/ReaderScreen.jsx | ui/screens/DetailScreen.kt |
| ui_kits/android-app/SearchScreen.jsx | ui/screens/SearchScreen.kt |
| ui_kits/android-app/BookmarksScreen.jsx | ui/screens/BookmarkScreen.kt |
| ui_kits/android-app/SettingsScreen.jsx | ui/screens/settings/SettingsMainScreen.kt, SettingsPreferencesScreen.kt |
| assets/logo-app-icon.png | app/src/main/ic_launcher-playstore.png |
| assets/otter-*.png | app/src/main/res/drawable-xxxhdpi/otter_*.png |
